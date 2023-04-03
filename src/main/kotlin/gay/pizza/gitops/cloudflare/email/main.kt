package gay.pizza.gitops.cloudflare.email

import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.inputStream

class GitopsCloudflareEmail : CliktCommand(
  help = "GitOps for Cloudflare Email",
  name = "gitops-cloudflare-email"
) {
  private val token by option(
    "--api-token", "-k",
    help = "Cloudflare API Token",
    envvar = "CLOUDFLARE_API_TOKEN"
  ).required()

  private val apply by option("--apply", help = "Apply the Configuration").flag()

  private val file by argument("config-path", help = "Path to Configuration")
    .path(mustExist = true, canBeFile = true, canBeDir = false)
    .default(Path(".").absolute())

  override fun run() {
    val client = CloudflareEmailClient(CloudflareEmailAuth(token = token))
    val config = Yaml.default.decodeFromStream(Configuration.serializer(), file.inputStream())
    val applier = ConfigurationApplier(client, config)
    runBlocking { applier.apply(!apply) }
  }
}

fun main(args: Array<String>) = GitopsCloudflareEmail().main(args)
