package com.teamscale.jacoco.agent.store.upload.teamscale;

import com.teamscale.client.EReportFormat;
import com.teamscale.client.ITeamscaleService;
import com.teamscale.client.TeamscaleServer;
import com.teamscale.client.TeamscaleServiceGenerator;
import com.teamscale.jacoco.agent.store.IXmlStore;
import com.teamscale.jacoco.agent.store.TimestampedFileStore;
import com.teamscale.jacoco.agent.util.Benchmark;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.slf4j.Logger;

import java.io.IOException;

/** Uploads XML Coverage to a Teamscale instance. */
public class TeamscaleUploadStore implements IXmlStore {

	/** The logger. */
	private final Logger logger = LoggingUtils.getLogger(this);

	/** The store to write failed uploads to. */
	private final TimestampedFileStore failureStore;

	/** Teamscale server details. */
	private final TeamscaleServer teamscaleServer;

	/** Constructor. */
	public TeamscaleUploadStore(TimestampedFileStore failureStore, TeamscaleServer teamscaleServer) {
		this.failureStore = failureStore;
		this.teamscaleServer = teamscaleServer;
	}

	@Override
	public void store(String xml) {
		try (Benchmark benchmark = new Benchmark("Uploading report to Teamscale")) {
			if (!tryUploading(xml)) {
				logger.warn("Storing failed upload in {}", failureStore.getOutputDirectory());
				failureStore.store(xml);
			}
		}
	}

	/** Performs the upload and returns <code>true</code> if successful. */
	private boolean tryUploading(String xml) {
		logger.debug("Uploading JaCoCo artifact to {}", teamscaleServer);

		try {
			// Cannot be executed in the constructor as this causes issues in WildFly server (See #100)
			ITeamscaleService api = TeamscaleServiceGenerator.createService(
					ITeamscaleService.class,
					teamscaleServer.url,
					teamscaleServer.userName,
					teamscaleServer.userAccessToken
			);
			api.uploadReport(
					teamscaleServer.project,
					teamscaleServer.commit,
					teamscaleServer.partition,
					EReportFormat.JACOCO,
					teamscaleServer.message,
					RequestBody.create(MultipartBody.FORM, xml)
			);
			return true;
		} catch (IOException e) {
			logger.error("Failed to upload coverage to {}", teamscaleServer, e);
			return false;
		}
	}

	@Override
	public String describe() {
		return "Uploading to " + teamscaleServer + " (fallback in case of network errors to: " + failureStore.describe()
				+ ")";
	}
}
