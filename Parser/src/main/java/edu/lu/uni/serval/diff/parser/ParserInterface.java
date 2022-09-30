package edu.lu.uni.serval.diff.parser;

import java.io.File;

public interface ParserInterface {

	public void parseCodeChangeDiffs(File prevFile, File revFile, File diffEntryFile);
	
	public String getAstEditScripts();

	public String getPatchesSourceCode();

	public String getBuggyTrees();

	public String getSizes();

	public String getTokensOfSourceCode();

	public String getOriginalTree();

	public String getActionSets();
	
}
