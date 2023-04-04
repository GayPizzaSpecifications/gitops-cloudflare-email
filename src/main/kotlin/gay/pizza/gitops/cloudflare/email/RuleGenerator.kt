package gay.pizza.gitops.cloudflare.email

fun Configuration.generateRoutingRules(domainConfig: DomainConfiguration): List<RoutingRule> {
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
    RoutingRule.createForwardRule(domainConfig.createDomainEmail(emailName), listOf(destination))
  }

  val generatedGroupRules = groups.map { (emailName, destinations) ->
    RoutingRule.createForwardRule(domainConfig.createDomainEmail(emailName), destinations)
  }

  val generatedWorkerRules = workers.map { (emailName, workerName) ->
    RoutingRule.createWorkerRule(domainConfig.createDomainEmail(emailName), listOf(workerName))
  }

  val generatedDropRules = drop.map { emailName ->
    RoutingRule.createDropRule(domainConfig.createDomainEmail(emailName))
  }

  return listOf(catchAllRule) + generatedForwardRules +
    generatedGroupRules + generatedWorkerRules +
    generatedDropRules
}
