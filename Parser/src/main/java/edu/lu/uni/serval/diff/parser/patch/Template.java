package edu.lu.uni.serval.diff.parser.patch;

import static java.lang.System.err;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.diff.parser.RunnableParser;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Template of patch diffs parser.
 * @author kui.liu
 */
public class Template {

	public static void main(String[] args) {
		String testDataPath = "../GumTreeInput/"; //DiffEntries  prevFiles  revFiles
		File inputFileDirector = new File(testDataPath);
		File[] files = inputFileDirector.listFiles();   // project folders
		
		String outputPath = "../GumTreeOutput/";
		
		StringBuilder astEditScripts = new StringBuilder();
//		StringBuilder originalTrees = new StringBuilder();
//		StringBuilder buggyTrees = new StringBuilder();
//		StringBuilder actionSets = new StringBuilder();
		StringBuilder tokens = new StringBuilder();
		StringBuilder sizes = new StringBuilder();
		StringBuilder patches = new StringBuilder();
		
		for (File file : files) {
			if (!file.getName().startsWith("w") && !file.getName().startsWith("v")) continue;
			
			String projectFolder = file.getPath();
			File revFileFolder = new File(projectFolder + "/revFiles/");// revised file folder
			File[] revFiles = revFileFolder.listFiles();
			int a = 0;
			System.out.println(file.getPath());
			String outputFile = outputPath + file.getName();
			FileHelper.deleteDirectory(outputFile);
			for (File revFile : revFiles) {
				String fileName = revFile.getName();
				if (fileName.endsWith(".java")) {
					File prevFile = new File(projectFolder + "/prevFiles/prev_" + fileName);// previous file
					File diffentryFile = new File(projectFolder + "/DiffEntries/" + fileName.replace(".java", ".txt")); // DiffEntry file
					
					CommitPatchSingleStatementParser parser = new CommitPatchSingleStatementParser();
					
					final ExecutorService executor = Executors.newSingleThreadExecutor();
					// schedule the work
					final Future<?> future = executor.submit(new RunnableParser(prevFile, revFile, diffentryFile, parser));
					try {
						// where we wait for task to complete
						future.get(Configuration.SECONDS_TO_WAIT, TimeUnit.SECONDS);
						astEditScripts.append(parser.getAstEditScripts());
//						originalTrees.append(parser.getOriginalTree());
//						buggyTrees.append(parser.getBuggyTrees());
//						actionSets.append(parser.getActionSets());
						tokens.append(parser.getTokensOfSourceCode());
						sizes.append(parser.getSizes());
						patches.append(parser.getPatchesSourceCode());
						a ++;
						if (a % 100 == 0) {
							FileHelper.outputToFile(outputFile + "/EditScripts.list", astEditScripts, true);
							FileHelper.outputToFile(outputFile + "/Tokens.list", tokens, true);
							FileHelper.outputToFile(outputFile + "/Sizes.list", sizes, true);
							FileHelper.outputToFile(outputFile + "/Patches.list", patches, true);
							astEditScripts.setLength(0);
							tokens.setLength(0);
							sizes.setLength(0);
							patches.setLength(0);
							System.out.println(a);
						}
//						System.out.println(a);
					} catch (TimeoutException e) {
						err.println("task timed out");
						future.cancel(true /* mayInterruptIfRunning */ );
					} catch (InterruptedException e) {
						err.println("task interrupted");
					} catch (ExecutionException e) {
						err.println("task aborted");
					} finally {
						executor.shutdownNow();
					}
				}
			}
//			break;
			FileHelper.outputToFile(outputFile + "/EditScripts.list", astEditScripts, true);
//			FileHelper.outputToFile(outputFile + "/OriginalTrees.list", originalTrees, false);
//			FileHelper.outputToFile(outputFile + "/BuggyTrees.list", buggyTrees, false);
//			FileHelper.outputToFile(outputFile + "/ActionSets.list", actionSets, false);
			FileHelper.outputToFile(outputFile + "/Tokens.list", tokens, true);
			FileHelper.outputToFile(outputFile + "/Sizes.list", sizes, true);
			FileHelper.outputToFile(outputFile + "/Patches.list", patches, true);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/EditScripts.list", astEditScripts, false);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/OriginalTrees.list", originalTrees, false);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/BuggyTrees.list", buggyTrees, false);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/ActionSets.list", actionSets, false);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/Tokens.list", tokens, false);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/Sizes.list", sizes, false);
//			FileHelper.outputToFile("OUTPUT/GumTreeResults_Exp2/Patches.list", patches, false);

			astEditScripts.setLength(0);
			tokens.setLength(0);
			sizes.setLength(0);
			patches.setLength(0);
		}
	}
	
}
