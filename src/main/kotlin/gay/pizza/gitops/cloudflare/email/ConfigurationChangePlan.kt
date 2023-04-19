package gay.pizza.gitops.cloudflare.email

data class ConfigurationChangePlan(
  val existingState: CloudflareEmailState,
  val desiredState: CloudflareEmailState,
  val addRoutingRules: List<RoutingRule>,
  val removeRoutingRules: List<RoutingRule>,
  val addDestinationAddresses: List<DestinationAddress>
) {
  fun isEmpty(): Boolean = addRoutingRules.isEmpty() &&
    removeRoutingRules.isEmpty() &&
    addDestinationAddresses.isEmpty()
}

fun ConfigurationChangePlan.describe(dry: Boolean = false): String = buildString {
  val tense = if (dry) "would" else "will"

  fun List<*>.plurality(singular: String) =
    if (size == 1) singular else "${singular}s"

  appendLine("found ${existingState.destinationAddresses.size} existing destinations")
  for (existing in existingState.destinationAddresses) {
    if (existing.verified == null &&
      desiredState.destinationAddresses.any { desired -> desired.email == existing.email }) {
      appendLine("warning: destination ${existing.email} is not verified, emails will not be forwarded")
    }
  }

  if (addDestinationAddresses.isEmpty()) {
    appendLine("$tense add no destinations")
  } else {
    appendLine("$tense add ${addDestinationAddresses.size} ${addDestinationAddresses.plurality("destination")}")
    for (destination in addDestinationAddresses) {
      appendLine("  ${destination.email}")
    }
  }

  appendLine("found ${existingState.routingRules.size} existing ${existingState.routingRules.plurality("rule")}")

  if (addRoutingRules.isEmpty()) {
    appendLine("$tense add no rules")
  } else {
    appendLine("$tense add ${addRoutingRules.size} ${addRoutingRules.plurality("rule")}:")
    for (rule in addRoutingRules) {
      appendLine("  ${rule.summarize()}")
    }
  }

  if (removeRoutingRules.isEmpty()) {
    appendLine("$tense remove no rules")
  } else {
    appendLine("$tense remove ${removeRoutingRules.size} ${removeRoutingRules.plurality("rule")}:")
    for (rule in removeRoutingRules) {
      appendLine("  ${rule.summarize()}")
    }
  }
}
