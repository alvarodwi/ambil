package me.varoa.ambil.service.job

import me.varoa.ambil.model.data.JobResult
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.service.DiscordClient

class DiscordJob(
    private val client: DiscordClient,
) : JobTask {
    override val name = "discord"

    override fun execute(): JobResult = discordJobResult(client.clickAllButtons())
}

/** Builds the outcome message from (label, result) pairs, one line per failed interaction. */
fun discordJobResult(results: List<Pair<String, Result<Unit>>>): JobResult {
    val claimed = results.filter { it.second.isSuccess }.map { it.first }
    val failed =
        results.mapNotNull { (label, result) ->
            result.exceptionOrNull()?.let { "Failed: $label: ${it.message ?: it::class.simpleName}" }
        }
    val claimedLine = "Claimed: ${claimed.joinToString(", ")}"

    return if (failed.isEmpty()) {
        JobResult(true, claimedLine)
    } else {
        JobResult(false, (failed + listOfNotNull(claimedLine.takeIf { claimed.isNotEmpty() })).joinToString("\n"))
    }
}
