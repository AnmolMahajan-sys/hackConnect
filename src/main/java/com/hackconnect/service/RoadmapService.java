package com.hackconnect.service;

import com.hackconnect.dto.request.RoadmapRequest;
import com.hackconnect.dto.response.RoadmapResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.*;
import com.hackconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final LearningRoadmapRepository      roadmapRepository;
    private final RoadmapStepRepository          stepRepository;
    private final UserRoadmapProgressRepository  progressRepository;
    private final UserRepository                 userRepository;

    /* ── Listing ─────────────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<RoadmapResponse.Summary> getAll(String domain, LearningRoadmap.Level level,
                                                 String email) {
        List<LearningRoadmap> roadmaps;
        if (domain != null && level != null) {
            roadmaps = roadmapRepository.findByDomainIgnoreCaseAndLevel(domain, level);
        } else if (domain != null) {
            roadmaps = roadmapRepository.findByDomainIgnoreCase(domain);
        } else if (level != null) {
            roadmaps = roadmapRepository.findByLevel(level);
        } else {
            roadmaps = roadmapRepository.findAll();
        }
        return roadmaps.stream()
                .map(r -> toSummary(r, email))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoadmapResponse.Summary> getPopular(String email) {
        return roadmapRepository.findTop6ByOrderByEnrolledCountDesc().stream()
                .map(r -> toSummary(r, email))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllDomains() {
        return roadmapRepository.findAllDomains();
    }

    /* ── Single roadmap ──────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public RoadmapResponse.Detail getById(Long id, String email) {
        LearningRoadmap r = findOrThrow(id);
        return toDetail(r, email);
    }

    /* ── Enroll ──────────────────────────────────────────────────────────── */

    @Transactional
    public void enroll(Long roadmapId, String email) {
        LearningRoadmap roadmap = findOrThrow(roadmapId);
        User user = findUserOrThrow(email);

        if (progressRepository.existsByUserIdAndRoadmapId(user.getId(), roadmapId)) {
            throw new HackConnectException.DuplicateResourceException(
                    "Already enrolled in this roadmap");
        }

        // Create a NOT_STARTED progress record for every step
        for (RoadmapStep step : roadmap.getSteps()) {
            UserRoadmapProgress progress = UserRoadmapProgress.builder()
                    .user(user)
                    .roadmap(roadmap)
                    .step(step)
                    .status(UserRoadmapProgress.Status.NOT_STARTED)
                    .build();
            progressRepository.save(progress);
        }

        roadmapRepository.incrementEnrolledCount(roadmapId);
    }

    /* ── Update step status ──────────────────────────────────────────────── */

    @Transactional
    public RoadmapResponse.StepDetail updateStepStatus(Long roadmapId, Long stepId,
                                                        UserRoadmapProgress.Status status,
                                                        String email) {
        User user = findUserOrThrow(email);

        if (!progressRepository.existsByUserIdAndRoadmapId(user.getId(), roadmapId)) {
            throw new HackConnectException.BadRequestException(
                    "Not enrolled in roadmap " + roadmapId + ". Enroll first.");
        }

        UserRoadmapProgress progress = progressRepository
                .findByUserIdAndStepId(user.getId(), stepId)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException(
                        "Progress record not found for step " + stepId));

        progress.setStatus(status);
        if (status == UserRoadmapProgress.Status.COMPLETED) {
            progress.setCompletedAt(LocalDateTime.now());
        } else {
            progress.setCompletedAt(null);
        }
        progressRepository.save(progress);

        return toStepDetail(progress.getStep(), progress);
    }

    /* ── My enrolled roadmaps ────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public List<RoadmapResponse.EnrolledSummary> getMyRoadmaps(String email) {
        User user = findUserOrThrow(email);
        List<LearningRoadmap> enrolled = progressRepository.findEnrolledRoadmaps(user.getId());

        return enrolled.stream().map(r -> {
            int total     = r.getSteps().size();
            long completed = progressRepository.countCompleted(user.getId(), r.getId());
            int percent    = total > 0 ? (int) ((completed * 100.0) / total) : 0;

            LocalDateTime enrolledAt = progressRepository
                    .findByUserIdAndRoadmapId(user.getId(), r.getId())
                    .stream()
                    .map(UserRoadmapProgress::getEnrolledAt)
                    .findFirst().orElse(null);

            return RoadmapResponse.EnrolledSummary.builder()
                    .roadmapId(r.getId())
                    .title(r.getTitle())
                    .domain(r.getDomain())
                    .level(r.getLevel())
                    .totalSteps(total)
                    .completedSteps((int) completed)
                    .progressPercent(percent)
                    .enrolledAt(enrolledAt)
                    .build();
        }).collect(Collectors.toList());
    }

    /* ── Create (Admin) ──────────────────────────────────────────────────── */

    @Transactional
    public RoadmapResponse.Detail create(RoadmapRequest.Create req) {
        LearningRoadmap roadmap = LearningRoadmap.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .domain(req.getDomain())
                .level(req.getLevel())
                .estimatedWeeks(req.getEstimatedWeeks())
                .thumbnailUrl(req.getThumbnailUrl())
                .steps(new ArrayList<>())
                .build();

        LearningRoadmap saved = roadmapRepository.save(roadmap);

        if (req.getSteps() != null) {
            int order = 1;
            for (RoadmapRequest.StepCreate sc : req.getSteps()) {
                RoadmapStep step = RoadmapStep.builder()
                        .roadmap(saved)
                        .title(sc.getTitle())
                        .description(sc.getDescription())
                        .stepOrder(sc.getStepOrder() > 0 ? sc.getStepOrder() : order)
                        .estimatedHours(sc.getEstimatedHours())
                        .resources(sc.getResources() != null ? sc.getResources() : new ArrayList<>())
                        .projectIdeas(sc.getProjectIdeas() != null ? sc.getProjectIdeas() : new ArrayList<>())
                        .build();
                saved.getSteps().add(stepRepository.save(step));
                order++;
            }
        }

        return toDetail(saved, null);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        roadmapRepository.deleteById(id);
    }

    /* ── Mapping helpers ─────────────────────────────────────────────────── */

    private RoadmapResponse.Summary toSummary(LearningRoadmap r, String email) {
        int total = r.getSteps().size();
        Integer completed = null;
        Integer percent   = null;
        boolean enrolled  = false;

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                enrolled  = progressRepository.existsByUserIdAndRoadmapId(user.getId(), r.getId());
                long comp = progressRepository.countCompleted(user.getId(), r.getId());
                completed = (int) comp;
                percent   = total > 0 ? (int) ((comp * 100.0) / total) : 0;
            }
        }

        return RoadmapResponse.Summary.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .domain(r.getDomain())
                .level(r.getLevel())
                .estimatedWeeks(r.getEstimatedWeeks())
                .thumbnailUrl(r.getThumbnailUrl())
                .enrolledCount(r.getEnrolledCount())
                .totalSteps(total)
                .completedSteps(completed)
                .progressPercent(percent)
                .enrolled(enrolled)
                .build();
    }

    private RoadmapResponse.Detail toDetail(LearningRoadmap r, String email) {
        // Build per-step progress map if user is known
        Map<Long, UserRoadmapProgress> progressMap = Map.of();
        boolean enrolled = false;

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                enrolled = progressRepository.existsByUserIdAndRoadmapId(user.getId(), r.getId());
                progressMap = progressRepository
                        .findByUserIdAndRoadmapId(user.getId(), r.getId())
                        .stream()
                        .collect(Collectors.toMap(p -> p.getStep().getId(), p -> p));
            }
        }

        final Map<Long, UserRoadmapProgress> pm = progressMap;
        List<RoadmapResponse.StepDetail> steps = r.getSteps().stream()
                .map(s -> toStepDetail(s, pm.get(s.getId())))
                .collect(Collectors.toList());

        int total     = steps.size();
        long completed = steps.stream()
                .filter(s -> s.getStatus() == UserRoadmapProgress.Status.COMPLETED)
                .count();
        Integer percent = total > 0 ? (int) ((completed * 100.0) / total) : 0;

        return RoadmapResponse.Detail.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .domain(r.getDomain())
                .level(r.getLevel())
                .estimatedWeeks(r.getEstimatedWeeks())
                .thumbnailUrl(r.getThumbnailUrl())
                .enrolledCount(r.getEnrolledCount())
                .steps(steps)
                .createdAt(r.getCreatedAt())
                .completedSteps((int) completed)
                .progressPercent(percent)
                .enrolled(enrolled)
                .build();
    }

    private RoadmapResponse.StepDetail toStepDetail(RoadmapStep s, UserRoadmapProgress p) {
        return RoadmapResponse.StepDetail.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .stepOrder(s.getStepOrder())
                .estimatedHours(s.getEstimatedHours())
                .resources(s.getResources())
                .projectIdeas(s.getProjectIdeas())
                .status(p != null ? p.getStatus() : null)
                .completedAt(p != null ? p.getCompletedAt() : null)
                .build();
    }

    private LearningRoadmap findOrThrow(Long id) {
        return roadmapRepository.findById(id)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException("Roadmap", id));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HackConnectException.ResourceNotFoundException(
                        "User not found: " + email));
    }
}
