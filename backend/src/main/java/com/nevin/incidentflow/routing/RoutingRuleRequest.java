package com.nevin.incidentflow.routing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class RoutingRuleRequest {

    @NotBlank(message = "service is required")
    private String service;

    @NotNull(message = "teamId is required")
    private UUID teamId;

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }
}
