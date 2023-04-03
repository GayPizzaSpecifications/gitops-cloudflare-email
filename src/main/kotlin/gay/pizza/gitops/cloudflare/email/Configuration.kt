package gay.pizza.gitops.cloudflare.email

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
  val domain: String,
  val zone: String,
  val forwards: Map<String, String>,
  @SerialName("catch-all")
  val catchAll: String
)
