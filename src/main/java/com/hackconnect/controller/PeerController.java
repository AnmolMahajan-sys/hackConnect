package com.hackconnect.controller;

import com.hackconnect.dto.request.PeerRequest;
import com.hackconnect.dto.response.ApiResponse;
import com.hackconnect.dto.response.PeerResponse;
import com.hackconnect.repository.UserRepository;
import com.hackconnect.service.PeerService;
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
@RequestMapping("/api/v1/peers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PeerController {

    private final PeerService    peerService;
    private final UserRepository userRepository;

    // ── DISCOVER ──────────────────────────────────────────────────────────────

    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<List<PeerResponse.UserCard>>> discover(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String college,
            @AuthenticationPrincipal UserDetails ud) {
        Long id = userId(ud);
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.discoverPeople(domain, skill, college, id)));
    }

    // ── CONNECTIONS ───────────────────────────────────────────────────────────

    @PostMapping("/connect/{receiverId}")
    public ResponseEntity<ApiResponse<PeerResponse.ConnectionCard>> sendRequest(
            @PathVariable Long receiverId,
            @RequestBody(required = false) PeerRequest.SendConnection req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                peerService.sendRequest(receiverId, ud.getUsername(), req),
                "Connection request sent"));
    }

    @PatchMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponse<PeerResponse.ConnectionCard>> accept(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.respondToRequest(id, true, ud.getUsername()),
                "Connection accepted"));
    }

    @PatchMapping("/requests/{id}/decline")
    public ResponseEntity<ApiResponse<PeerResponse.ConnectionCard>> decline(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.respondToRequest(id, false, ud.getUsername()),
                "Connection declined"));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<ApiResponse<List<PeerResponse.ConnectionCard>>> pending(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.getPendingRequests(ud.getUsername())));
    }

    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<List<PeerResponse.UserCard>>> myConnections(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.getMyConnections(ud.getUsername())));
    }

    // ── GROUPS ────────────────────────────────────────────────────────────────

    @GetMapping("/groups/discover")
    public ResponseEntity<ApiResponse<List<PeerResponse.GroupSummary>>> discoverGroups(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String hackathon,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.discoverGroups(domain, hackathon, userId(ud))));
    }

    @GetMapping("/groups/my")
    public ResponseEntity<ApiResponse<List<PeerResponse.GroupSummary>>> myGroups(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.getMyGroups(ud.getUsername())));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<PeerResponse.GroupDetail>> getGroup(
            @PathVariable Long groupId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.getGroupDetail(groupId, ud.getUsername())));
    }

    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<PeerResponse.GroupSummary>> createGroup(
            @Valid @RequestBody PeerRequest.CreateGroup req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                peerService.createGroup(req, ud.getUsername()),
                "Group created successfully"));
    }

    @PostMapping("/groups/{groupId}/join")
    public ResponseEntity<ApiResponse<PeerResponse.GroupSummary>> joinGroup(
            @PathVariable Long groupId, @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.joinGroup(groupId, ud.getUsername()),
                "Joined group"));
    }

    @DeleteMapping("/groups/{groupId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(
            @PathVariable Long groupId, @AuthenticationPrincipal UserDetails ud) {
        peerService.leaveGroup(groupId, ud.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Left group"));
    }

    // ── MESSAGES ──────────────────────────────────────────────────────────────

    @GetMapping("/groups/{groupId}/messages")
    public ResponseEntity<ApiResponse<List<PeerResponse.MessageResponse>>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.ok(
                peerService.getMessages(groupId, ud.getUsername(), limit)));
    }

    @PostMapping("/groups/{groupId}/messages")
    public ResponseEntity<ApiResponse<PeerResponse.MessageResponse>> sendMessage(
            @PathVariable Long groupId,
            @Valid @RequestBody PeerRequest.SendMessage req,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                peerService.sendMessage(groupId, req, ud.getUsername())));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Long userId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .map(u -> u.getId()).orElse(null);
    }
}
