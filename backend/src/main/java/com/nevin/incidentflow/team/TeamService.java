package com.nevin.incidentflow.team;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final ResponseTeamRepository responseTeamRepository;

    public TeamService(ResponseTeamRepository responseTeamRepository) {
        this.responseTeamRepository = responseTeamRepository;
    }

    public List<ResponseTeam> listTeams() {
        return responseTeamRepository.findAll();
    }

    public ResponseTeam createTeam(TeamRequest request) {
        ResponseTeam team = new ResponseTeam(request.getName(), request.getDescription(), request.isDefault());
        return responseTeamRepository.save(team);
    }
}
