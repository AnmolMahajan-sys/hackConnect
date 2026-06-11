package com.hackconnect.dto.request;

import com.hackconnect.model.Opportunity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class OpportunityRequest {

    @NotBlank @Size(max = 200)
    private String title;

    private String description;

    @NotNull
    private Opportunity.Type type;

    @NotBlank @Size(max = 150)
    private String organizer;

    @Size(max = 200)
    private String location;

    private boolean online;

    private LocalDateTime startDate;

    private LocalDateTime deadline;

    @Size(max = 500)
    private String registrationUrl;

    @Size(max = 200)
    private String prize;

    @NotBlank @Size(max = 80)
    private String domain;

    private Set<String> tags;
}
