package gay.pizza.gitops.cloudflare.email

suspend fun CloudflareEmailClient.applyConfiguration(config: Configuration, dry: Boolean) {
  val existingRoutingRules = listRoutingRules(config.zone)
  val generatedRoutingRules = config.generateRoutingRules()
  val existingDestinationAddresses = listDestinationAddresses(config.account)
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

  fun ruleOrRules(list: List<*>) = if (list.size == 1) "rule" else "rules"
  fun destinationOrDestinations(list: List<*>) = if (list.size == 1) "destination" else "destinations"

  println("found ${existingDestinationAddresses.size} existing destinations")
  for (existing in existingDestinationAddresses) {
    if (existing.verified == null) {
      println("warning: destination ${existing.email} is not verified, emails will not be forwarded")
    }
  }
  if (wouldAddDestinations.isEmpty()) {
    println("$tense add no destinations")
  } else {
    println("$tense add ${wouldAddDestinations.size} ${destinationOrDestinations(wouldAddDestinations)}")
    for (destination in wouldAddDestinations) {
      println("  ${destination.email}")
    }
  }

  println("found ${existingRoutingRules.size} existing ${ruleOrRules(existingRoutingRules)}")

  if (wouldAdd.isEmpty()) {
    println("$tense add no rules")
  } else {
    println("$tense add ${wouldAdd.size} ${ruleOrRules(wouldAdd)}:")
    for (rule in wouldAdd) {
      println("  ${rule.summarize()}")
    }
  }

  if (wouldRemove.isEmpty()) {
    println("$tense remove no rules")
  } else {
    println("$tense remove ${wouldRemove.size} ${ruleOrRules(wouldRemove)}:")
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

  for (destination in wouldAddDestinations) {
    val result = addDestinationAddress(config.account, destination)
    if (!result.success) {
      throw RuntimeException("Failed to add destination address '${destination.email}': API call failed.")
    }
  }

  // From here on out, we actually handle the catch-all weirdness.
  val catchAllRuleToAdd = wouldAdd.firstOrNull { it.matchers[0].type == "all" }
  val catchAllRuleToRemove = wouldRemove.firstOrNull { it.matchers[0].type == "all" }

  if (catchAllRuleToRemove != null && catchAllRuleToAdd == null) {
    throw RuntimeException("Cannot remove catch-all without adding a catch-all as well!")
  }

  if (catchAllRuleToAdd != null) {
    val result = updateCatchAllRule(config.zone, catchAllRuleToAdd)
    if (!result.success) {
      throw RuntimeException("Failed to update catch-all rule '${catchAllRuleToAdd.summarize()}': API call failed.")
    }
  }

  for (rule in wouldRemove.filter { it != catchAllRuleToRemove }) {
    if (rule.tag == null) throw RuntimeException("Unable to delete rule '${rule.summarize()}': tag unknown")
    val result = deleteRoutingRule(config.zone, rule.tag)
    if (!result.success) {
      throw RuntimeException("Failed to delete rule '${rule.summarize()}': API call failed.")
    }
  }

  for (rule in wouldAdd.filter { it != catchAllRuleToAdd }) {
    val result = addRoutingRule(config.zone, rule)
    if (!result.success) {
      throw RuntimeException("Failed to add rule '${rule.summarize()}': API call failed.")
    }
  }

  println("email configuration applied")
}
