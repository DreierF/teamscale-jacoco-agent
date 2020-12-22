package com.teamscale.config.extension

import com.teamscale.TeamscalePlugin
import com.teamscale.config.Commit
import com.teamscale.config.ServerConfiguration
import com.teamscale.config.TopLevelReportConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.process.JavaForkOptions
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension


/**
 * Holds all user configuration for the teamscale plugin.
 */
open class TeamscalePluginExtension(val project: Project) {

    val server = ServerConfiguration()

    /** Configures the Teamscale server. */
    fun server(action: Action<in ServerConfiguration>) {
        action.execute(server)
    }

    val commit = Commit()

    /** Configures the code commit. */
    fun commit(action: Action<in Commit>) {
        action.execute(commit)
    }

    var baseline: Long? = null

    /** Configures the baseline. */
    fun baseline(action: Action<in Long?>) {
        action.execute(baseline)
    }

    val report = TopLevelReportConfiguration(project)

    /** Configures the reports to be uploaded. */
    fun report(action: Action<in TopLevelReportConfiguration>) {
        action.execute(report)
    }

    fun <T> applyTo(task: T): TeamscaleTestImpactedTaskExtension where T : Task, T : JavaForkOptions {
        val jacocoTaskExtension: JacocoTaskExtension = task.extensions.getByType(JacocoTaskExtension::class.java)
        jacocoTaskExtension.excludes?.addAll(DEFAULT_EXCLUDES)

        val extension =
            task.extensions.create(
                TeamscalePlugin.teamscaleExtensionName,
                TeamscaleTestImpactedTaskExtension::class.java,
                project,
                this,
                jacocoTaskExtension
            )
        extension.agent.setDestination(task.project.provider {
            project.file("${project.buildDir}/jacoco/${project.name}-${task.name}")
        })
        return extension
    }

    companion object {
        private val DEFAULT_EXCLUDES = listOf(
            "org.junit.*",
            "org.gradle.*",
            "com.esotericsoftware.*",
            "com.teamscale.jacoco.agent.*",
            "com.teamscale.test_impacted.*",
            "com.teamscale.report.*",
            "com.teamscale.client.*",
            "org.jacoco.core.*",
            "shadow.*",
            "okhttp3.*",
            "okio.*",
            "retrofit2.*",
            "*.MockitoMock.*",
            "*.FastClassByGuice.*",
            "*.ConstructorAccess"
        )
    }
}
