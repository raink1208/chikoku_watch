package com.github.raink1208.deploytool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.raink1208.deploytool.config.Config
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class DeployCommand : CliktCommand(
    name = "deploy-tool"
) {
    private val logger = LoggerFactory.getLogger(DeployCommand::class.java)

    override fun help(context: Context): String {
        return "File monitoring and auto-upload tool for cloud storage"
    }

    private val configPath by option("-c", "--config", help = "Configuration file path")
        .default("config/config.properties")

    private val dryRun by option("-n", "--dry-run", help = "Dry run mode (no actual upload)")
        .flag(default = false)

    private val force by option("-f", "--force", help = "Force upload (skip change detection)")
        .flag(default = false)

    private val file by option("--file", help = "Process only specific file")

    private val verbose by option("-v", "--verbose", help = "Verbose logging")
        .flag(default = false)

    init {
        versionOption("1.0-SNAPSHOT")
    }

    override fun run() {
        try {
            logger.info("Loading configuration from: $configPath")
            val config = Config.load(configPath)

            logger.info("Loaded configuration from $configPath")

            val deployTool = DeployTool(config)
            val result = deployTool.run(dryRun, force, file)

            if (result.isSuccess()) {
                exitProcess(0)
            } else {
                exitProcess(1)
            }
        } catch (e: Exception) {
            logger.error("Error: ${e.message}", e)
            System.err.println("Error: ${e.message}")
            exitProcess(1)
        }
    }
}

fun main(args: Array<String>) = DeployCommand().main(args)
