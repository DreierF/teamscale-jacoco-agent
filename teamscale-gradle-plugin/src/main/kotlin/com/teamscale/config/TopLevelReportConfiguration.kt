package com.teamscale.config

import com.teamscale.TestImpacted
import com.teamscale.config.extension.TeamscaleJacocoReportTaskExtension
import com.teamscale.config.extension.TeamscaleTestImpactedTaskExtension
import com.teamscale.config.extension.TeamscaleTestTaskExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Provides some utility methods that users of the plugin can use
 * in their scripts to set properties for all tasks of a certain type.
 */
class TopLevelReportConfiguration(val project: Project) {

    /** Configures settings for all testwise coverage reports. */
    fun testwiseCoverage(action: Action<in TestwiseCoverageConfiguration>) {
        project.tasks.withType(TestImpacted::class.java) { testImpacted ->
            val testImpactedExtension =
                testImpacted.extensions.getByType(TeamscaleTestImpactedTaskExtension::class.java)
            testImpactedExtension.testwiseCoverage(action)
        }
    }

    /**
     * Configures jacoco report settings for all jacoco report tasks
     * and makes sure xml report generation is enabled.
     */
    fun jacoco(action: Action<in JacocoReportConfiguration>) {
        project.tasks.withType(JacocoReport::class.java) { jacocoReport ->
            val testExtension = jacocoReport.extensions.getByType(TeamscaleJacocoReportTaskExtension::class.java)
            testExtension.jacoco(action)
        }
    }

    /**
     * Configures junit report settings for all test tasks
     * and makes sure xml report generation is enabled.
     */
    fun junit(action: Action<in JUnitReportConfiguration>) {
        project.tasks.withType(Test::class.java) { test ->
            if (test is TestImpacted) {
                return@withType
            }
            val testExtension = test.extensions.getByType(TeamscaleTestTaskExtension::class.java)
            testExtension.junit(action)
        }
    }
}