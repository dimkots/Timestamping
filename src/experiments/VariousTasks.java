package experiments;

import java.io.IOException;

import algorithms.Simple;
import utilities.Constants;
import utilities.Day;
import utilities.Index;
import utilities.Preprocessor;

public class VariousTasks {

	// kjetil: 20000101-20081130
	// topix: 2008/09/01 - 2008,8,1
	// call1: 1900/01/01 - 1900,0,1
	// call2: 1903/01/01 - 1903,0,1
	// call3: 1908/01/01 - 1908,0,1
	// slashdot: 19980102 - 1998,0,2
	// engadget: 20040301 - 2004,02,01 to 20111101 - 2011,10,01
	
	
	public static void main(String[] args) {
		// Preprocessor.removeTerms("CallDFs.txt", "call1.txt",20,2);

		Day startDayCall1 = new Day(1908,0,1);
		Day startDayCall2 = new Day(1908,0,1);
		Day startDayCall3 = new Day(1908,0,1);
		//Preprocessor.removeStopWords("call3_tok.txt", "call3_tok_nostop.txt", startDay);
		String[] datasetFiles = {"call1_tok.txt","call2_tok.txt","call3_tok.txt"};
		Day[] startDays = {startDayCall1, startDayCall2, startDayCall3};
		
		Preprocessor.createLexicon(datasetFiles, "top10kdfsLexicon.txt", startDays);
		
		
		// Preprocessor.countTerms("dfGE20newsFile.txt", 1);
		//Preprocessor.mergePartitions(
			//	"D:\\Programming\\Java\\icdm\\related\\m1", "news.txt");

		// try {
		// Preprocessor.countTerms("dfGE20newsFileNOSTOP.txt", 1);

		// Simple s1 = new Simple("newsFile.txt",1);
		// System.out.println("Read training file!");
		//
		// Preprocessor.computeFreqSequences(s1.getDataset(),
		// "newsFileTIME-SERIES.txt", false);
		// System.out.println("Computed freq sequences");
		//
		// String datasetFile = "dfGE20call1.txt";
		// Index.buildLuceneIndex("luceneIndex", datasetFile, 2);
		// } catch (IOException e) {
		// // // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//

	}

}
