package com.hackconnect.service;

import com.hackconnect.dto.request.RoadmapRequest;
import com.hackconnect.dto.response.RoadmapResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.*;
import com.hackconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock LearningRoadmapRepository     roadmapRepository;
    @Mock RoadmapStepRepository         stepRepository;
    @Mock UserRoadmapProgressRepository progressRepository;
    @Mock UserRepository                userRepository;
    @InjectMocks RoadmapService service;

    private User student;
    private LearningRoadmap roadmap;
    private RoadmapStep step1, step2;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1L).email("student@iit.ac.in")
                .role(User.Role.STUDENT).active(true).build();

        roadmap = LearningRoadmap.builder()
                .id(1L)
                .title("Full Stack")
                .domain("Full Stack")
                .level(LearningRoadmap.Level.BEGINNER)
                .estimatedWeeks(16)
                .enrolledCount(100)
                .steps(new ArrayList<>())
                .build();

        step1 = RoadmapStep.builder().id(1L).title("HTML & CSS").stepOrder(1)
                .roadmap(roadmap).resources(new ArrayList<>()).projectIdeas(new ArrayList<>()).build();
        step2 = RoadmapStep.builder().id(2L).title("JavaScript").stepOrder(2)
                .roadmap(roadmap).resources(new ArrayList<>()).projectIdeas(new ArrayList<>()).build();

        roadmap.getSteps().addAll(List.of(step1, step2));
    }

    /* ── getById ─────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("getById() returns detail without user context")
    void getById_noUser_returnsDetail() {
        when(roadmapRepository.findById(1L)).thenReturn(Optional.of(roadmap));

        RoadmapResponse.Detail detail = service.getById(1L, null);

        assertThat(detail.getTitle()).isEqualTo("Full Stack");
        assertThat(detail.getSteps()).hasSize(2);
        assertThat(detail.getSteps().get(0).getStatus()).isNull();
    }

    @Test
    @DisplayName("getById() throws for unknown roadmap")
    void getById_notFound() {
        when(roadmapRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L, null))
                .isInstanceOf(HackConnectException.ResourceNotFoundException.class);
    }

    /* ── enroll ──────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("enroll() creates progress rows for each step")
    void enroll_createsProgressRows() {
        when(roadmapRepository.findById(1L)).thenReturn(Optional.of(roadmap));
        when(userRepository.findByEmail("student@iit.ac.in")).thenReturn(Optional.of(student));
        when(progressRepository.existsByUserIdAndRoadmapId(1L, 1L)).thenReturn(false);

        service.enroll(1L, "student@iit.ac.in");

        verify(progressRepository, times(2)).save(any(UserRoadmapProgress.class));
        verify(roadmapRepository).incrementEnrolledCount(1L);
    }

    @Test
    @DisplayName("enroll() throws DuplicateResourceException on re-enrol")
    void enroll_duplicateThrows() {
        when(roadmapRepository.findById(1L)).thenReturn(Optional.of(roadmap));
        when(userRepository.findByEmail("student@iit.ac.in")).thenReturn(Optional.of(student));
        when(progressRepository.existsByUserIdAndRoadmapId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.enroll(1L, "student@iit.ac.in"))
                .isInstanceOf(HackConnectException.DuplicateResourceException.class)
                .hasMessageContaining("Already enrolled");
    }

    /* ── updateStepStatus ────────────────────────────────────────────────── */

    @Test
    @DisplayName("updateStepStatus() marks step COMPLETED and sets completedAt")
    void updateStepStatus_completed() {
        UserRoadmapProgress progress = UserRoadmapProgress.builder()
                .id(1L).user(student).roadmap(roadmap).step(step1)
                .status(UserRoadmapProgress.Status.NOT_STARTED).build();

        when(userRepository.findByEmail("student@iit.ac.in")).thenReturn(Optional.of(student));
        when(progressRepository.existsByUserIdAndRoadmapId(1L, 1L)).thenReturn(true);
        when(progressRepository.findByUserIdAndStepId(1L, 1L)).thenReturn(Optional.of(progress));
        when(progressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoadmapResponse.StepDetail result = service.updateStepStatus(
                1L, 1L, UserRoadmapProgress.Status.COMPLETED, "student@iit.ac.in");

        assertThat(result.getStatus()).isEqualTo(UserRoadmapProgress.Status.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStepStatus() throws if not enrolled")
    void updateStepStatus_notEnrolled() {
        when(userRepository.findByEmail("student@iit.ac.in")).thenReturn(Optional.of(student));
        when(progressRepository.existsByUserIdAndRoadmapId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.updateStepStatus(
                1L, 1L, UserRoadmapProgress.Status.COMPLETED, "student@iit.ac.in"))
                .isInstanceOf(HackConnectException.BadRequestException.class)
                .hasMessageContaining("Not enrolled");
    }

    /* ── create ──────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("create() persists roadmap and returns detail")
    void create_persistsRoadmap() {
        when(roadmapRepository.save(any())).thenAnswer(inv -> {
            LearningRoadmap r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });

        RoadmapRequest.Create req = new RoadmapRequest.Create();
        req.setTitle("New Roadmap");
        req.setDomain("DevOps");
        req.setLevel(LearningRoadmap.Level.ADVANCED);
        req.setEstimatedWeeks(8);

        RoadmapResponse.Detail detail = service.create(req);

        assertThat(detail.getTitle()).isEqualTo("New Roadmap");
        assertThat(detail.getDomain()).isEqualTo("DevOps");
    }
}
