/*-------------------------------------------------------------------------+
|                                                                          |
| Copyright (c) 2009-2018 CQSE GmbH                                        |
|                                                                          |
+-------------------------------------------------------------------------*/
package eu.cqse.teamscale.jacoco.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.agent.rt.internal_c13123e.PreMain;

import eu.cqse.teamscale.jacoco.agent.AgentOptions.AgentOptionParseException;
import eu.cqse.teamscale.jacoco.agent.dump.Dump;
import eu.cqse.teamscale.jacoco.agent.dump.JacocoRuntimeController;
import eu.cqse.teamscale.jacoco.agent.dump.JacocoRuntimeController.DumpException;
import eu.cqse.teamscale.jacoco.agent.report.XmlReportGenerator;
import eu.cqse.teamscale.jacoco.agent.store.IXmlStore;
import eu.cqse.teamscale.jacoco.agent.util.LoggingUtils;
import eu.cqse.teamscale.jacoco.agent.util.Timer;

/**
 * A wrapper around the JaCoCo Java agent that automatically triggers a dump and
 * XML conversion based on a time interval.
 */
public class Agent {

	/**
	 * Entry point for the agent, called by the JVM.
	 */
	public static void premain(String options, Instrumentation instrumentation) throws Exception {
		AgentOptions agentOptions;
		try {
			agentOptions = new AgentOptions(options);
		} catch (AgentOptionParseException e) {
			LoggingUtils.initializeDefaultLogging();
			LogManager.getLogger(Agent.class).fatal("Failed to parse agent options: " + e.getMessage(), e);
			System.err.println("Failed to parse agent options: " + e.getMessage());
			throw e;
		}

		LoggingUtils.initializeLogging(agentOptions.getLoggingConfig());

		LogManager.getLogger(Agent.class).info("Starting JaCoCo's agent");
		PreMain.premain(agentOptions.createJacocoAgentOptions(), instrumentation);

		Agent agent = new Agent(agentOptions);
		agent.startDumpLoop();
		agent.registerShutdownHook();
	}

	/** The logger. */
	private final Logger logger = LogManager.getLogger(this);

	/** Regular dump task. */
	private final Timer timer;

	/** Controls the JaCoCo runtime. */
	private final JacocoRuntimeController controller;

	/** Converts binary data to XML. */
	private final XmlReportGenerator generator;

	/** Stores the XML files. */
	private final IXmlStore store;

	/** Constructor. */
	public Agent(AgentOptions options) {
		controller = new JacocoRuntimeController();

		generator = new XmlReportGenerator(options.getClassDirectoriesOrZips(), options.getLocationIncludeFilter(),
				options.shouldIgnoreDuplicateClassFiles());
		store = options.createStore();

		timer = new Timer(this::dump, Duration.ofMinutes(options.getDumpIntervalInMinutes()));

		logger.info("Starting JaCoCo agent with options: {}", options.getOriginalOptionsString());
		logger.info("Dumping every {} minutes. Storage method: {}", options.getDumpIntervalInMinutes(),
				store.describe());
	}

	/**
	 * Dumps the current execution data, converts it and writes it to the
	 * {@link #store}. Logs any errors, never throws an exception.
	 */
	private void dump() {
		logger.debug("Starting dump");

		try {
			dumpUnsafe();
		} catch (Throwable t) {
			// we want to catch anything in order to avoid killing the regular job
			logger.error("Dump job failed with an exception. Retrying later", t);
		}
	}

	/**
	 * Performs the actual dump but does not handle e.g. OutOfMemoryErrors.
	 */
	private void dumpUnsafe() {
		Dump dump;
		try {
			dump = controller.dumpAndReset();
		} catch (DumpException e) {
			logger.error("Dumping failed, retrying later", e);
			return;
		}

		String xml;
		try {
			xml = generator.convert(dump);
		} catch (IOException e) {
			logger.error("Converting binary dump to XML failed", e);
			return;
		}

		store.store(xml);
	}

	/**
	 * Registers a shutdown hook that stops the timer and dumps coverage a final
	 * time.
	 */
	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			dump();
			timer.stop();
			logger.info("CQSE JaCoCo agent successfully shut down.");

			// manually shut down the logging system since we prevented automatic shutdown
			LogManager.shutdown();
		}));
	}

	/**
	 * Starts the regular {@link #dump()}.
	 */
	private void startDumpLoop() {
		timer.start();
	}

}