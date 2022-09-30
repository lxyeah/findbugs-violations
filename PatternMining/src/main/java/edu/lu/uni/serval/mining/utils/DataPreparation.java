package edu.lu.uni.serval.mining.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import edu.lu.uni.serval.cluster.DataPreparer;
import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.mining.utils.MaxSizeSelector.MaxSizeType;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Prepare data for fix patterns mining and evaluation.
 * 
 * @author kui.liu
 *
 */
public class DataPreparation {
	
	/**
	 * Prepare data for token embedding in the process of fix patterns mining.
	 */
	public static void prepareDataForTokenEmbedding() {
		// Collect all data into one file.
		String editScriptsFilePath = Configuration.EDITSCRIPTS_FILE_PATH;
		String patchesSourceCodeFilePath = Configuration.PATCH_SOURCECODE_FILE_PATH;
		String buggyTokensFilePath = Configuration.BUGGY_CODE_TOKEN_FILE_PATH;
		String editScriptSizesFilePath = Configuration.EDITSCRIPT_SIZES_FILE_PATH;
		String alarmTypesFilePath = Configuration.ALARM_TYPES_FILE_PATH;

		String editScriptsFile = Configuration.EDITSCRIPTS_FILE;
		String patchesSourceCodeFile = Configuration.PATCH_SOURCECODE_FILE;
		String buggyTokensFile = Configuration.BUGGY_CODE_TOKENS_FILE;
		String editScriptSizesFile = Configuration.EDITSCRIPT_SIZES_FILE;
		String alarmTypesFile = Configuration.ALARM_TYPES_FILE;
		File file = new File(editScriptsFilePath);
		File[] subFiles = file.listFiles();
		
		boolean hasAlarmTypes = false;
		if (FileHelper.isValidPath(alarmTypesFilePath)) {
			hasAlarmTypes = true;
		}
		// Merge results of parsed patches.
		for (File subFile : subFiles) {
			String fileName = subFile.getName(); // edistScripts file
			String id = fileName.substring(fileName.lastIndexOf("_"));
			FileHelper.outputToFile(editScriptsFile, FileHelper.readFile(subFile), true);
			String patchesSourceCode = patchesSourceCodeFilePath + "patches" + id;
			FileHelper.outputToFile(patchesSourceCodeFile, FileHelper.readFile(patchesSourceCode), true);
			String sizes = editScriptSizesFilePath + "sizes" + id;
			FileHelper.outputToFile(editScriptSizesFile, FileHelper.readFile(sizes), true);
			String buggyTokens = buggyTokensFilePath + "tokens" + id;
			FileHelper.outputToFile(buggyTokensFile, FileHelper.readFile(buggyTokens), true);
			if (hasAlarmTypes) {
				String alarmTypes = alarmTypesFilePath + "alarmTypes" + id;
				FileHelper.outputToFile(alarmTypesFile, FileHelper.readFile(alarmTypes), true);
			}
		}
		
		try {
			List<Integer> sizes = MaxSizeSelector.readSizes(editScriptSizesFile);
			List<String> editScriptsList = DataPreparation.readStringList(editScriptsFile);
			List<String> tokensList = DataPreparation.readStringList(buggyTokensFile);
			System.out.println(sizes.size() + ":" + editScriptsList.size() + ":" + tokensList.size());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void dataSelection() {
		String editScriptsFile = Configuration.EDITSCRIPTS_FILE;
		String patchesSourceCodeFile = Configuration.PATCH_SOURCECODE_FILE;
		String buggyTokensFile = Configuration.BUGGY_CODE_TOKENS_FILE;
		String editScriptSizesFile = Configuration.EDITSCRIPT_SIZES_FILE;
		String alarmTypesFile = Configuration.ALARM_TYPES_FILE;
		
		// Select data by the size of edit script vectors.
		List<Integer> sizesList;
		try {
			sizesList = MaxSizeSelector.readSizes(editScriptSizesFile);
//			int maxSize = MaxSizeSelector.selectMaxSize(MaxSizeType.UpperWhisker, sizesList);
			int maxSize = 20; // TODO
			List<Integer> outlierIndexes = new ArrayList<>();
			for (int i = 0, size = sizesList.size(); i < size; i ++) {
				if (sizesList.get(i) > maxSize) {
					outlierIndexes.add(i);
				}
			}
			FileHelper.outputToFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_EDIT_SCRIPTS, "" + maxSize, false);
			
			selectData(editScriptsFile, outlierIndexes, Configuration.SELECTED_EDITSCRIPTES_FILE);
			selectData(patchesSourceCodeFile, outlierIndexes, Configuration.PATCH_SIGNAL, Configuration.SELECTED_PATCHES_SOURE_CODE_FILE);
			int maxTokenVectorSize = selectDataOfSourceCodeTokens(buggyTokensFile, outlierIndexes, Configuration.SELECTED_BUGGY_TOKEN_FILE);
			FileHelper.outputToFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_SOURCE_CODE, "" + maxTokenVectorSize, false);
			if (FileHelper.isValidPath(alarmTypesFile)) {
				selectData(alarmTypesFile, outlierIndexes, Configuration.SELECTED_ALARM_TYPES_FILE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<String> readStringList(String inputFile) {
		List<String> list = new ArrayList<>();
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(inputFile);
			scanner = new Scanner(fis);
			while(scanner.hasNextLine()) {
				list.add(scanner.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return list;
	}
	
	public static void prepareData() {
		String editScriptsFile = "Dataset/editScripts.list";//Configuration.EDITSCRIPT_SIZES_FILE;
		String patchesSourceCodeFile = "Dataset/sourceCode.list";//Configuration.PATCH_SOURCECODE_FILE;
		String buggyTokensFile = "Dataset/tokens.list";//Configuration.BUGGY_CODE_TOKENS_FILE;
		String editScriptSizesFile = "Dataset/editScriptSizes.list";//Configuration.EDITSCRIPT_SIZES_FILE;
		// Select data by the size of edit script vectors.
		List<Integer> sizesList;
		try {
			sizesList = MaxSizeSelector.readSizes(editScriptSizesFile);
			int maxSize = MaxSizeSelector.selectMaxSize(MaxSizeType.ThirdQuartile, sizesList);
			List<Integer> outlierIndexes = new ArrayList<>();
			for (int i = 0, size = sizesList.size(); i < size; i++) {
				if (sizesList.get(i) > maxSize) {
					outlierIndexes.add(i);
				}
			}
			FileHelper.outputToFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_EDIT_SCRIPTS, "" + maxSize, false);
System.out.println("Outliers: " + outlierIndexes.size());
			selectData(editScriptsFile, outlierIndexes, Configuration.SELECTED_EDITSCRIPTES_FILE);
System.out.println("Outliers: " + outlierIndexes.size());
			selectData(patchesSourceCodeFile, outlierIndexes, Configuration.PATCH_SIGNAL,
					Configuration.SELECTED_PATCHES_SOURE_CODE_FILE);
System.out.println("Outliers: " + outlierIndexes.size());
			int maxTokenVectorSize = selectDataOfSourceCodeTokens(buggyTokensFile, outlierIndexes,
					Configuration.SELECTED_BUGGY_TOKEN_FILE);
			FileHelper.outputToFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_SOURCE_CODE, "" + maxTokenVectorSize,
					false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void selectData(String intputFile, List<Integer> outlierIndexList, String outputFile) {
		List<Integer> outlierIndexes = new ArrayList<>();
		outlierIndexes.addAll(outlierIndexList);
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(intputFile);
			scanner = new Scanner(fis);
			int index = 0;
			StringBuilder builder = new StringBuilder();
			int counter = 0;
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (outlierIndexes.contains(index)) {
					outlierIndexes.remove(new Integer(index));
				} else {
					builder.append(line + "\n");
					if (++ counter % 100000 == 0) {
						FileHelper.outputToFile(outputFile, builder, true);
						builder.setLength(0);
					}
				}
				index ++;
			}
			
			FileHelper.outputToFile(outputFile, builder, true);
			builder.setLength(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (scanner != null) {
					scanner.close();
					scanner = null;
				}
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void selectData(String inputFile, List<Integer> outliers, String startingSignal, String outputFile) {
		List<Integer> outlierIndexes = new ArrayList<>();
		outlierIndexes.addAll(outliers);
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(inputFile);
			scanner = new Scanner(fis);
			int index = -1;
			StringBuilder builder = new StringBuilder();
			int counter = 0;
			String singleEntity = "";
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith(startingSignal)) { //if (line.equals(startingSignal)) {
					if (!"".equals(singleEntity)) {
						if (outlierIndexes.contains(index)) {
							outlierIndexes.remove(new Integer(index));
						} else {
							builder.append(singleEntity + "\n");
							if (++ counter % 100000 == 0) {
								FileHelper.outputToFile(outputFile, builder, true);
								builder.setLength(0);
							}
						}
						singleEntity = "";
					}
					index ++;
				}
				singleEntity += line + "\n";
			}
			
			FileHelper.outputToFile(outputFile, builder, true);
			builder.setLength(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (scanner != null) {
					scanner.close();
					scanner = null;
				}
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static int selectDataOfSourceCodeTokens(String inputFile, List<Integer> outlierIndexList, String outputFile) {
		List<Integer> outlierIndexes = new ArrayList<>();
		outlierIndexes.addAll(outlierIndexList);
		FileInputStream fis = null;
		Scanner scanner = null;
		int size = 0;
		try {
			fis = new FileInputStream(inputFile);
			scanner = new Scanner(fis);
			int index = 0;
			StringBuilder builder = new StringBuilder();
			int counter = 0;
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (outlierIndexes.contains(index)) {
					outlierIndexes.remove(new Integer(index));
				} else {
					builder.append(line + "\n");
					if (++ counter % 100000 == 0) {
						FileHelper.outputToFile(outputFile, builder, true);
						builder.setLength(0);
					}
					String[] tokens = line.split(" ");
					if (tokens.length > size) size = tokens.length;
				}
				index ++;
			}
			
			FileHelper.outputToFile(outputFile, builder, true);
			builder.setLength(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (scanner != null) {
					scanner.close();
					scanner = null;
				}
				if (fis != null) {
					fis.close();
					fis = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return size;
	}
	
	/**
	 * Prepare data for feature learning.
	 */
	public static void prepareDataForFeatureLearning() {
		String zeroVector = "";
		for (int i =0, length = Configuration.VECTOR_SIZE_OF_EMBEDED_TOKEN1 - 1; i < length; i ++) {
			zeroVector += "0, ";
		}
		zeroVector += "0";
		int maxSize = Integer.parseInt(FileHelper.readFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_EDIT_SCRIPTS).trim());

		String embeddedTokensFile = Configuration.EMBEDDED_EDIT_SCRIPT_TOKENS;
		Map<String, String> embeddedTokens = readEmbeddedTokens(embeddedTokensFile);

		String editScriptsFile = Configuration.SELECTED_EDITSCRIPTES_FILE;
		String outputFile = Configuration.VECTORIED_EDIT_SCRIPTS;
		dataPrepare(editScriptsFile, maxSize, outputFile, embeddedTokens, zeroVector);
	}
	
	public static Map<String, String> readEmbeddedTokens(String embeddedTokensFile) {
		Map<String, String> embeddedTokens = new HashMap<>();
		File file = new File(embeddedTokensFile);
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(file);
			scanner = new Scanner(fis);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				int firstBlankIndex = line.indexOf(" ");
				String token = line.substring(0, firstBlankIndex);
				String value = line.substring(firstBlankIndex + 1).replaceAll(" ", ", ");
				embeddedTokens.put(token, value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return embeddedTokens;
	}

	private static void dataPrepare(String inputFile, int maxSize, String outputFile, Map<String, String> embeddedTokens, String zeroVector) {
		File file = new File(inputFile);
		FileInputStream fis = null;
		Scanner scanner = null;
		StringBuilder builder = new StringBuilder();
		int counter = 0;
		int batchSize = 100;
		
		try {
			fis = new FileInputStream(file);
			scanner = new Scanner(fis);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				StringBuilder vectorStr = convertToVector(embeddedTokens, line, maxSize, zeroVector);
				builder.append(vectorStr);
				if (++ counter % batchSize == 0) {
					FileHelper.outputToFile(outputFile, builder, true);
					builder.setLength(0);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(counter + " : " + (counter % batchSize));
		FileHelper.outputToFile(outputFile, builder, true);
		builder.setLength(0);
	}
	
	public static StringBuilder convertToVector(Map<String, String> embeddedTokens, String line, int maxSize, String zeroVector) {
		String[] tokens = line.split(" ");
		StringBuilder vectorStr = new StringBuilder();
		int length = tokens.length;
		if (length == maxSize) {
			for (int i = 0; i < length - 1; i ++) {
				String token = tokens[i];
				String feature = embeddedTokens.get(token);
				if (feature == null) {
					feature = zeroVector;
				}
				vectorStr.append(feature).append(", ");
			}
			String feature = embeddedTokens.get(tokens[length - 1]);
			if (feature == null) {
				feature = zeroVector;
			}
			vectorStr.append(feature).append("\n");
		} else {
			if (length > maxSize) {
				System.out.println(length);
			}
			for (int i = 0; i < length; i ++) {
				String token = tokens[i];
				String feature = embeddedTokens.get(token);
				if (feature == null) {
					feature = zeroVector;
				}
				vectorStr.append(feature).append(", ");
			}
			for (int i = length; i < maxSize - 1; i ++) {
				vectorStr.append(zeroVector).append(", ");
			}
			vectorStr.append(zeroVector).append("\n");
		}
		
		return vectorStr;
	}

	/**
	 * Prepare data for clustering.
	 */
	public static void prepareDataForClustering(String featureFileName) {
		String featureFile = Configuration.EXTRACTED_FEATURES + featureFileName;
		String arffFile = Configuration.CLUSTER_INPUT;
		DataPreparer.prepareData(featureFile, arffFile);
	}
	
	/**
	 * Read cluster results.
	 */
	public static List<Integer> readClusterResults() {
		List<Integer> clusterResults = new ArrayList<>();
		String clusterResultsFile = Configuration.CLUSTER_OUTPUT;
		String results = FileHelper.readFile(clusterResultsFile);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new StringReader(results));
			String line = null;
			while ((line = reader.readLine()) != null) {
				clusterResults.add(Integer.parseInt(line));
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
		return clusterResults;
	}
	
	public static Map<Integer, List<Integer>> readClusterResult(List<Integer> clusterResults) {
		Map<Integer, List<Integer>> clusters = new HashMap<>();
		
		for (int i = 0, size = clusterResults.size(); i < size; i ++) {
			int clusterNo = clusterResults.get(i);
			if (clusters.containsKey(clusterNo)) {
				clusters.get(clusterNo).add(i + 1);
			} else {
				List<Integer> newCLuster = new ArrayList<>();
				newCLuster.add(i + 1);
				clusters.put(clusterNo, newCLuster);
			}
		}

		return clusters;
	}
	
	/**
	 * Data for un-supervised learning.
	 */
	public static void prepareTokensForEvaluation1() {
		String outputFile = Configuration.EMBEDDING_DATA_TOKENS1;
		FileHelper.outputToFile(outputFile, FileHelper.readFile(Configuration.SELECTED_BUGGY_TOKEN_FILE), false);
		List<File> files = FileHelper.getAllFilesInCurrentDiectory(Configuration.TEST_DATA_FILE, ".list");
		for (File file : files) {
			FileHelper.outputToFile(outputFile, FileHelper.readFile(file), true);
		}
	}
	
	public static void prepareDataForFeatureLearningOfEvaluation1() {
		String zeroVector = "";
		for (int i =0, length = Configuration.VECTOR_SIZE_OF_EMBEDED_TOKEN2 - 1; i < length; i ++) {
			zeroVector += "0, ";
		}
		zeroVector += "0";
		int maxSize = Integer.parseInt(FileHelper.readFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_SOURCE_CODE));
		
		String allEmbeddedTokens = Configuration.EMBEDDED_ALL_TOKENS1;
		Map<String, String> embeddedTokens = readEmbeddedTokens(allEmbeddedTokens);

		// Testing data 
		String clusteredTokens = Configuration.TEST_DATA_FILE;
		List<File> files = FileHelper.getAllFilesInCurrentDiectory(clusteredTokens, ".list");
		for (@SuppressWarnings("unused") File file : files) {
			
		}
		String allTokensOfSourceCode = Configuration.EMBEDDING_DATA_TOKENS1; // TODO testing data should be separated.
		dataPrepare(allTokensOfSourceCode, maxSize, Configuration.VECTORIED_ALL_SOURCE_CODE1, embeddedTokens, zeroVector);
	}
	
	/**
	 * Data for supervised learning.
	 */
	public static void prepareTokensForEvaluation2(Map<Integer, Integer> commonClustersMappingLabel) {
		String clusteredTokens = Configuration.CLUSTERED_TOKENSS_FILE;
		String outputFile = Configuration.EMBEDDING_DATA_TOKENS2;
		
		List<File> files = FileHelper.getAllFilesInCurrentDiectory(clusteredTokens, ".list");
		for (File file : files) {
			String fileName = file.getName();
			String clusterNumStr = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf(".list"));
			int clusterNum = Integer.parseInt(clusterNumStr);
			if (commonClustersMappingLabel.containsKey(clusterNum)) {
				String content = FileHelper.readFile(file);
				FileHelper.outputToFile(outputFile, content, true);
			}
		}
		files.clear();
		files = FileHelper.getAllFilesInCurrentDiectory(Configuration.TEST_DATA_FILE, ".list");
		for (File file : files) {
			FileHelper.outputToFile(outputFile, FileHelper.readFile(file), true);
		}
	}

	public static void prepareDataForFeatureLearningOfEvaluation2(Map<Integer, Integer> commonClustersMappingLabel) {
		String zeroVector = "";
		for (int i =0, length = Configuration.VECTOR_SIZE_OF_EMBEDED_TOKEN2 - 1; i < length; i ++) {
			zeroVector += "0, ";
		}
		zeroVector += "0";
		
		String allEmbeddedTokensOfEvaluation = Configuration.EMBEDDED_ALL_TOKENS2;
		Map<String, String> embeddedTokens = readEmbeddedTokens(allEmbeddedTokensOfEvaluation);

		int maxSize = Integer.parseInt(FileHelper.readFile(Configuration.MAX_TOKEN_VECTORS_SIZE_OF_SOURCE_CODE));
		// Training data
		String clusteredTokens = Configuration.CLUSTERED_TOKENSS_FILE;
		List<File> files = FileHelper.getAllFilesInCurrentDiectory(clusteredTokens, ".list");
		for (File file : files) {
			String fileName = file.getName();
			String clusterNumStr = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf(".list"));
			int clusterNum = Integer.parseInt(clusterNumStr);
			if (commonClustersMappingLabel.containsKey(clusterNum)) {
				dataPrepare(file.getPath(), maxSize, Configuration.TRAINING_DATA, embeddedTokens, zeroVector, clusterNum);
			}
		}
		// Testing data
		files.clear();
		String testingData = Configuration.TEST_DATA_FILE;
		files = FileHelper.getAllFilesInCurrentDiectory(testingData, ".list");
		String testingDataPath = Configuration.TESTING_DATA;
		for (File file : files) {
			String fileName = file.getName();
			fileName.replace(".list", ".csv");
			dataPrepare(file.getPath(), maxSize, testingDataPath + fileName, embeddedTokens, zeroVector, 0);
		}
	}

	private static void dataPrepare(String inputFile, int maxSize, String outputFile, Map<String, String> embeddedTokens,
			String zeroVector, int clusterNum) {
		FileInputStream fis = null;
		Scanner scanner = null;
		StringBuilder builder = new StringBuilder();
		int counter = 0;
		
		try {
			fis = new FileInputStream(inputFile);
			scanner = new Scanner(fis);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				StringBuilder vectorStr = convertToVector(embeddedTokens, line, maxSize, zeroVector, clusterNum);
				builder.append(vectorStr);
				if (++ counter % 10000 == 0) {
					FileHelper.outputToFile(outputFile, builder, true);
					builder.setLength(0);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		FileHelper.outputToFile(outputFile, builder, true);
		builder.setLength(0);
	}
	
	private static StringBuilder convertToVector(Map<String, String> embeddedTokens, String line, int maxSize, String zeroVector, int clusterNum) {
		String[] tokens = line.split(" ");
		StringBuilder vectorStr = new StringBuilder();
		int length = tokens.length;
		if (length == maxSize) {
			for (int i = 0; i < length; i ++) {
				String token = tokens[i];
				vectorStr.append(embeddedTokens.get(token) + ", ");
			}
		} else {
			for (int i = 0; i < length; i ++) {
				String token = tokens[i];
				vectorStr.append(embeddedTokens.get(token) + ", ");
			}
			for (int i = length; i < maxSize; i ++) {
				vectorStr.append(zeroVector + ", ");
			}
		}
		
		vectorStr.append(clusterNum + "\n");
		
		return vectorStr;
	}

	public static Map<Integer, Integer> readCommonCLusters() {
		Map<Integer, Integer> commonClustersMappingLabel = new HashMap<>();
		String commonClusters = FileHelper.readFile(Configuration.CLUSTERNUMBER_LABEL_MAP);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new StringReader(commonClusters));
			String line = reader.readLine();
			while ((line = reader.readLine()) != null) {
				String[] strArray = line.split(" : ");
				int key = Integer.parseInt(strArray[1]);
				int value = Integer.parseInt(strArray[0]);
				commonClustersMappingLabel.put(key, value);
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
		return commonClustersMappingLabel;
	}

	public static void separateTrainingDataFeatures() {
		String trainingDataFeatures = Configuration.FEATURES_OF_TRAINING_DATA;
		List<File> featureFiles = FileHelper.getAllFilesInCurrentDiectory(trainingDataFeatures, ".csv");
		File featureFile = featureFiles.get(0);
		// File featureFile = new File(Configuration.FEATURES_OF_TRAINING_DATA + "");
		
		Map<Integer, Integer> numbersMap = readNumberOfInstances(); // <Integer, Integer>: <clusterNum, numOfInstances>
		Map<Integer, Integer> orders = new HashMap<>(); //<Integer, Integer> : <order, clusterNum>
		Map<Integer, String> fileNames = new HashMap<>();
		String clusteredTokens = Configuration.CLUSTERED_TOKENSS_FILE;
		List<File> files = FileHelper.getAllFilesInCurrentDiectory(clusteredTokens, ".list");
		int order = 1;
		for (File file : files) {
			String fileName = file.getName();
			String clusterNumStr = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf(".list"));
			int clusterNum = Integer.parseInt(clusterNumStr);
			if (numbersMap.containsKey(clusterNum)) {
				orders.put(order, clusterNum);
				fileNames.put(order, fileName);
				order ++;
			}
		}
		
		String featuresOfClusterPath = Configuration.FEATURES_OF_COMMON_CLUSTERS;
		order = 1;
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(featureFile);
			scanner = new Scanner(fis);
			int counter = 0;
			StringBuilder features = new StringBuilder();
			while (scanner.hasNextLine()) {
				features.append(scanner.nextLine() + "\n");
				counter ++;
				if (counter == numbersMap.get(orders.get(order))) {
					FileHelper.outputToFile(featuresOfClusterPath + fileNames.get(order), features, false);
					features.setLength(0);
					counter = 0;
					order ++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<Integer, Integer> readNumberOfInstances() {
		Map<Integer, Integer> numbersMap = new HashMap<>();
		String fileContent = FileHelper.readFile(Configuration.COMMON_CLUSTERS_SIZES);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new StringReader(fileContent));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] numbers = line.split(":");
				numbersMap.put(Integer.parseInt(numbers[0]), Integer.parseInt(numbers[1]));
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
		return numbersMap;
	}

	public static Map<Integer, Integer> readLabelMapClusterNum() {
		Map<Integer, Integer> labelMapClusterNumMap = new HashMap<>();
		String fileContent = FileHelper.readFile(Configuration.CLUSTERNUMBER_LABEL_MAP);
		BufferedReader reader = null;
		reader = new BufferedReader(new StringReader(fileContent));
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String[] labelMapClusterNum = line.split(":");
				labelMapClusterNumMap.put(Integer.parseInt(labelMapClusterNum[0]), Integer.parseInt(labelMapClusterNum[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
