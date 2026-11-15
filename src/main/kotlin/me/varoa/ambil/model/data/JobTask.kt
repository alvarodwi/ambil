package me.varoa.ambil.model.data

interface JobTask {
    val name: String

    fun execute(): JobResult
}
