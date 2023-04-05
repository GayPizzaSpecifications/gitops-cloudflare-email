package gay.pizza.gitops.cloudflare.email

fun RoutingRule.Companion.create(to: String, type: String, value: List<String>): RoutingRule = RoutingRule(
  enabled = true,
  matchers = listOf(
    RoutingRuleMatcher(
      type = "literal",
      field = "to",
      value = to
    )
  ),
  actions = listOf(
    RoutingRuleAction(
      type = type,
      value = value
    )
  )
)

fun RoutingRule.Companion.createForwardRule(to: String, destinations: List<String>): RoutingRule =
  create(to, "forward", destinations)

fun RoutingRule.Companion.createDropRule(to: String): RoutingRule =
  create(to, "drop", emptyList())

fun RoutingRule.Companion.createWorkerRule(to: String, workers: List<String>): RoutingRule =
  create(to, "worker", workers)
