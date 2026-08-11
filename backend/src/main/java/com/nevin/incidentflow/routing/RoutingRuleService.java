package com.nevin.incidentflow.routing;

import com.nevin.incidentflow.team.ResponseTeam;
import com.nevin.incidentflow.team.ResponseTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoutingRuleService {

    private final RoutingRuleRepository routingRuleRepository;
    private final ResponseTeamRepository responseTeamRepository;

    public RoutingRuleService(RoutingRuleRepository routingRuleRepository,
                               ResponseTeamRepository responseTeamRepository) {
        this.routingRuleRepository = routingRuleRepository;
        this.responseTeamRepository = responseTeamRepository;
    }

    public List<RoutingRule> listRules() {
        return routingRuleRepository.findAllByOrderByServiceAsc();
    }

    public RoutingRule createRule(RoutingRuleRequest request) {
        if (routingRuleRepository.findByService(request.getService()).isPresent()) {
            throw new RoutingRuleConflictException(request.getService());
        }
        ResponseTeam team = findTeam(request.getTeamId());
        return routingRuleRepository.save(new RoutingRule(request.getService(), team));
    }

    @Transactional
    public RoutingRule updateRule(UUID ruleId, RoutingRuleRequest request) {
        RoutingRule rule = routingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RoutingRuleNotFoundException(ruleId));
        rule.setTeam(findTeam(request.getTeamId()));
        return rule;
    }

    public void deleteRule(UUID ruleId) {
        if (!routingRuleRepository.existsById(ruleId)) {
            throw new RoutingRuleNotFoundException(ruleId);
        }
        routingRuleRepository.deleteById(ruleId);
    }

    private ResponseTeam findTeam(UUID teamId) {
        return responseTeamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));
    }
}
