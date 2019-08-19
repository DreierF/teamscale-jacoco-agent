package com.teamscale.report.testwise.closure;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.teamscale.client.FileSystemUtils;
import com.teamscale.client.StringUtils;
import com.teamscale.report.testwise.closure.model.ClosureCoverage;
import com.teamscale.report.testwise.model.TestwiseCoverage;
import com.teamscale.report.testwise.model.builder.FileCoverageBuilder;
import com.teamscale.report.testwise.model.builder.TestCoverageBuilder;
import com.teamscale.report.util.ILogger;
import okio.BufferedSource;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Creates {@link TestwiseCoverage} from Google closure coverage files. The given {@link ClosureCoverage} files must be
 * augmented with the {@link ClosureCoverage#uniformPath} field, which is not part of the Google closure coverage
 * specification.
 */
public class ClosureTestwiseCoverageGenerator {

	/** Directories and zip files that contain closure coverage files. */
	private Collection<File> closureCoverageDirectories;

	/** Include filter to apply to all js files contained in the original Closure coverage report. */
	private Predicate<String> locationIncludeFilter;

	/** The logger. */
	private final ILogger logger;

	/** JSON adapter for google closure coverage. */
	private final JsonAdapter<ClosureCoverage> closureCoverageAdapter = new Moshi.Builder().build()
			.adapter(ClosureCoverage.class);

	/**
	 * Create a new generator with a collection of report files.
	 *
	 * @param closureCoverageDirectories Root directory that contains the Google closure coverage reports.
	 * @param locationIncludeFilter      Filter for js files
	 */
	public ClosureTestwiseCoverageGenerator(Collection<File> closureCoverageDirectories,
											Predicate<String> locationIncludeFilter, ILogger logger) {
		this.closureCoverageDirectories = closureCoverageDirectories;
		this.locationIncludeFilter = locationIncludeFilter;
		this.logger = logger;
	}

	/**
	 * Converts all JSON files in {@link #closureCoverageDirectories} to {@link TestCoverageBuilder} and takes care of
	 * merging coverage distributed over multiple files.
	 */
	public TestwiseCoverage readTestCoverage() {
		TestwiseCoverage testwiseCoverage = new TestwiseCoverage();
		for (File closureCoverageDirectory : closureCoverageDirectories) {
			if (closureCoverageDirectory.isFile()) {
				testwiseCoverage.add(readTestCoverage(closureCoverageDirectory));
				continue;
			}
			List<File> coverageFiles = FileSystemUtils.listFilesRecursively(closureCoverageDirectory,
					file -> "json".equals(FileSystemUtils.getFileExtension(file)));
			for (File coverageReportFile : coverageFiles) {
				testwiseCoverage.add(readTestCoverage(coverageReportFile));
			}
		}
		return testwiseCoverage;
	}

	/**
	 * Reads the given JSON file and converts its content to {@link TestCoverageBuilder}. If this fails for some reason
	 * the method returns null.
	 */
	private TestCoverageBuilder readTestCoverage(File file) {
		try (BufferedSource source = Okio.buffer(Okio.source(file))) {
			ClosureCoverage coverage = closureCoverageAdapter.fromJson(JsonReader.of(source));
			return convertToTestCoverage(coverage);
		} catch (IOException e) {
			logger.error("Error while reading closure coverage from " + file.getAbsolutePath() + "!", e);
			return null;
		}
	}

	/** Converts the given {@link ClosureCoverage} to {@link TestCoverageBuilder}. */
	private TestCoverageBuilder convertToTestCoverage(ClosureCoverage coverage) {
		if (coverage == null || StringUtils.isEmpty(coverage.uniformPath)) {
			return null;
		}
		TestCoverageBuilder testCoverage = new TestCoverageBuilder(coverage.uniformPath);
		List<FileAndCoveredLines> executedLines = zip(coverage.fileNames, coverage.executedLines);
		for (FileAndCoveredLines fileNameAndExecutedLines : executedLines) {
			if (!locationIncludeFilter.test(fileNameAndExecutedLines.fileName)) {
				continue;
			}

			File coveredFile = new File(fileNameAndExecutedLines.fileName);
			List<Boolean> coveredLines = fileNameAndExecutedLines.coveredLines;
			String path = Optional.ofNullable(coveredFile.getParent()).orElse("");
			FileCoverageBuilder fileCoverage = new FileCoverageBuilder(path, coveredFile.getName());
			for (int i = 0; i < coveredLines.size(); i++) {
				if (coveredLines.get(i) != null && coveredLines.get(i)) {
					fileCoverage.addLine(i + 1);
				}
			}
			testCoverage.add(fileCoverage);
		}
		return testCoverage;
	}

	private static List<FileAndCoveredLines> zip(List<String> firstValues, List<List<Boolean>> secondValues) {
		List<FileAndCoveredLines> result = new ArrayList<>(firstValues.size());
		Iterator<String> firstIterator = firstValues.iterator();
		Iterator<List<Boolean>> secondIterator = secondValues.iterator();
		while (firstIterator.hasNext()) {
			result.add(new FileAndCoveredLines(firstIterator.next(), secondIterator.next()));
		}
		return result;
	}

	/** Wrapper that holds closure coverage for a single file. */
	private static class FileAndCoveredLines {

		private final String fileName;

		private final List<Boolean> coveredLines;

		/** Constructor. */
		public FileAndCoveredLines(String fileName, List<Boolean> coveredLines) {
			this.fileName = fileName;
			this.coveredLines = coveredLines;
		}
	}
}