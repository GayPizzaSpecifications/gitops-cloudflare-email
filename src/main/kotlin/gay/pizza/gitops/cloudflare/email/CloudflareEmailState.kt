package gay.pizza.gitops.cloudflare.email

data class CloudflareEmailState(
  val routingRules: List<RoutingRule>,
  val destinationAddresses: List<DestinationAddress>
)

fun CloudflareEmailState.planForDesiredState(
  desiredState: CloudflareEmailState
): ConfigurationChangePlan = ConfigurationChangePlan(
  existingState = this,
  desiredState = desiredState,
  addRoutingRules = desiredState.routingRules.filter { generated ->
    !routingRules.any { existing -> generated.simpleEquals(existing) } },
  removeRoutingRules = routingRules.filter { existing ->
    !desiredState.routingRules.any { generated -> existing.simpleEquals(generated) } },
  addDestinationAddresses = desiredState.destinationAddresses.filter { required ->
    !destinationAddresses.any { existing ->
      required.email == existing.email
    }
  }
)
