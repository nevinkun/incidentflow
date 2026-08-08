package com.nevin.incidentflow.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResponseTeamRepository extends JpaRepository<ResponseTeam, UUID> {

    Optional<ResponseTeam> findByIsDefaultTrue();
}
