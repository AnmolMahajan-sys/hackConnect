package com.hackconnect.service;

import com.hackconnect.dto.request.OpportunityRequest;
import com.hackconnect.dto.response.OpportunityResponse;
import com.hackconnect.exception.HackConnectException;
import com.hackconnect.model.Opportunity;
import com.hackconnect.model.User;
import com.hackconnect.repository.OpportunityRepository;
import com.hackconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock OpportunityRepository opportunityRepository;
    @Mock UserRepository        userRepository;
    @InjectMocks OpportunityService service;

    private User admin;
    private Opportunity sampleOpp;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).email("admin@hc.dev")
                .role(User.Role.ADMIN).active(true).build();

        sampleOpp = Opportunity.builder()
                .id(10L)
                .title("Test Hackathon")
                .type(Opportunity.Type.HACKATHON)
                .organizer("Test Org")
                .domain("Web Dev")
                .verified(true)
                .deadline(LocalDateTime.now().plusDays(30))
                .tags(Set.of("test"))
                .postedBy(admin)
                .viewCount(5)
                .build();
    }

    /* ── filter ─────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("filter() returns paginated results")
    void filter_returnsMappedPage() {
        Page<Opportunity> page = new PageImpl<>(List.of(sampleOpp));
        when(opportunityRepository.filter(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<OpportunityResponse> result =
                service.filter(null, null, null, null, null, false, 0, 10, "deadline");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Hackathon");
    }

    /* ── getById ─────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("getById() increments view count and returns response")
    void getById_incrementsViews() {
        when(opportunityRepository.findById(10L)).thenReturn(Optional.of(sampleOpp));

        OpportunityResponse resp = service.getById(10L);

        verify(opportunityRepository).incrementViewCount(10L);
        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getDaysUntilDeadline()).isPositive();
    }

    @Test
    @DisplayName("getById() throws ResourceNotFoundException for unknown id")
    void getById_notFound() {
        when(opportunityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(HackConnectException.ResourceNotFoundException.class);
    }

    /* ── create ──────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("create() saves and returns response")
    void create_persistsOpportunity() {
        when(userRepository.findByEmail("admin@hc.dev")).thenReturn(Optional.of(admin));
        when(opportunityRepository.save(any())).thenAnswer(inv -> {
            Opportunity o = inv.getArgument(0);
            o = Opportunity.builder().id(99L).title(o.getTitle()).type(o.getType())
                    .organizer(o.getOrganizer()).domain(o.getDomain()).verified(false)
                    .tags(o.getTags() != null ? o.getTags() : Set.of()).postedBy(admin).build();
            return o;
        });

        OpportunityRequest req = new OpportunityRequest();
        req.setTitle("New Hack");
        req.setType(Opportunity.Type.HACKATHON);
        req.setOrganizer("Org");
        req.setDomain("AI/ML");

        OpportunityResponse resp = service.create(req, "admin@hc.dev");

        assertThat(resp.getTitle()).isEqualTo("New Hack");
        assertThat(resp.isVerified()).isFalse();
    }

    /* ── verify ──────────────────────────────────────────────────────────── */

    @Test
    @DisplayName("verify() sets verified=true")
    void verify_setsVerifiedTrue() {
        sampleOpp.setVerified(false);
        when(opportunityRepository.findById(10L)).thenReturn(Optional.of(sampleOpp));
        when(opportunityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OpportunityResponse resp = service.verify(10L);

        assertThat(resp.isVerified()).isTrue();
    }

    /* ── getUpcoming ─────────────────────────────────────────────────────── */

    @Test
    @DisplayName("getUpcoming() returns up to limit results")
    void getUpcoming_returnsLimitedResults() {
        when(opportunityRepository.findUpcoming(any(), any()))
                .thenReturn(List.of(sampleOpp));

        List<OpportunityResponse> result = service.getUpcoming(5);

        assertThat(result).hasSize(1);
    }
}
