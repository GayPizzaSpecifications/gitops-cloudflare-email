package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutingRule(
  val name: String? = null,
  val tag: String? = null,
  val enabled: Boolean,
  val matchers: List<RoutingRuleMatcher>,
  val actions: List<RoutingRuleAction>,
  val priority: Int? = null
) {
  fun simpleEquals(other: RoutingRule): Boolean = enabled == other.enabled &&
    matchers.size == other.matchers.size &&
    matchers.all { other.matchers.contains(it) } &&
    actions.size == other.actions.size &&
    actions.all { other.actions.contains(it) }
}

@Serializable
data class RoutingRuleAction(
  val type: String,
  val value: List<String>
)

@Serializable
data class RoutingRuleMatcher(
  val type: String,
  val field: String? = null,
  val value: String? = null
)

@Serializable
data class DestinationAddress(
  val verified: String? = null,
  val email: String
)

@Serializable
data class ApiCallResult<T>(
  val result: T? = null,
  @SerialName("result_info")
  val resultInfo: ApiCallResultInfo? = null,
  val success: Boolean
)

@Serializable
data class ApiCallResultInfo(
  val count: Int,
  val page: Int,
  @SerialName("per_page")
  val perPage: Int,
  @SerialName("total_count")
  val totalCount: Int
)

fun ApiCallResult<*>.check(operationSummary: String) {
  if (success) {
    return
  }

  throw RuntimeException("Failed to $operationSummary: API call failed.")
}
