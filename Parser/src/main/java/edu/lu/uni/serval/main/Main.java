package edu.lu.uni.serval.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.diff.parser.RunnableParser;
import edu.lu.uni.serval.diff.parser.violations.FixedViolationHunkParser;
import edu.lu.uni.serval.diff.parser.violations.ViolationInstance;
import edu.lu.uni.serval.main.violations.AkkaParser;
import edu.lu.uni.serval.main.violations.AkkaParser2;
import edu.lu.uni.serval.main.violations.MessageFile;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.violation.info.parser.ParsingProcess;

/**
 * Parse diffs of fixed violations with GumTree.
 * 
 * @author kui.liu
 *
 */
public class Main {

	public static void main(String[] args) {
		// Prepare Java files and positions of violations.
		new ParsingProcess().parse();
		
		// Parse fixed violations.
		new Main().parseFixedViolations();
		
		// Parser violation code of unfixed violations.
		AkkaParser.main(args);
		// Parser violation code of fixed violations.
		AkkaParser2.main(args);
	}
	
	public void parseFixedViolations() {
		// output path
		final String editScriptsFilePath = Configuration.EDITSCRIPTS_FILE_PATH;
		final String patchesSourceCodeFilePath = Configuration.PATCH_SOURCECODE_FILE_PATH;
		final String buggyTokensFilePath = Configuration.BUGGY_CODE_TOKEN_FILE_PATH;
		final String editScriptSizesFilePath = Configuration.EDITSCRIPT_SIZES_FILE_PATH;
		final String alarmTypesFilePath = Configuration.ALARM_TYPES_FILE_PATH;
		FileHelper.deleteDirectory(editScriptsFilePath);
		FileHelper.deleteDirectory(patchesSourceCodeFilePath);
		FileHelper.deleteDirectory(buggyTokensFilePath);
		FileHelper.deleteDirectory(editScriptSizesFilePath);
		FileHelper.deleteDirectory(alarmTypesFilePath);

		final List<MessageFile> files = getMessageFiles(Configuration.GUM_TREE_INPUT);

		StringBuilder editScripts = new StringBuilder();
		StringBuilder patchesSourceCode = new StringBuilder();
		StringBuilder sizes = new StringBuilder();
		StringBuilder tokens = new StringBuilder();
		StringBuilder alarmTypes = new StringBuilder();
		StringBuilder testingInfo = new StringBuilder();

		int counter = 0;

		int testViolations = 0;
		int nullGumTreeResults = 0;
		int noSourceCodeChanges = 0;
		int noStatementChanges = 0;
		int nullDiffEntry = 0;
		int nullMappingGumTreeResults = 0;
		int pureDeletion = 0;
		int largeHunk = 0;
		int nullSourceCode = 0;
		int testInfos = 0;
		int timeouts = 0;
		int numViolations = 0;
		StringBuilder builder = new StringBuilder();

		for (MessageFile msgFile : files) {
			File revFile = msgFile.getRevFile();
			File prevFile = msgFile.getPrevFile();
			File diffentryFile = msgFile.getDiffEntryFile();
			File positionFile = msgFile.getPositionFile();
			if (revFile.getName().toLowerCase().contains("test#") || revFile.getName().toLowerCase().contains("tests#")) {
				testViolations += countViolations(positionFile, "#TestViolation:");
				continue;
			}
			FixedViolationHunkParser parser = new FixedViolationHunkParser(positionFile);
			// parser.setUselessViolations(uselessViolations);

			final ExecutorService executor = Executors.newSingleThreadExecutor();
			// schedule the work
			final Future<?> future = executor.submit(new RunnableParser(prevFile, revFile, diffentryFile, parser));
			try {
				// wait for task to complete
				future.get(Configuration.SECONDS_TO_WAIT, TimeUnit.SECONDS);

				nullDiffEntry += parser.nullMatchedDiffEntry;
				nullMappingGumTreeResults += parser.nullMappingGumTreeResult;
				pureDeletion += parser.pureDeletions;
				largeHunk += parser.largeHunk;
				nullSourceCode += parser.nullSourceCode;
				testInfos += parser.testInfos;
				testingInfo.append(parser.testingInfo);
				builder.append(parser.unfixedViolations);
				numViolations += parser.violations;

				String editScript = parser.getAstEditScripts();
				if ("".equals(editScript)) {
					if (parser.resultType == 1) {
						nullGumTreeResults += countViolations(positionFile, "");
					} else if (parser.resultType == 2) {
						noSourceCodeChanges += countViolations(positionFile, "");
					} else if (parser.resultType == 3) {
						noStatementChanges += countViolations(positionFile, "");
//					} else if (parser.resultType == 4) {
					}
				} else {
					editScripts.append(editScript);
					patchesSourceCode.append(parser.getPatchesSourceCode());
					sizes.append(parser.getSizes());
					alarmTypes.append(parser.getAlarmTypes());
					tokens.append(parser.getTokensOfSourceCode());

					counter++;
					if (counter % 5000 == 0) {
						FileHelper.outputToFile(editScriptsFilePath + "edistScripts.list", editScripts, true);
						FileHelper.outputToFile(patchesSourceCodeFilePath + "patches.list", patchesSourceCode, true);
						FileHelper.outputToFile(editScriptSizesFilePath + "sizes.list", sizes, true);
						FileHelper.outputToFile(buggyTokensFilePath + "tokens.list", tokens, true);
						editScripts.setLength(0);
						patchesSourceCode.setLength(0);
						sizes.setLength(0);
						tokens.setLength(0);
						FileHelper.outputToFile(alarmTypesFilePath + "alarmTypes.list", alarmTypes, true);
						alarmTypes.setLength(0);
						FileHelper.outputToFile("OUTPUT/testingInfo.list", testingInfo, true);
						testingInfo.setLength(0);
					}
				}
			} catch (TimeoutException e) {
				// err.println("task timed out");
				future.cancel(true);
				timeouts += countViolations(positionFile, "#Timeout:");
				// System.err.println("#Timeout: " + revFile.getName());
			} catch (InterruptedException e) {
				timeouts += countViolations(positionFile, "#TimeInterrupted:");
				// err.println("task interrupted");
				// System.err.println("#TimeInterrupted: " + revFile.getName());
			} catch (ExecutionException e) {
				timeouts += countViolations(positionFile, "#TimeAborted:");
				// err.println("task aborted");
				// System.err.println("#TimeAborted: " + revFile.getName());
			} finally {
				executor.shutdownNow();
			}
		}

		if (sizes.length() > 0) {
			FileHelper.outputToFile(editScriptsFilePath + "edistScripts.list", editScripts, true);
			FileHelper.outputToFile(patchesSourceCodeFilePath + "patches.list", patchesSourceCode, true);
			FileHelper.outputToFile(editScriptSizesFilePath + "sizes.list", sizes, true);
			FileHelper.outputToFile(buggyTokensFilePath + "tokens.list", tokens, true);
			editScripts.setLength(0);
			patchesSourceCode.setLength(0);
			sizes.setLength(0);
			tokens.setLength(0);

			FileHelper.outputToFile(alarmTypesFilePath + "alarmTypes.list", alarmTypes, true);
			alarmTypes.setLength(0);
			FileHelper.outputToFile("OUTPUT/testingInfo.list", testingInfo, true);
			testingInfo.setLength(0);
		}
		String statistic = "TestViolations: " + testViolations + "\nNullGumTreeResults: " + nullGumTreeResults
				+ "\nNoSourceCodeChanges: " + noSourceCodeChanges + "\nNoStatementChanges: " + noStatementChanges
				+ "\nNullDiffEntry: " + nullDiffEntry + "\nNullMatchedGumTreeResults: " + nullMappingGumTreeResults
				+ "\nPureDeletion: " + pureDeletion + "\nLargeHunk: " + largeHunk + "\nNullSourceCode: "
				+ nullSourceCode + "\nTestingInfo: " + testInfos + "\nTimeout: " + timeouts + "\nViolations: "
				+ numViolations;
		FileHelper.outputToFile("../data/FixedViolations/statistic.list", statistic, false);
		FileHelper.outputToFile("../data/FixedViolations/UnfixedV.list", builder, false);
	}
	
