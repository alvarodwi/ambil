package me.varoa.ambil.service.job

import me.varoa.ambil.model.data.JobResult
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.service.DiscordClient

class DiscordJob(
    private val client: DiscordClient,
) : JobTask {
    override val name = "discord"

    override fun execute(): JobResult {
        val results = client.clickAllButtons()

        val failed = results.any { it.isFailure }

        return if (failed) {
            JobResult(false, "Some interactions failed")
        } else {
            JobResult(true, "All interactions succeeded")
        }
    }
}
