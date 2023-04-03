package gay.pizza.gitops.cloudflare.email

fun RoutingRule.Companion.createForwardRule(to: String, destinations: List<String>): RoutingRule {
  return RoutingRule(
    enabled = true,
    matchers = listOf(
      RoutingRuleMatcher(
        type = "literal",
        field = "to",
        value = to
      )
    ),
    actions = listOf(
      RoutingRuleAction(
        type = "forward",
        value = destinations
      )
    )
  )
}