	private int countViolations(File positionFile, String type) {//, List<Violation> uselessViolations) {
		int counter = 0;
		String content = FileHelper.readFile(positionFile);
		BufferedReader reader = new BufferedReader(new StringReader(content));
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String[] elements = line.split(":");
				ViolationInstance v = new ViolationInstance(Integer.parseInt(elements[1]), Integer.parseInt(elements[2]), elements[0]);
				String fileName = positionFile.getName().replace(".txt", ".java");
				v.setFileName(fileName);
				counter ++;
				if (!"".equals(type)) {
//					System.err.println(type + fileName + ":" + elements[1] + ":" + elements[2] + ":" + elements[0]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return counter;
	}
	
	/**
	 * Get violation-related files.
	 * 
	 * @param gumTreeInput
	 * @return
	 */
	private List<MessageFile> getMessageFiles(String gumTreeInput) {
		String inputPath = gumTreeInput; // prevFiles  revFiles diffentryFile positionsFile
		File revFilesPath = new File(inputPath + "revFiles/");
		File[] revFiles = revFilesPath.listFiles();   // project folders
		List<MessageFile> msgFiles = new ArrayList<>();
		
		for (File revFile : revFiles) {
			if (revFile.getName().endsWith(".java")) {
				String fileName = revFile.getName();
				File prevFile = new File(gumTreeInput + "prevFiles/prev_" + fileName);// previous file
				fileName = fileName.replace(".java", ".txt");
				File diffentryFile = new File(gumTreeInput + "diffentries/" + fileName); // DiffEntry file
				File positionFile = new File(gumTreeInput + "positions/" + fileName); // position file
				MessageFile msgFile = new MessageFile(revFile, prevFile, diffentryFile);
				msgFile.setPositionFile(positionFile);
				msgFiles.add(msgFile);
			}
		}
		
		return msgFiles;
	}

}
