package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
  val domains: List<DomainConfiguration>,
  val groups: Map<String, List<String>> = emptyMap(),
  val forwards: Map<String, String> = emptyMap(),
  val workers: Map<String, String> = emptyMap(),
  val drop: List<String> = emptyList(),
  @SerialName("catch-all")
  val catchAll: String
)

@Serializable
data class DomainConfiguration(
  val domain: String,
  val account: String,
  val zone: String
)

fun Configuration.collectDestinationAddresses(): List<DestinationAddress> = listOf(
  listOf(catchAll),
  forwards.map { forward -> forward.value },
  groups.map { group -> group.value }.flatten()
).flatten().toSet().toList().map { email -> DestinationAddress(email = email) }

fun DomainConfiguration.createDomainEmail(emailName: String): String =
  if (emailName.endsWith("@${domain}")) {
    emailName
  } else {
    "${emailName}@${domain}"
  }

fun Configuration.generateEmailState(domainConfig: DomainConfiguration): CloudflareEmailState = CloudflareEmailState(
  routingRules = generateRoutingRules(domainConfig),
  destinationAddresses = collectDestinationAddresses()
)
