export interface Team {
  id: string
  name: string
}

export interface RoutingRule {
  id: string
  service: string
  team: Team
}
