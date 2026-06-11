package com.hackconnect.controller;

import com.hackconnect.dto.request.RoadmapRequest;
import com.hackconnect.dto.response.ApiResponse;
import com.hackconnect.dto.response.RoadmapResponse;
import com.hackconnect.model.LearningRoadmap;
import com.hackconnect.model.UserRoadmapProgress;
import com.hackconnect.service.RoadmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    /**
     * GET /api/v1/roadmaps
     *   ?domain=Frontend  (optional)
     *   &level=BEGINNER   (optional: BEGINNER | INTERMEDIATE | ADVANCED)
     *
     * Returns summary list. When authenticated, includes per-user progress.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoadmapResponse.Summary>>> getAll(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) LearningRoadmap.Level level,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.ok(roadmapService.getAll(domain, level, email)));
    }

    /**
     * GET /api/v1/roadmaps/popular
     * Top 6 by enrolled count.
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<RoadmapResponse.Summary>>> getPopular(
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.ok(roadmapService.getPopular(email)));
    }

    /**
     * GET /api/v1/roadmaps/domains
     * All distinct domain values.
     */
    @GetMapping("/domains")
    public ResponseEntity<ApiResponse<List<String>>> getDomains() {
        return ResponseEntity.ok(ApiResponse.ok(roadmapService.getAllDomains()));
    }

    /**
     * GET /api/v1/roadmaps/my
     * Requires auth. Returns all roadmaps the user is enrolled in with progress.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RoadmapResponse.EnrolledSummary>>> getMyRoadmaps(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(roadmapService.getMyRoadmaps(user.getUsername())));
    }

    /**
     * GET /api/v1/roadmaps/{id}
     * Full detail with all steps. When authenticated, includes per-step status.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoadmapResponse.Detail>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ResponseEntity.ok(ApiResponse.ok(roadmapService.getById(id, email)));
    }

    /**
     * POST /api/v1/roadmaps/{id}/enroll
     * Enrols the authenticated user. Creates NOT_STARTED progress rows for each step.
     */
    @PostMapping("/{id}/enroll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> enroll(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        roadmapService.enroll(id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Enrolled successfully"));
    }

    /**
     * PATCH /api/v1/roadmaps/{roadmapId}/steps/{stepId}/status
     * Body: { "status": "IN_PROGRESS" | "COMPLETED" | "NOT_STARTED" }
     * Updates a single step's status for the authenticated user.
     */
    @PatchMapping("/{roadmapId}/steps/{stepId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoadmapResponse.StepDetail>> updateStepStatus(
            @PathVariable Long roadmapId,
            @PathVariable Long stepId,
            @Valid @RequestBody RoadmapRequest.StepStatus req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                roadmapService.updateStepStatus(roadmapId, stepId, req.getStatus(),
                        user.getUsername()),
                "Step status updated"));
    }

    /**
     * POST /api/v1/roadmaps        — Admin only
     * Create a new roadmap with steps in one request.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoadmapResponse.Detail>> create(
            @Valid @RequestBody RoadmapRequest.Create req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(roadmapService.create(req), "Roadmap created"));
    }

    /**
     * DELETE /api/v1/roadmaps/{id} — Admin only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roadmapService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Roadmap deleted"));
    }
}
