package com.nevin.incidentflow.team;

import jakarta.validation.constraints.NotBlank;

public class TeamRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private boolean isDefault;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}
