package me.varoa.ambil.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// No credentials: login is manual in a persistent browser profile.
@Serializable
@SerialName("blablalink")
data object BlablalinkConfig : JobConfig
