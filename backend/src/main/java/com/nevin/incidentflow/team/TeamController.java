package com.nevin.incidentflow.team;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ResponseEntity<List<ResponseTeam>> listTeams() {
        return ResponseEntity.ok(teamService.listTeams());
    }

    @PostMapping
    public ResponseEntity<ResponseTeam> createTeam(@jakarta.validation.Valid @RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }
}
