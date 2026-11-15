package me.varoa.ambil.config

import kotlinx.serialization.json.Json
import me.varoa.ambil.model.config.AppConfig
import java.io.File

object ConfigLoader {
    val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        }

    fun load(): AppConfig {
        val configFile = resolveConfigFile()
        require(configFile.exists()) {
            "config.json not found at: ${configFile.absolutePath}\n" +
                "Copy config.json.example to config.json and fill in your values."
        }
        return try {
            json.decodeFromString<AppConfig>(
                configFile.readText(),
            )
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to parse config.json: ${e.message}",
                e,
            )
        }
    }

    /**
     * Looks for config.json next to the running JAR first,
     * then in the working directory.
     */
    private fun resolveConfigFile(): File {
        val jarLocation =
            ConfigLoader::class.java
                .protectionDomain
                ?.codeSource
                ?.location
                ?.toURI()
        if (jarLocation != null) {
            val siblingConfig =
                File(jarLocation)
                    .parentFile
                    ?.resolve("config.json")

            if (siblingConfig?.exists() == true) {
                return siblingConfig
            }
        }
        return File("config.json")
    }
}
