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
)

fun Configuration.collectDestinationAddresses(): List<DestinationAddress> = listOf(
  listOf(catchAll),
  forwards.map { forward -> forward.value },
  groups.map { group -> group.value }.flatten()
).flatten().toSet().toList().map { email -> DestinationAddress(email = email) }

fun Configuration.createDomainEmail(emailName: String): String = "${emailName}@${domain}"
