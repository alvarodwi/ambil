package me.varoa.ambil.service.job

import me.varoa.ambil.model.data.JobResult
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.service.BlablalinkClient

class BlablalinkJob(
    private val client: BlablalinkClient,
) : JobTask {
    override val name = "blablalink"

    override fun execute(): JobResult {
        val result = client.run()

        return if (result.success) {
            JobResult(true, "${result.signIn}. ${result.likes}")
        } else {
            JobResult(false, result.signIn)
        }
    }
}
