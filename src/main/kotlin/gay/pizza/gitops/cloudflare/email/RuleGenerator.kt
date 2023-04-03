package gay.pizza.gitops.cloudflare.email

fun Configuration.generateRoutingRules(): List<RoutingRule> {
  val catchAllRule = RoutingRule(
    enabled = true,
    matchers = listOf(
      RoutingRuleMatcher(
        type = "all"
      )
    ),
    actions = listOf(
      RoutingRuleAction(
        type = "forward",
        value = listOf(catchAll)
      )
    ),
    priority = Int.MAX_VALUE
  )

  val generatedForwardRules = forwards.map { (emailName, destination) ->
    RoutingRule.createForwardRule(createDomainEmail(emailName), listOf(destination))
  }

  val generatedGroupRules = groups.map { (emailName, destinations) ->
    RoutingRule.createForwardRule(createDomainEmail(emailName), destinations)
  }

  return listOf(catchAllRule) + generatedForwardRules + generatedGroupRules
}
