package com.teamscale

import com.teamscale.client.TeamscaleClient
import com.teamscale.config.TeamscalePluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.net.ConnectException
import java.net.SocketTimeoutException

/** Handles report uploads to Teamscale. */
open class TeamscaleUploadTask : DefaultTask() {

    /** The global teamscale configuration. */
    @Internal
    lateinit var extension: TeamscalePluginExtension

    /** The Teamscale server configuration. */
    @get:Input
    val server
        get() = extension.server

    /** The commit for which the reports should be uploaded. */
    @get:Input
    val commitDescriptor
        get() = extension.commit.getOrResolveCommitDescriptor(project)

    /** The list of reports to be uploaded. */
    @Input
    val reports = mutableSetOf<Report>()

    @Input
    var ignoreFailures: Boolean = false

    init {
        group = "Teamscale"
        description = "Uploads reports to Teamscale"
    }

    /** Executes the report upload. */
    @TaskAction
    fun action() {
        if (reports.isEmpty()) {
            logger.info("Skipping upload. No reports to upload.")
            return
        }

        server.validate()

        try {
            logger.info("Uploading to $server at $commitDescriptor...")
            uploadReports()
        } catch (e: Exception) {
            if (ignoreFailures) {
                logger.warn("Ignoring failure during upload:")
                logger.warn(e.message, e)
            } else {
                throw e
            }
        }
    }

    private fun uploadReports() {
        // We want to upload e.g. all JUnit test reports that go to the same partition
        // as one commit so we group them before uploading them
        for ((key, reports) in reports.groupBy { Triple(it.format, it.partition, it.message) }) {
            val (format, partition, message) = key
            val reportFiles = reports.map { it.reportFile }.distinct()
            logger.info("Uploading ${reportFiles.size} ${format.name} report(s) to partition $partition...")
            if (reportFiles.isEmpty()) {
                logger.info("Skipped empty upload!")
                continue
            }

            try {
                retry(3) {
                    val client =
                        TeamscaleClient(server.url, server.userName, server.userAccessToken, server.project)
                    client.uploadReports(
                        format, reportFiles, commitDescriptor, partition, "$message ($partition)"
                    )
                }
            } catch (e: ConnectException) {
                throw GradleException("Upload failed (${e.message})", e)
            } catch (e: SocketTimeoutException) {
                throw GradleException("Upload failed (${e.message})", e)
            }
        }
    }
}

/**
 * Retries the given block numOfRetries-times catching any thrown exceptions.
 * If none of the retries succeeded the latest catched exception is rethrown.
 */
fun <T> retry(numOfRetries: Int, block: () -> T): T {
    var throwable: Throwable? = null
    (1..numOfRetries).forEach { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            throwable = e
            println("Failed attempt $attempt / $numOfRetries")
        }
    }
    throw throwable!!
}
