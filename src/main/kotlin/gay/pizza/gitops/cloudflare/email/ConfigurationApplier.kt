package gay.pizza.gitops.cloudflare.email

class ConfigurationApplier(private val client: CloudflareEmailClient, private val config: Configuration) {
  suspend fun apply(dry: Boolean) {
    val existingRoutingRules = client.listRoutingRules(config.zone)
    val generatedRoutingRules = config.forwards.map { (emailName, destination) ->
      CloudflareEmailClient.RoutingRule(
        enabled = true,
        matchers = listOf(
          CloudflareEmailClient.RoutingRuleMatcher(
            type = "literal",
            field = "to",
            value = "${emailName}@${config.domain}"
          )
        ),
        actions = listOf(
          CloudflareEmailClient.RoutingRuleAction(
            type = "forward",
            value = listOf(destination)
          )
        )
      )
    } + CloudflareEmailClient.RoutingRule(
      enabled = true,
      matchers = listOf(
        CloudflareEmailClient.RoutingRuleMatcher(
          type = "all"
        )
      ),
      actions = listOf(
        CloudflareEmailClient.RoutingRuleAction(
          type = "forward",
          value = listOf(config.catchAll)
        )
      ),
      priority = Int.MAX_VALUE
    )

    val tense = if (dry) "would" else "will"

    val wouldRemove = existingRoutingRules.filter { existing ->
      !generatedRoutingRules.any { generated -> existing.simpleEquals(generated) } }
    val wouldAdd = generatedRoutingRules.filter { generated ->
      !existingRoutingRules.any { existing -> generated.simpleEquals(existing) } }

    fun ruleOrRules(list: List<*>) = if (list.size == 1) "rule" else "rules"

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

    // From here on out, we actually handle the catch-all weirdness.
    val catchAllRuleToAdd = wouldAdd.firstOrNull { it.matchers[0].type == "all" }
    val catchAllRuleToRemove = wouldRemove.firstOrNull { it.matchers[0].type == "all" }

    if (catchAllRuleToRemove != null && catchAllRuleToAdd == null) {
      throw RuntimeException("Cannot remove catch-all without adding a catch-all as well!")
    }

    if (catchAllRuleToAdd != null) {
      val result = client.updateCatchAllRule(config.zone, catchAllRuleToAdd)
      if (!result.success) {
        throw RuntimeException("Failed to update catch-all rule '${catchAllRuleToAdd.summarize()}': API call failed.")
      }
    }

    for (rule in wouldRemove.filter { it != catchAllRuleToRemove }) {
      if (rule.tag == null) throw RuntimeException("Unable to delete rule '${rule.summarize()}': tag unknown")
      val result = client.deleteRoutingRule(config.zone, rule.tag)
      if (!result.success) {
        throw RuntimeException("Failed to delete rule '${rule.summarize()}': API call failed.")
      }
    }

    for (rule in wouldAdd.filter { it != catchAllRuleToAdd }) {
      val result = client.addRoutingRule(config.zone, rule)
      if (!result.success) {
        throw RuntimeException("Failed to add rule '${rule.summarize()}': API call failed.")
      }
    }

    println("email configuration applied")
    client.close()
  }
}
