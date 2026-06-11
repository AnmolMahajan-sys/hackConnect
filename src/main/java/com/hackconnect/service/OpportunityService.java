package com.hackconnect.service;

import com.hackconnect.dto.request.OpportunityRequest;
import com.hackconnect.dto.response.OpportunityResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.Opportunity;
import com.hackconnect.model.User;
import com.hackconnect.repository.OpportunityRepository;
import com.hackconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final UserRepository        userRepository;

    /* ── Read ───────────────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> filter(
            Opportunity.Type type,
            String domain,
            Boolean online,
            Boolean verified,
            String search,
            Boolean upcomingOnly,
            int page, int size,
            String sortBy) {

        Sort sort = buildSort(sortBy);
        PageRequest pageable = PageRequest.of(page, size, sort);

        LocalDateTime deadlineAfter = Boolean.TRUE.equals(upcomingOnly) ? LocalDateTime.now() : null;

        return opportunityRepository
                .filter(type, domain, online, verified, search, deadlineAfter, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OpportunityResponse getById(Long id) {
        Opportunity opp = findOrThrow(id);
        // bump view count in a separate query to avoid optimistic lock issues
        opportunityRepository.incrementViewCount(id);
        opp.setViewCount(opp.getViewCount() + 1);
        return toResponse(opp);
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> getUpcoming(int limit) {
        return opportunityRepository
                .findUpcoming(LocalDateTime.now(), PageRequest.of(0, limit))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> getTrending() {
        return opportunityRepository.findTop6ByVerifiedTrueOrderByViewCountDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllDomains() {
        return opportunityRepository.findAllDomains();
    }

    /* ── Write ──────────────────────────────────────────────────────────── */

    @Transactional
    public OpportunityResponse create(OpportunityRequest req, String creatorEmail) {
        User poster = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException("User not found"));

        Opportunity opp = Opportunity.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .type(req.getType())
                .organizer(req.getOrganizer())
                .location(req.getLocation())
                .online(req.isOnline())
                .startDate(req.getStartDate())
                .deadline(req.getDeadline())
                .registrationUrl(req.getRegistrationUrl())
                .prize(req.getPrize())
                .domain(req.getDomain())
                .tags(req.getTags() != null ? req.getTags() : new java.util.HashSet<>())
                .postedBy(poster)
                .verified(false)
                .build();

        return toResponse(opportunityRepository.save(opp));
    }

    @Transactional
    public OpportunityResponse update(Long id, OpportunityRequest req, String editorEmail) {
        Opportunity opp = findOrThrow(id);
        assertOwnerOrAdmin(opp, editorEmail);

        opp.setTitle(req.getTitle());
        opp.setDescription(req.getDescription());
        opp.setType(req.getType());
        opp.setOrganizer(req.getOrganizer());
        opp.setLocation(req.getLocation());
        opp.setOnline(req.isOnline());
        opp.setStartDate(req.getStartDate());
        opp.setDeadline(req.getDeadline());
        opp.setRegistrationUrl(req.getRegistrationUrl());
        opp.setPrize(req.getPrize());
        opp.setDomain(req.getDomain());
        if (req.getTags() != null) opp.setTags(req.getTags());

        return toResponse(opportunityRepository.save(opp));
    }

    @Transactional
    public void delete(Long id, String email) {
        Opportunity opp = findOrThrow(id);
        assertOwnerOrAdmin(opp, email);
        opportunityRepository.delete(opp);
    }

    @Transactional
    public OpportunityResponse verify(Long id) {
        Opportunity opp = findOrThrow(id);
        opp.setVerified(true);
        return toResponse(opportunityRepository.save(opp));
    }

    /* ── Helpers ────────────────────────────────────────────────────────── */

    private Opportunity findOrThrow(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException("Opportunity", id));
    }

    private void assertOwnerOrAdmin(Opportunity opp, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException("User not found"));
        boolean isOwner = opp.getPostedBy() != null &&
                          opp.getPostedBy().getEmail().equals(email);
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new HackConnectException.AccessDeniedException(
                    "You are not allowed to modify this opportunity");
        }
    }

    private Sort buildSort(String sortBy) {
        return switch (sortBy == null ? "deadline" : sortBy) {
            case "views"    -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "newest"   -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "title"    -> Sort.by(Sort.Direction.ASC,  "title");
            default         -> Sort.by(Sort.Direction.ASC,  "deadline");
        };
    }

    public OpportunityResponse toResponse(Opportunity o) {
        Long days = null;
        if (o.getDeadline() != null) {
            days = ChronoUnit.DAYS.between(LocalDateTime.now(), o.getDeadline());
            if (days < 0) days = 0L;
        }
        return OpportunityResponse.builder()
                .id(o.getId())
                .title(o.getTitle())
                .description(o.getDescription())
                .type(o.getType())
                .organizer(o.getOrganizer())
                .location(o.getLocation())
                .online(o.isOnline())
                .startDate(o.getStartDate())
                .deadline(o.getDeadline())
                .registrationUrl(o.getRegistrationUrl())
                .prize(o.getPrize())
                .domain(o.getDomain())
                .tags(o.getTags())
                .verified(o.isVerified())
                .viewCount(o.getViewCount())
                .createdAt(o.getCreatedAt())
                .daysUntilDeadline(days)
                .build();
    }
}
