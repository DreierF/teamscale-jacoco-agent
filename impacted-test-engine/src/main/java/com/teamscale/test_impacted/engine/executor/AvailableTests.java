package com.teamscale.test_impacted.engine.executor;

import com.teamscale.client.ClusteredTestDetails;
import com.teamscale.client.PrioritizableTest;
import com.teamscale.client.StringUtils;
import com.teamscale.client.TestDetails;
import com.teamscale.test_impacted.engine.ImpactedTestEngine;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds a list of test details that can currently be executed. Provides the ability to translate uniform paths returned
 * by the Teamscale server to unique IDs used in JUnit Platform.
 */
public class AvailableTests {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpactedTestEngine.class);

	/**
	 * A mapping from the tests uniform path (Teamscale internal representation) to unique id (JUnit internal
	 * representation).
	 */
	private Map<String, UniqueId> uniformPathToUniqueIdMapping = new HashMap<>();

	/** List of all test details. */
	private List<ClusteredTestDetails> testList = new ArrayList<>();

	/** Adds a new {@link TestDetails} object and the according uniqueId. */
	public void add(UniqueId uniqueId, ClusteredTestDetails details) {
		uniformPathToUniqueIdMapping.put(details.uniformPath, uniqueId);
		testList.add(details);
	}

	/** Returns the list of available tests. */
	public List<ClusteredTestDetails> getTestList() {
		return testList;
	}

	/**
	 * Converts the {@link PrioritizableTest}s which are match the {@link UniqueId}s returned by the {@link TestEngine}s
	 * used by the {@link ImpactedTestEngine}.
	 */
	public Set<UniqueId> convertToUniqueIds(List<PrioritizableTest> impactedTests) {
		Set<UniqueId> list = new HashSet<>();
		for (PrioritizableTest impactedTest : impactedTests) {
			LOGGER.info(() -> impactedTest.uniformPath + " " + impactedTest.selectionReason);

			UniqueId testUniqueId = uniformPathToUniqueIdMapping.get(impactedTest.uniformPath);
			if (testUniqueId == null) {
				LOGGER.error(() -> "Retrieved invalid test '" + impactedTest.uniformPath + "' from Teamscale server!");
				LOGGER.error(() -> "The following seem related:");
				uniformPathToUniqueIdMapping.keySet().stream().sorted(Comparator
						.comparing(testPath -> StringUtils.editDistance(impactedTest.uniformPath, testPath))).limit(5)
						.forEach(testAlternative -> LOGGER.error(() -> " - " + testAlternative));

				LOGGER.error(() -> "Falling back to execute all...");
				return new HashSet<>(uniformPathToUniqueIdMapping.values());
			}
			list.add(testUniqueId);
		}
		return list;
	}
}
