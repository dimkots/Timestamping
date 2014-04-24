package experiments;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import burstdetection.BurstEngine;

import utilities.Constants;
import utilities.Day;
import utilities.Index;
import utilities.Preprocessor;
import utilities.Sorter;
import utilities.Stats;
import algorithms.Simple;

public class SimpleExp {
	
	//kjetil: 20000101-20081130
	//topix: 2008/09/01 - 2008,8,1
	//call1: 1900/01/01 - 1900,0,1
	//call2: 1903/01/01 - 1903,0,1
	//slashdot: 19980102 - 1998,0,2
	//engadget: 20040301 - 2004,02,01 to 20111101 - 2011,10,01
	private static Day startDay = new Day(1900,0,1);
	private static String burstsFilename = "bursts.txt";
	private static String datasetFile = "NOSP_call1_HUGESAMPLE.txt";
	
	private static String FINALdatasetFile = "FINAL.txt";
	
	private static String DFtermsFile = "DF_TERMS.txt";
	
	private static String testdocsFilename = "testNewsFile.txt";
	private static String trainingdocsFilename = "trainingNewsFile.txt";
	
	private static String graphFilename = "graphsScores.txt";
	private static String cliqueFilename = "cliquesScores.txt";
	private static String cliquesResultsFilename = "cliquesResultsScores.txt";
	public static void main(String[] args) {
		
		try {

			File path = new File(System.getProperty("luceneIndex"), "index");
			Directory dir = new SimpleFSDirectory(path);
			boolean fromScratch = true;
			boolean otherTestset = true;
			
			for (int i = 0; i<1;i++){
			
				if (fromScratch){
					
					//1. Build Lucene Index
					System.out.println("Building first Lucene index...");
					dir = Index.buildLuceneIndex("luceneIndex", datasetFile, startDay, false);
					System.out.println("Built first Lucene index!");
					
					//2. Open the Lucene Index				
					//3. Remove all terms with df<20 and rebuild Lucene index
					Index lucIndex = new Index(dir);
					System.out.println("Creating DF terms file...");
					lucIndex.createMinDFsFile(DFtermsFile, 20);
					System.out.println("Created DF terms file!");
					
					System.out.println("Removing terms with DF<20...");
					Preprocessor.removeTerms(DFtermsFile, datasetFile, FINALdatasetFile, 20, startDay);
					System.out.println("Removed terms with DF<20!");
				}
				
				if (fromScratch || otherTestset)
				{
					System.out.println("Creating training and test files...");
					Preprocessor.createTestTrainingFiles(FINALdatasetFile, testdocsFilename, trainingdocsFilename, 2000, 91000);
					System.out.println("Created training and test files!");
					
					System.out.println("Building final Lucene index...");
					dir = Index.buildLuceneIndex("luceneIndex", trainingdocsFilename, startDay, true);
					System.out.println("Built final Lucene index!");
				}
				else {
					
					path = new File(System.getProperty("luceneIndex"), "indexFinal");
					dir = new SimpleFSDirectory(path);
				}
					
				
				Index lucIndex2 = new Index(dir);
				
				
				Simple s1 = new Simple(trainingdocsFilename,startDay);
				System.out.println("Read training file!");
				
			
				Preprocessor.computeFreqSequences(s1.getDataset(), "timeSeries"+trainingdocsFilename, false);
				
				HashMap<String, HashMap<Integer, Integer>> tokens = new HashMap<String, HashMap<Integer, Integer>>();
				HashMap<String, Integer> corpusTokens = new HashMap<String, Integer>();
				
				Preprocessor.computeTfsInDocs(trainingdocsFilename, startDay, tokens, corpusTokens);
				System.out.println("Computed freq sequences");
				//BurstEngine.computeMAX1Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, false, 1);
				//BurstEngine.computeMAX2Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, false);
				
				BurstEngine.computeMAXKBurstsSplit("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, 5, false);
				Simple s2 = new Simple();
				s2.readTestDocs(testdocsFilename,startDay);
				System.out.println("Have read testDocs file!");

				s2.readBurstsFile(burstsFilename);
				System.out.println("Have read bursts file!");

				lucIndex2.computeIdfs();
				
				
				System.out.println("k-terms\tcliques\tunion\tweight\tdays\tprecision\tnobursts\tmanybursts");
				int ktfidf;
				int cliques;
				boolean union = false;
				boolean weight = true;
				
				for(int uw = 0; uw < 2; uw++){
					if(uw == 0){
						union = true;
						weight = true;
					}
					else if(uw == 1){
						union = false;
						weight = true;
					}
					else if(uw == 2){
						union = true;
						weight = false;
					}
					else if(uw == 3){
						union = true;
						weight = true;
					}
					
					for (cliques = 1; cliques < 5; cliques++){
						
						for (ktfidf = 5; ktfidf <= 35; ktfidf+=10){
							
							System.out.print(ktfidf +"\t"+ cliques+"\t");
							
							if (union)
								System.out.print("U\t");
							else
								System.out.print("I\t");
							if (weight)
								System.out.print("W\t");
							else
								System.out.print("NON W\t");
							s2.graphAlgorithm(graphFilename, cliqueFilename, cliquesResultsFilename, weight, union, lucIndex2, ktfidf, cliques, corpusTokens);	
						}
							
					}
						
				}
				
				
				
				
				
//				s2.testRepTermsWithGraphs(graphFilename, cliqueFilename, cliquesResultsFilename, true, lucIndex, ktfidf);

				//lucIndex.computeIdfs();
				
				
				//System.out.println("Iteration:"+ i);
				
//				lucIndex.computeIdfs();
//				int intervalCnt = 10;
//				
//				int frameLength = 10;
//				int extDays = frameLength/(2*intervalCnt);
//				sum10+= s2.sumSeriesAlgorithm(frameLength, extDays, lucIndex, ktfidf);
//				
//				frameLength = 30;
//				extDays = frameLength/(2*intervalCnt);
//				sum30+= s2.sumSeriesAlgorithm(frameLength, extDays, lucIndex, ktfidf);
//				
//				frameLength = 60;
//				extDays = frameLength/(2*intervalCnt);
//				sum60+= s2.sumSeriesAlgorithm(frameLength, extDays, lucIndex, ktfidf);
//				
//				frameLength = 90;
//				extDays = frameLength/(2*intervalCnt);
//				sum90+= s2.sumSeriesAlgorithm(frameLength, extDays, lucIndex, ktfidf);
//				
//				frameLength = 120;
//				extDays = frameLength/(2*intervalCnt);
//				sum120+= s2.sumSeriesAlgorithm(frameLength, extDays, lucIndex, ktfidf);
//				
				
	

			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		

		}	
		
	

}
