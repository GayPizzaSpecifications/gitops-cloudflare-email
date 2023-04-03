package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.json.Json

fun CloudflareEmailClient.RoutingRule.summarize(): String {
  val canBeEasySummarized = matchers.size == 1 &&
    ((matchers[0].type == "literal" &&
      matchers[0].field == "to") || (matchers[0].type == "all"))
    actions.all { action -> action.type == "forward" }

  if (!canBeEasySummarized) {
    return "complex rule ${Json.encodeToString(CloudflareEmailClient.RoutingRule.serializer(), this)}"
  }

  val destinations = actions.map { it.value }.flatten().toSet()

  if (matchers[0].type == "all") {
    return "forward all other mail to ${destinations.joinToString(", ")}"
  }

  return "forward mail from ${matchers[0].value} to ${destinations.joinToString(", ")}"
}
