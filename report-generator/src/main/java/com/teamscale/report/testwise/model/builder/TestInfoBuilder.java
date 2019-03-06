package com.teamscale.report.testwise.model.builder;

import com.teamscale.client.TestDetails;
import com.teamscale.report.testwise.model.TestExecution;
import com.teamscale.report.testwise.model.TestInfo;

/** Generic container of all information about a specific test including details, execution info and coverage. */
public class TestInfoBuilder {

	/** The uniformPath of the test (see TEST_IMPACT_ANALYSIS_DOC.md for more information). */
	private final String uniformPath;

	/** The test details of this test. */
	private TestDetails details;

	/** Information about the execution result of this test. */
	private TestExecution execution;

	/** Coverage generated by this test. */
	private TestCoverageBuilder coverage;

	/** Constructor. */
	/* package */ TestInfoBuilder(String uniformPath) {
		this.uniformPath = uniformPath;
	}

	/** @see #uniformPath */
	public String getUniformPath() {
		return uniformPath;
	}

	/** Returns true if there is no coverage for the test yet. */
	public boolean isEmpty() {
		return coverage.isEmpty();
	}

	/** @see #details */
	public void setDetails(TestDetails details) {
		this.details = details;
	}

	/** @see #execution */
	public void setExecution(TestExecution execution) {
		this.execution = execution;
	}

	/** @see #coverage */
	public void setCoverage(TestCoverageBuilder coverage) {
		this.coverage = coverage;
	}

	/** Builds a {@link TestInfo} object of the data in this container. */
	public TestInfo build() {
		TestInfo testInfo;
		if (execution != null) {
			testInfo = new TestInfo(uniformPath, details.sourcePath, details.content,
					execution.getDurationSeconds(),
					execution.getResult(), execution.getMessage());
		} else {
			testInfo = new TestInfo(uniformPath, details.sourcePath, details.content, null, null, null);
		}
		if (coverage != null) {
			testInfo.paths.addAll(coverage.getPaths());
		}
		return testInfo;
	}
}