package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.json.Json

fun CloudflareEmailClient.RoutingRule.summarize(): String {
  val canBeEasySummarized = matchers.size == 1 &&
    actions.size == 1 &&
    ((matchers[0].type == "literal" &&
      matchers[0].field == "to") || (matchers[0].type == "all"))
    actions[0].type == "forward" &&
    actions[0].value.size == 1

  if (!canBeEasySummarized) {
    return "complex rule ${Json.encodeToString(CloudflareEmailClient.RoutingRule.serializer(), this)}"
  }

  if (matchers[0].type == "all") {
    return "forward all other mail to ${actions[0].value[0]}"
  }

  return "forward mail from ${matchers[0].value} to ${actions[0].value[0]}"
}
