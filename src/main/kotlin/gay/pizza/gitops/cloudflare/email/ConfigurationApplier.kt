package gay.pizza.gitops.cloudflare.email

suspend fun CloudflareEmailClient.applyConfiguration(
  config: Configuration,
  domainConfig: DomainConfiguration,
  dry: Boolean
) {
  println("generating rules for domain ${domainConfig.domain}")
  val existingRoutingRules = listRoutingRules(domainConfig.zone)
  val generatedRoutingRules = config.generateRoutingRules(domainConfig)
  val existingDestinationAddresses = listDestinationAddresses(domainConfig.account)
  val requiredDestinationAddresses = config.collectDestinationAddresses()

  val tense = if (dry) "would" else "will"

  val wouldRemove = existingRoutingRules.filter { existing ->
    !generatedRoutingRules.any { generated -> existing.simpleEquals(generated) } }
  val wouldAdd = generatedRoutingRules.filter { generated ->
    !existingRoutingRules.any { existing -> generated.simpleEquals(existing) } }

  val wouldAddDestinations = requiredDestinationAddresses.filter { required ->
    !existingDestinationAddresses.any { existing ->
      required.email == existing.email
    }
  }

  fun List<*>.plurality(singular: String) =
    if (size == 1) singular else "${singular}s"

  println("found ${existingDestinationAddresses.size} existing destinations")
  for (existing in existingDestinationAddresses) {
    if (existing.verified == null) {
      println("warning: destination ${existing.email} is not verified, emails will not be forwarded")
    }
  }
  if (wouldAddDestinations.isEmpty()) {
    println("$tense add no destinations")
  } else {
    println("$tense add ${wouldAddDestinations.size} ${wouldAddDestinations.plurality("destination")}")
    for (destination in wouldAddDestinations) {
      println("  ${destination.email}")
    }
  }

  println("found ${existingRoutingRules.size} existing ${existingRoutingRules.plurality("rule")}")

  if (wouldAdd.isEmpty()) {
    println("$tense add no rules")
  } else {
    println("$tense add ${wouldAdd.size} ${wouldAdd.plurality("rule")}:")
    for (rule in wouldAdd) {
      println("  ${rule.summarize()}")
    }
  }

  if (wouldRemove.isEmpty()) {
    println("$tense remove no rules")
  } else {
    println("$tense remove ${wouldRemove.size} ${wouldRemove.plurality("rule")}:")
    for (rule in wouldRemove) {
      println("  ${rule.summarize()}")
    }
  }

  if (dry) {
    return
  }

  if (wouldAdd.isEmpty() && wouldRemove.isEmpty()) {
    println("nothing to be done")
    return
  }

  println("applying email configuration for domain ${domainConfig.domain}")

  for (destination in wouldAddDestinations) {
    val result = addDestinationAddress(domainConfig.account, destination)
    result.check("add destination address '${destination.email}'")
  }

  // From here on out, we actually handle the catch-all weirdness.
  val catchAllRuleToAdd = wouldAdd.firstOrNull { it.matchers[0].type == "all" }
  val catchAllRuleToRemove = wouldRemove.firstOrNull { it.matchers[0].type == "all" }

  if (catchAllRuleToRemove != null && catchAllRuleToAdd == null) {
    throw RuntimeException("Cannot remove catch-all without adding a catch-all as well!")
  }

  if (catchAllRuleToAdd != null) {
    val result = updateCatchAllRule(domainConfig.zone, catchAllRuleToAdd)
    result.check("update catch-all rule '${catchAllRuleToAdd.summarize()}'")
  }

  for (rule in wouldRemove.filter { it != catchAllRuleToRemove }) {
    if (rule.tag == null) {
      throw RuntimeException("Unable to delete rule '${rule.summarize()}': tag unknown")
    }
    val result = deleteRoutingRule(domainConfig.zone, rule.tag)
    result.check("delete rule '${rule.summarize()}'")
  }

  for (rule in wouldAdd.filter { it != catchAllRuleToAdd }) {
    val result = addRoutingRule(domainConfig.zone, rule)
    result.check("add rule '${rule.summarize()}'")
  }

  println("email configuration applied for domain ${domainConfig.domain}")
}
