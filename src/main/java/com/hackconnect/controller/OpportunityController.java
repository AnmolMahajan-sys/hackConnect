package com.hackconnect.controller;

import com.hackconnect.dto.request.OpportunityRequest;
import com.hackconnect.dto.response.ApiResponse;
import com.hackconnect.dto.response.OpportunityResponse;
import com.hackconnect.model.Opportunity;
import com.hackconnect.service.OpportunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    /**
     * GET /api/v1/opportunities
     *   ?type=HACKATHON          (optional)
     *   &domain=Web Dev          (optional)
     *   &online=true             (optional)
     *   &verified=true           (optional)
     *   &search=react            (optional, searches title+organizer+domain)
     *   &upcomingOnly=true       (optional, deadline > now)
     *   &page=0&size=10          (pagination)
     *   &sortBy=deadline|views|newest|title
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OpportunityResponse>>> filter(
            @RequestParam(required = false) Opportunity.Type type,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean upcomingOnly,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deadline") String sortBy) {

        return ResponseEntity.ok(ApiResponse.ok(
                opportunityService.filter(type, domain, online, verified,
                        search, upcomingOnly, page, size, sortBy)));
    }

    /**
     * GET /api/v1/opportunities/upcoming?limit=6
     * Returns soonest-deadline opportunities (deadline > now).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<OpportunityResponse>>> getUpcoming(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.getUpcoming(limit)));
    }

    /**
     * GET /api/v1/opportunities/trending
     * Returns top-6 verified by view count.
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<OpportunityResponse>>> getTrending() {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.getTrending()));
    }

    /**
     * GET /api/v1/opportunities/domains
     * Returns all distinct domain values (for filter dropdowns).
     */
    @GetMapping("/domains")
    public ResponseEntity<ApiResponse<List<String>>> getDomains() {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.getAllDomains()));
    }

    /**
     * GET /api/v1/opportunities/{id}
     * Also increments view count.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.getById(id)));
    }

    /**
     * POST /api/v1/opportunities
     * Requires authentication. Any logged-in user can post an opportunity.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OpportunityResponse>> create(
            @Valid @RequestBody OpportunityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        opportunityService.create(req, user.getUsername()),
                        "Opportunity created. Pending admin verification."));
    }

    /**
     * PUT /api/v1/opportunities/{id}
     * Only the poster or an ADMIN can update.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OpportunityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                opportunityService.update(id, req, user.getUsername()),
                "Opportunity updated"));
    }

    /**
     * DELETE /api/v1/opportunities/{id}
     * Only the poster or an ADMIN can delete.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        opportunityService.delete(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Opportunity deleted"));
    }

    /**
     * PATCH /api/v1/opportunities/{id}/verify
     * Admin-only action to mark an opportunity as verified.
     */
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                opportunityService.verify(id), "Opportunity verified"));
    }
}
