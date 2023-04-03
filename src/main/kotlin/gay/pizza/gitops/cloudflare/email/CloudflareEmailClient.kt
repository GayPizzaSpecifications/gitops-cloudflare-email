package gay.pizza.gitops.cloudflare.email

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CloudflareEmailClient(private val auth: CloudflareEmailAuth) : AutoCloseable {
  private val httpClient: HttpClient = HttpClient {
    install(ContentNegotiation) {
      json(Json { ignoreUnknownKeys = true })
    }
  }

  private val baseUrl = "https://api.cloudflare.com/client/v4"

  private suspend fun listRoutingRules(zone: String, page: Int): ApiCallResult<List<RoutingRule>> =
    httpClient.get("${baseUrl}/zones/${zone}/email/routing/rules?page=${page}") {
      applyAuthentication()
    }.body()

  private suspend fun getCatchAllRule(zone: String): ApiCallResult<RoutingRule> =
    httpClient.get("${baseUrl}/zones/${zone}/email/routing/catch_all") {
      applyAuthentication()
    }.body()

  suspend fun updateCatchAllRule(zone: String, rule: RoutingRule): ApiCallResult<RoutingRule> =
    httpClient.put("${baseUrl}/zones/${zone}/email/routing/rules/catch_all") {
      applyAuthentication()
      setBody(Json.encodeToString(RoutingRule.serializer(),
        rule.copy(tag = null, name = null, priority = null)))
    }.body()

  suspend fun addRoutingRule(zone: String, rule: RoutingRule): ApiCallResult<RoutingRule> =
    httpClient.post("${baseUrl}/zones/${zone}/email/routing/rules") {
      applyAuthentication()
      setBody(Json.encodeToString(RoutingRule.serializer(), rule))
    }.body()

  suspend fun deleteRoutingRule(zone: String, id: String): ApiCallResult<List<RoutingRule>> =
    httpClient.delete("${baseUrl}/zones/${zone}/email/routing/rules/${id}") {
      applyAuthentication()
    }.body()

  suspend fun listRoutingRules(zone: String): List<RoutingRule> {
    val rules = mutableListOf<RoutingRule>()
    var page = 1
    while (true) {
      val result = listRoutingRules(zone, page)
      if (!result.success) {
        throw RuntimeException("Failed to fetch routing rules.")
      }

      if (result.result!!.isEmpty()) {
        break
      }

      rules.addAll(result.result)
      page++
    }
    return rules
  }

  private fun HttpRequestBuilder.applyAuthentication() {
    if (auth.apiEmail != null) {
      header("X-Auth-Email", auth.apiEmail)
    }

    if (auth.apiKey != null) {
      header("X-Auth-Key", auth.apiKey)
    }

    if (auth.token != null) {
      header("Authorization", "Bearer ${auth.token}")
    }
  }

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
  data class ApiCallResult<T>(
    val result: T? = null,
    @SerialName("result_info")
    val resultInfo: ResultInfo? = null,
    val success: Boolean
  )

  @Serializable
  data class ResultInfo(
    val count: Int,
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    @SerialName("total_count")
    val totalCount: Int
  )

  override fun close() {
    httpClient.close()
  }
}

class CloudflareEmailAuth(
  val apiEmail: String? = null,
  val apiKey: String? = null,
  val token: String? = null
)
