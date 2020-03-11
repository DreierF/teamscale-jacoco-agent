package com.teamscale.jacoco.agent.upload.delay;

import com.teamscale.client.CommitDescriptor;
import com.teamscale.jacoco.agent.upload.IUploader;
import com.teamscale.jacoco.agent.util.DaemonThreadFactory;
import com.teamscale.jacoco.agent.util.LoggingUtils;
import com.teamscale.report.jacoco.CoverageFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Wraps an {@link IUploader} and in order to delay upload  until a {@link CommitDescriptor} is asynchronously made
 * available.
 */
public class DelayedCommitDescriptorUploader implements IUploader {

	private final Executor executor;
	private final Logger logger = LoggingUtils.getLogger(this);
	private final Function<CommitDescriptor, IUploader> wrappedUploaderFactory;
	private IUploader wrappedUploader = null;
	private Path cacheDir;

	public DelayedCommitDescriptorUploader(Function<CommitDescriptor, IUploader> wrappedUploaderFactory,
										   Path cacheDir) {
		this(wrappedUploaderFactory, cacheDir, Executors.newSingleThreadExecutor(
				new DaemonThreadFactory(DelayedCommitDescriptorUploader.class, "Delayed cache upload thread")));
	}

	/**
	 * Visible for testing. Allows tests to control the {@link Executor} to test the asynchronous functionality of this
	 * class.
	 */
	/*package*/ DelayedCommitDescriptorUploader(Function<CommitDescriptor, IUploader> wrappedUploaderFactory,
												Path cacheDir, Executor executor) {
		this.wrappedUploaderFactory = wrappedUploaderFactory;
		this.cacheDir = cacheDir;
		this.executor = executor;

		registerShutdownHook();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (wrappedUploader == null) {
				logger.error("The application was shut down before a commit could be found. The recorded coverage" +
								" is still cached in {} but will not be automatically processed. You configured the" +
								" agent to auto-detect the commit to which the recorded coverage should be uploaded to" +
								" Teamscale. In order to fix this problem, you need to provide a git.properties file" +
								" in all of the profiled Jar/War/Ear/... files. If you're using Gradle or" +
								" Maven, you can use a plugin to create a proper git.properties file for you, see" +
								" https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto-git-info",
						cacheDir.toAbsolutePath());
			}
		}));
	}

	@Override
	public synchronized void upload(CoverageFile file) {
		if (wrappedUploader == null) {
			logger.info("The commit to upload to has not yet been found. Caching coverage XML in {}",
					cacheDir.toAbsolutePath());
		} else {
			wrappedUploader.upload(file);
		}
	}

	@Override
	public String describe() {
		if (wrappedUploader != null) {
			return wrappedUploader.describe();
		}
		return "Temporary cache until commit is resolved: " + cacheDir.toAbsolutePath();
	}

	/**
	 * Sets the commit to upload the XMLs to and asynchronously triggers the upload of all cached XMLs. This method
	 * should only be called once.
	 */
	public synchronized void setCommitAndTriggerAsynchronousUpload(CommitDescriptor commit) {
		if (wrappedUploader == null) {
			wrappedUploader = wrappedUploaderFactory.apply(commit);
			logger.info("Commit to upload to has been found: {}. Uploading any cached XMLs now to {}", commit,
					wrappedUploader.describe());
			executor.execute(this::uploadCachedXmls);
		} else {
			logger.error("Tried to set upload commit multiple times (old uploader: {}, new commit: {})." +
					" This is a programming error. Please report a bug.", wrappedUploader.describe(), commit);
		}
	}

	private void uploadCachedXmls() {
		try {
			Stream<Path> xmlFilesStream = Files.list(cacheDir).filter(path -> {
				String fileName = path.getFileName().toString();
				return fileName.startsWith("jacoco-") && fileName.endsWith(".xml");
			});
			xmlFilesStream.forEach(path -> wrappedUploader.upload(new CoverageFile(path.toFile())));
			logger.debug("Finished upload of cached XMLs to {}", wrappedUploader.describe());
		} catch (IOException e) {
			logger.error("Failed to list cached coverage XML files in {}", cacheDir.toAbsolutePath(), e);
		}

	}
}
