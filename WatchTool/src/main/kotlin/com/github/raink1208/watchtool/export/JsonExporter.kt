package com.github.raink1208.watchtool.export

import com.github.raink1208.watchtool.models.StreamReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsonExporter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(report: StreamReport, outputPath: String) {
        val jsonString = json.encodeToString(report)
        File(outputPath).apply {
            parentFile?.mkdirs()
            writeText(jsonString)
        }
    }
}

