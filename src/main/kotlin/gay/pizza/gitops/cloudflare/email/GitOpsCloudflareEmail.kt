package gay.pizza.gitops.cloudflare.email

import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.inputStream

class GitOpsCloudflareEmail : CliktCommand(
  help = "GitOps for Cloudflare Email",
  name = "gitops-cloudflare-email"
) {
  private val token by option(
    "--api-token", "-t",
    help = "Cloudflare API Token",
    envvar = "CLOUDFLARE_API_TOKEN"
  ).required()

  private val apply by option("--apply", help = "Apply the Configuration").flag()

  private val file by argument("config-path", help = "Path to Configuration")
    .path(mustExist = true, canBeFile = true, canBeDir = false)
    .default(Path(".").absolute())

  override fun run() {
    val config = file.inputStream().use { stream ->
      Yaml.default.decodeFromStream(Configuration.serializer(), stream)
    }

    CloudflareEmailClient(token).use { client ->
      for (domain in config.domains) {
        runBlocking {
          handleDomainConfig(client, config, domain)
        }
      }
    }
  }

  private suspend fun handleDomainConfig(
    client: CloudflareEmailClient,
    config: Configuration,
    domainConfig: DomainConfiguration
  ) {
    println("[${domainConfig.domain}] generating desired state")
    val desiredState = config.generateEmailState(domainConfig)
    println("[${domainConfig.domain}] fetching current state")
    val currentState = client.currentEmailState(domainConfig.account, domainConfig.zone)
    println("[${domainConfig.domain}] planning state changes")
    val plan = currentState.planForDesiredState(desiredState)
    println(plan.describe(dry = !apply)
      .trim()
      .lineSequence()
      .joinToString("\n") { "[${domainConfig.domain}] $it" })
    if (!apply) {
      return
    }
    client.applyConfigurationPlan(plan, domainConfig)
  }
}
