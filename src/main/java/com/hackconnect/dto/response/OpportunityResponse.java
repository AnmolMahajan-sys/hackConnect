package com.hackconnect.dto.response;

import com.hackconnect.model.Opportunity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class OpportunityResponse {
    private Long   id;
    private String title;
    private String description;
    private Opportunity.Type type;
    private String organizer;
    private String location;
    private boolean online;
    private LocalDateTime startDate;
    private LocalDateTime deadline;
    private String registrationUrl;
    private String prize;
    private String domain;
    private Set<String> tags;
    private boolean verified;
    private int viewCount;
    private LocalDateTime createdAt;
    /** Days remaining until deadline; null if deadline is null */
    private Long daysUntilDeadline;
}
