package com.nevin.incidentflow.routing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {

    Optional<RoutingRule> findByService(String service);

    List<RoutingRule> findAllByOrderByServiceAsc();
}
