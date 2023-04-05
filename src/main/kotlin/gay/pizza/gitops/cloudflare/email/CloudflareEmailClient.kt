package gay.pizza.gitops.cloudflare.email

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class CloudflareEmailClient(private val token: String) : AutoCloseable {
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

  private suspend fun listDestinationAddresses(account: String, page: Int): ApiCallResult<List<DestinationAddress>> =
    httpClient.get("${baseUrl}/accounts/${account}/email/routing/addresses?page=${page}") {
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

  suspend fun addDestinationAddress(account: String, address: DestinationAddress): ApiCallResult<DestinationAddress> =
    httpClient.post("${baseUrl}/accounts/${account}/email/routing/addresses") {
      applyAuthentication()
      setBody(Json.encodeToString(DestinationAddress.serializer(), address))
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

  suspend fun listDestinationAddresses(zone: String): List<DestinationAddress> {
    val rules = mutableListOf<DestinationAddress>()
    var page = 1
    while (true) {
      val result = listDestinationAddresses(zone, page)
      if (!result.success) {
        throw RuntimeException("Failed to fetch destination addresses.")
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
    header("Authorization", "Bearer $token")
  }

  override fun close() {
    httpClient.close()
  }
}
