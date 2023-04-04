package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.json.Json

fun RoutingRule.summarize(): String {
  val canBeEasySummarized = matchers.size == 1 &&
    ((matchers[0].type == "literal" &&
      matchers[0].field == "to") || (matchers[0].type == "all")) &&
    actions.all { action -> listOf("forward", "worker", "drop").contains(action.type) } &&
    actions.map { action -> action.type }.toSet().size == 1

  if (!canBeEasySummarized) {
    return "complex rule ${Json.encodeToString(RoutingRule.serializer(), this)}"
  }

  val isForward = actions.all { action -> action.type == "forward" }
  val isWorker = actions.all { action -> action.type == "worker" }
  val isDrop = actions.all { action -> action.type == "drop" }

  val isCatchAll = matchers[0].type == "all"

  if (isForward) {
    val destinations = actions.map { it.value }.flatten().toSet()
    if (isCatchAll) {
      return "forward all other mail to ${destinations.joinToString(", ")}"
    }
    return "forward mail from ${matchers[0].value} to ${destinations.joinToString(", ")}"
  }

  if (isWorker) {
    val workerNames = actions.map { it.value }.flatten().toSet()
    val plural = if (workerNames.size == 1) "worker" else "workers"
    if (isCatchAll) {
      return "send all other mail to $plural ${workerNames.joinToString(", ")}"
    }
    return "send mail from ${matchers[0].value} to $plural ${workerNames.joinToString(", ")}"
  }

  if (isDrop) {
    if (isCatchAll) {
      return "drop all other mail from ${matchers[0].value}"
    }
    return "drop mail from ${matchers[0].value}"
  }
  return "complex rule ${Json.encodeToString(RoutingRule.serializer(), this)}"
}
