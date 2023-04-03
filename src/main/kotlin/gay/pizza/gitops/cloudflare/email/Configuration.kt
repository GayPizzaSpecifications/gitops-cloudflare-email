package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
  val domain: String,
  val account: String,
  val zone: String,
  val groups: Map<String, List<String>> = emptyMap(),
  val forwards: Map<String, String> = emptyMap(),
  @SerialName("catch-all")
  val catchAll: String
) {
  fun generateRoutingRules(): List<CloudflareEmailClient.RoutingRule> {
    val catchAllRule = CloudflareEmailClient.RoutingRule(
      enabled = true,
      matchers = listOf(
        CloudflareEmailClient.RoutingRuleMatcher(
          type = "all"
        )
      ),
      actions = listOf(
        CloudflareEmailClient.RoutingRuleAction(
          type = "forward",
          value = listOf(catchAll)
        )
      ),
      priority = Int.MAX_VALUE
    )

    val generatedForwardRules = forwards.map { (emailName, destination) ->
      makeRoutingRule(emailName, listOf(destination))
    }

    val generatedGroupRules = groups.map { (emailName, destinations) ->
      makeRoutingRule(emailName, destinations)
    }

    return listOf(catchAllRule) + generatedForwardRules + generatedGroupRules
  }

  private fun makeRoutingRule(emailName: String, destinations: List<String>): CloudflareEmailClient.RoutingRule {
    return CloudflareEmailClient.RoutingRule(
      enabled = true,
      matchers = listOf(
        CloudflareEmailClient.RoutingRuleMatcher(
          type = "literal",
          field = "to",
          value = "${emailName}@${domain}"
        )
      ),
      actions = listOf(
        CloudflareEmailClient.RoutingRuleAction(
          type = "forward",
          value = destinations
        )
      )
    )
  }

  fun collectAllAddresses(): List<String> = listOf(
    listOf(catchAll),
    forwards.map { forward -> forward.value },
    groups.map { group -> group.value }.flatten()
  ).flatten().toSet().toList()
}
