package gay.pizza.gitops.cloudflare.email

suspend fun CloudflareEmailClient.applyConfigurationPlan(
  plan: ConfigurationChangePlan,
  domainConfig: DomainConfiguration
) {
  if (plan.isEmpty()) {
    println("[${domainConfig.domain}] nothing to be done")
    return
  }

  println("[${domainConfig.domain}] applying email configuration")

  for (destination in plan.addDestinationAddresses) {
    val result = addDestinationAddress(domainConfig.account, destination)
    result.check("add destination address '${destination.email}'")
  }

  // From here on out, we actually handle the catch-all weirdness.
  val catchAllRuleToAdd = plan.addRoutingRules.firstOrNull { it.matchers[0].type == "all" }
  val catchAllRuleToRemove = plan.removeRoutingRules.firstOrNull { it.matchers[0].type == "all" }

  if (catchAllRuleToRemove != null && catchAllRuleToAdd == null) {
    throw RuntimeException("Cannot remove catch-all without adding a catch-all as well!")
  }

  if (catchAllRuleToAdd != null) {
    val result = updateCatchAllRule(domainConfig.zone, catchAllRuleToAdd)
    result.check("update catch-all rule '${catchAllRuleToAdd.summarize()}'")
  }

  for (rule in plan.removeRoutingRules.filter { it != catchAllRuleToRemove }) {
    if (rule.tag == null) {
      throw RuntimeException("Unable to delete rule '${rule.summarize()}': tag unknown")
    }
    val result = deleteRoutingRule(domainConfig.zone, rule.tag)
    result.check("delete rule '${rule.summarize()}'")
  }

  for (rule in plan.addRoutingRules.filter { it != catchAllRuleToAdd }) {
    val result = addRoutingRule(domainConfig.zone, rule)
    result.check("add rule '${rule.summarize()}'")
  }

  println("[${domainConfig.domain}] email configuration applied")
}
