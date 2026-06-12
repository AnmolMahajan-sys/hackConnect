package com.hackconnect.service;

import com.hackconnect.dto.request.PeerRequest;
import com.hackconnect.dto.response.PeerResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.*;
import com.hackconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeerService {

    private final UserRepository              userRepository;
    private final ConnectionRequestRepository connectionRepo;
    private final GroupRepository             groupRepo;
    private final GroupMemberRepository       memberRepo;
    private final GroupMessageRepository      messageRepo;
    private final SimpMessagingTemplate       messagingTemplate;

    // ─── DISCOVER PEOPLE ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PeerResponse.UserCard> discoverPeople(String domain, String skill,
                                                      String college, Long currentUserId) {
        List<User> users = userRepository.discoverPeers(
                currentUserId,
                blank(domain)  ? null : domain,
                blank(skill)   ? null : skill,
                blank(college) ? null : college);

        return users.stream()
                .map(u -> toUserCard(u, currentUserId))
                .collect(Collectors.toList());
    }

    // ─── CONNECTIONS ──────────────────────────────────────────────────────────

    @Transactional
    public PeerResponse.ConnectionCard sendRequest(Long receiverId, String senderEmail,
                                                   PeerRequest.SendConnection req) {
        User sender   = findByEmail(senderEmail);
        User receiver = findById(receiverId);

        if (sender.getId().equals(receiverId))
            throw new HackConnectException.BadRequestException("You cannot connect with yourself");

        if (connectionRepo.existsBySenderAndReceiver(sender, receiver) ||
                connectionRepo.existsBySenderAndReceiver(receiver, sender))
            throw new HackConnectException.DuplicateResourceException(
                    "Connection request already exists");

        ConnectionRequest conn = ConnectionRequest.builder()
                .sender(sender).receiver(receiver)
                .message(req != null ? req.getMessage() : null)
                .build();

        ConnectionRequest saved = connectionRepo.save(conn);

        // Real-time notification
        messagingTemplate.convertAndSendToUser(
                receiver.getEmail(), "/queue/notifications",
                Map.of("type", "CONNECTION_REQUEST",
                        "from", sender.getName(),
                        "id",   saved.getId()));

        return toConnectionCard(saved);
    }

    @Transactional
    public PeerResponse.ConnectionCard respondToRequest(Long requestId, boolean accept,
                                                        String receiverEmail) {
        ConnectionRequest conn = connectionRepo.findById(requestId)
                .orElseThrow(() -> new HackConnectException
                        .ResourceNotFoundException("Connection request", requestId));

        if (!conn.getReceiver().getEmail().equals(receiverEmail))
            throw new HackConnectException.AccessDeniedException("Not your request");

        if (conn.getStatus() != ConnectionRequest.Status.PENDING)
            throw new HackConnectException.BadRequestException("Request already handled");

        conn.setStatus(accept ? ConnectionRequest.Status.ACCEPTED
                : ConnectionRequest.Status.DECLINED);
        ConnectionRequest saved = connectionRepo.save(conn);

        if (accept) {
            messagingTemplate.convertAndSendToUser(
                    conn.getSender().getEmail(), "/queue/notifications",
                    Map.of("type", "CONNECTION_ACCEPTED",
                            "from", conn.getReceiver().getName()));
        }
        return toConnectionCard(saved);
    }

    @Transactional(readOnly = true)
    public List<PeerResponse.ConnectionCard> getPendingRequests(String email) {
        User user = findByEmail(email);
        return connectionRepo.findByReceiverAndStatus(user, ConnectionRequest.Status.PENDING)
                .stream().map(this::toConnectionCard).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PeerResponse.UserCard> getMyConnections(String email) {
        User user = findByEmail(email);
        return connectionRepo.findAcceptedConnections(user.getId()).stream()
                .map(c -> {
                    User peer = c.getSender().getId().equals(user.getId())
                            ? c.getReceiver() : c.getSender();
                    return toUserCard(peer, user.getId());
                })
                .collect(Collectors.toList());
    }

    // ─── GROUPS ───────────────────────────────────────────────────────────────

    @Transactional
    public PeerResponse.GroupSummary createGroup(PeerRequest.CreateGroup req, String email) {
        User creator = findByEmail(email);

        Group group = Group.builder()
                .name(req.getName()).description(req.getDescription())
                .domain(req.getDomain()).hackathonName(req.getHackathonName())
                .type(req.getType() != null ? req.getType() : Group.GroupType.PROJECT)
                .maxMembers(req.getMaxMembers()).open(req.isOpen())
                .creator(creator).build();

        Group saved = groupRepo.save(group);

        // Creator becomes admin member
        GroupMember admin = GroupMember.builder()
                .group(saved).user(creator).role(GroupMember.Role.ADMIN).build();
        memberRepo.save(admin);
        saved.getMembers().add(admin);

        sendSystemMsg(saved, creator.getName() + " created the group 🎉");
        return toGroupSummary(saved, creator.getId());
    }

    @Transactional
    public PeerResponse.GroupSummary joinGroup(Long groupId, String email) {
        User  user  = findByEmail(email);
        Group group = findGroup(groupId);

        if (!group.isOpen())
            throw new HackConnectException.AccessDeniedException("This group is not open to join");
        if (memberRepo.existsByGroupIdAndUserId(groupId, user.getId()))
            throw new HackConnectException.DuplicateResourceException("Already a member");
        if (group.getMembers().size() >= group.getMaxMembers())
            throw new HackConnectException.BadRequestException("Group is full");

        GroupMember member = GroupMember.builder()
                .group(group).user(user).role(GroupMember.Role.MEMBER).build();
        memberRepo.save(member);
        group.getMembers().add(member);

        sendSystemMsg(group, user.getName() + " joined the group");
        messagingTemplate.convertAndSend("/topic/group/" + groupId,
                Map.of("type", "MEMBER_JOINED", "name", user.getName()));

        return toGroupSummary(group, user.getId());
    }

    @Transactional
    public void leaveGroup(Long groupId, String email) {
        User  user   = findByEmail(email);
        Group group  = findGroup(groupId);

        GroupMember member = memberRepo.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new HackConnectException
                        .BadRequestException("Not a member of this group"));

        if (member.getRole() == GroupMember.Role.ADMIN && group.getMembers().size() > 1)
            throw new HackConnectException.BadRequestException(
                    "Transfer admin role before leaving");

        memberRepo.delete(member);
        sendSystemMsg(group, user.getName() + " left the group");
        messagingTemplate.convertAndSend("/topic/group/" + groupId,
                Map.of("type", "MEMBER_LEFT", "name", user.getName()));
    }

    @Transactional(readOnly = true)
    public List<PeerResponse.GroupSummary> discoverGroups(String domain, String hackathon,
                                                          Long userId) {
        List<Group> groups;
        if (!blank(hackathon))
            groups = groupRepo.findByHackathonNameContainingIgnoreCaseAndOpenTrue(hackathon);
        else if (!blank(domain))
            groups = groupRepo.findByDomainIgnoreCaseAndOpenTrueOrderByCreatedAtDesc(domain);
        else
            groups = groupRepo.findByOpenTrueOrderByCreatedAtDesc();

        return groups.stream().map(g -> toGroupSummary(g, userId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PeerResponse.GroupSummary> getMyGroups(String email) {
        User user = findByEmail(email);
        return groupRepo.findByMemberId(user.getId()).stream()
                .map(g -> toGroupSummary(g, user.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PeerResponse.GroupDetail getGroupDetail(Long groupId, String email) {
        User  user  = findByEmail(email);
        Group group = findGroup(groupId);
        return toGroupDetail(group, user.getId());
    }

    // ─── MESSAGES ─────────────────────────────────────────────────────────────

    @Transactional
    public PeerResponse.MessageResponse sendMessage(Long groupId,
                                                    PeerRequest.SendMessage req,
                                                    String email) {
        User  user  = findByEmail(email);
        Group group = findGroup(groupId);

        if (!memberRepo.existsByGroupIdAndUserId(groupId, user.getId()))
            throw new HackConnectException.AccessDeniedException("Join the group to send messages");

        GroupMessage msg = GroupMessage.builder()
                .group(group).sender(user).content(req.getContent())
                .type(req.getType() != null ? req.getType() : GroupMessage.MessageType.TEXT)
                .build();

        GroupMessage saved = messageRepo.save(msg);
        PeerResponse.MessageResponse response = toMsgResponse(saved);

        // Push to all WebSocket subscribers of this group
        messagingTemplate.convertAndSend("/topic/group/" + groupId, response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<PeerResponse.MessageResponse> getMessages(Long groupId, String email, int limit) {
        User user = findByEmail(email);
        if (!memberRepo.existsByGroupIdAndUserId(groupId, user.getId()))
            throw new HackConnectException.AccessDeniedException("Not a member of this group");

        List<GroupMessage> msgs = messageRepo.findByGroupIdOrderBySentAtDesc(
                groupId, PageRequest.of(0, Math.min(limit, 100)));
        Collections.reverse(msgs); // oldest first
        return msgs.stream().map(this::toMsgResponse).collect(Collectors.toList());
    }

    // ─── MAPPING HELPERS ──────────────────────────────────────────────────────

    private PeerResponse.UserCard toUserCard(User u, Long viewerUserId) {
        PeerResponse.ConnectionStatus status = PeerResponse.ConnectionStatus.NONE;

        if (viewerUserId != null) {
            if (connectionRepo.areConnected(viewerUserId, u.getId())) {
                status = PeerResponse.ConnectionStatus.CONNECTED;
            } else {
                User viewer = userRepository.findById(viewerUserId).orElse(null);
                if (viewer != null) {
                    connectionRepo.findBySenderAndReceiver(viewer, u)
                            .filter(c -> c.getStatus() == ConnectionRequest.Status.PENDING)
                            .ifPresent(c -> {});
                    Optional<ConnectionRequest> sent     = connectionRepo.findBySenderAndReceiver(viewer, u);
                    Optional<ConnectionRequest> received = connectionRepo.findBySenderAndReceiver(u, viewer);

                    if (sent.isPresent() && sent.get().getStatus() == ConnectionRequest.Status.PENDING)
                        status = PeerResponse.ConnectionStatus.PENDING_SENT;
                    else if (received.isPresent() && received.get().getStatus() == ConnectionRequest.Status.PENDING)
                        status = PeerResponse.ConnectionStatus.PENDING_RECEIVED;
                }
            }
        }

        return PeerResponse.UserCard.builder()
                .id(u.getId()).name(u.getName()).college(u.getCollege())
                .graduationYear(u.getGraduationYear()).bio(u.getBio())
                .skills(u.getSkills()).interests(u.getInterests())
                .githubUrl(u.getGithubUrl()).linkedinUrl(u.getLinkedinUrl())
                .connectionStatus(status)
                .build();
    }

    private PeerResponse.ConnectionCard toConnectionCard(ConnectionRequest c) {
        return PeerResponse.ConnectionCard.builder()
                .id(c.getId())
                .from(toUserCard(c.getSender(), null))
                .to(toUserCard(c.getReceiver(), null))
                .status(c.getStatus()).message(c.getMessage())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private PeerResponse.GroupSummary toGroupSummary(Group g, Long userId) {
        boolean isMember = userId != null
                && memberRepo.existsByGroupIdAndUserId(g.getId(), userId);
        return PeerResponse.GroupSummary.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription())
                .domain(g.getDomain()).hackathonName(g.getHackathonName()).type(g.getType())
                .memberCount(g.getMembers().size()).maxMembers(g.getMaxMembers())
                .open(g.isOpen()).member(isMember)
                .creator(toUserCard(g.getCreator(), userId))
                .createdAt(g.getCreatedAt())
                .build();
    }

    private PeerResponse.GroupDetail toGroupDetail(Group g, Long userId) {
        boolean isMember = userId != null
                && memberRepo.existsByGroupIdAndUserId(g.getId(), userId);
        boolean isAdmin  = g.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && m.getRole() == GroupMember.Role.ADMIN);

        List<PeerResponse.MemberCard> members = g.getMembers().stream()
                .map(m -> PeerResponse.MemberCard.builder()
                        .userId(m.getUser().getId()).name(m.getUser().getName())
                        .college(m.getUser().getCollege()).skills(m.getUser().getSkills())
                        .role(m.getRole()).joinedAt(m.getJoinedAt()).build())
                .collect(Collectors.toList());

        return PeerResponse.GroupDetail.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription())
                .domain(g.getDomain()).hackathonName(g.getHackathonName()).type(g.getType())
                .maxMembers(g.getMaxMembers()).open(g.isOpen())
                .member(isMember).admin(isAdmin)
                .creator(toUserCard(g.getCreator(), userId))
                .members(members).createdAt(g.getCreatedAt())
                .build();
    }

    private PeerResponse.MessageResponse toMsgResponse(GroupMessage m) {
        return PeerResponse.MessageResponse.builder()
                .id(m.getId()).groupId(m.getGroup().getId())
                .senderId(m.getSender().getId()).senderName(m.getSender().getName())
                .content(m.getContent()).type(m.getType()).sentAt(m.getSentAt())
                .build();
    }

    private void sendSystemMsg(Group group, String content) {
        GroupMessage msg = GroupMessage.builder()
                .group(group).sender(group.getCreator())
                .content(content).type(GroupMessage.MessageType.SYSTEM).build();
        GroupMessage saved = messageRepo.save(msg);
        messagingTemplate.convertAndSend("/topic/group/" + group.getId(), toMsgResponse(saved));
    }

    // ─── MISC HELPERS ─────────────────────────────────────────────────────────

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HackConnectException
                        .ResourceNotFoundException("User not found: " + email));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new HackConnectException
                        .ResourceNotFoundException("User", id));
    }

    private Group findGroup(Long id) {
        return groupRepo.findById(id)
                .orElseThrow(() -> new HackConnectException
                        .ResourceNotFoundException("Group", id));
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
