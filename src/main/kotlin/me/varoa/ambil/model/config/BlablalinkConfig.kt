package me.varoa.ambil.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("blablalink")
data class BlablalinkConfig(
    val username: String,
    val password: String,
) : JobConfig
