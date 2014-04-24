package experiments;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.print.attribute.standard.Finishings;

import lda.Lda;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import utilities.Day;
import utilities.Index;
import utilities.Preprocessor;
import algorithms.BurstySimDater;
import algorithms.Simple;
import burstdetection.BurstEngine;

public class CopyOfCheckExp {

	//kjetil: 20000101-20081130
	//topix: 2008/09/01 - 2008,8,1
	//call1: 1900/01/01 - 1900,0,1
	//call2: 1903/01/01 - 1903,0,1
	//call3: 1908/01/01 - 1908,0,1
	//slashdot: 19980102 - 1998,0,2
	//engadget: 20040301 - 2004,02,01 to 20111101 - 2011,10,01
	private static Day startDay = new Day(1900,0,1);
	private static String burstsFilename = "bursts.txt";
	
	private static String FINALdatasetFile = "FINAL.txt";
	private static final String testdocsFilename = "testFile.txt";
	private static final String trainingdocsFilename = "trainingFile.txt";
	private static String DFtermsFile = "DF_TERMS.txt";

	public static void main(String[] args) {

		try {

			//"call1_tok_nostop_jaccard.txt";
			String datasetFile = args[0];
			
			File path = new File(System.getProperty("luceneIndex"), "index");
			Directory dir = new SimpleFSDirectory(path);
			boolean fromScratch = false;
			boolean otherTestset = false;
			boolean removeRareTerms = false;
			boolean newbursts = false;
			boolean justTrain = false;
			
			int minDF = 4;
			//call1: 110387, call2: 133684, call3: 129732:
			//call1-jaccard: 85914
			int totalDocs = Integer.parseInt(args[5]);
			int testingDocs = 1000;

			for (int i = 0; i<1;i++){

				if (fromScratch && removeRareTerms){

					//1. Build Lucene Index
					System.out.println("Building first Lucene index...");
					dir = Index.buildLuceneIndex("luceneIndex", datasetFile, startDay, false);
					System.out.println("Built first Lucene index!");

					//2. Open the Lucene Index				
					//3. Remove all terms with df<20 and rebuild Lucene index
					Index lucIndex = new Index(dir);
					System.out.println("Creating DF terms file...");
					lucIndex.createMinDFsFile(DFtermsFile, minDF);
					System.out.println("Created DF terms file!");

					System.out.println("Removing terms with DF<"+ minDF + "...");
					Preprocessor.removeTerms(DFtermsFile, datasetFile, FINALdatasetFile, startDay);
					//FINALdatasetFile = datasetFile;
					System.out.println("Removed terms with DF<"+ minDF + "...");
				}

				if (fromScratch || otherTestset)
				{
					if (!removeRareTerms)
						FINALdatasetFile = datasetFile;
					
					System.out.println("Creating training and test files...");
					Preprocessor.createTestTrainingFiles(FINALdatasetFile, testdocsFilename, trainingdocsFilename, testingDocs, totalDocs);
					System.out.println("Created training and test files!");

					System.out.println("Building final Lucene index...");
					dir = Index.buildLuceneIndex("luceneIndex", trainingdocsFilename, startDay, true);
					System.out.println("Built final Lucene index!");
				}
				else {
					path = new File(System.getProperty("luceneIndex"), "indexFinal");
					dir = new SimpleFSDirectory(path);
				}

				int numberOfTopics = 20;
				int termsPerTopic = 30;
				
				if (justTrain){
					Lda.LDATrainTopics(trainingdocsFilename, numberOfTopics);
					System.exit(0);
				}
					
				Index lucIndex2 = new Index(dir);
				BurstySimDater chk1 = new BurstySimDater();
				
				chk1.readDocsFile(trainingdocsFilename, startDay);
				System.out.println("Read dataset file!");

				
				
				int extendLength = 5;
				
				if (newbursts){
					Preprocessor.computeFreqSequences(chk1.getDataset(), "timeSeries"+trainingdocsFilename, false);
					System.out.println("Computed freq sequences");	
//					BurstEngine.computeMAX2Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, true);			
//					BurstEngine.computeMAX1Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, true,1);
					BurstEngine.computeMAXKBurstsSplit("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, 150, true);
					//System.exit(1);
				}

				/* 
				 * topics array contains one entry per testing document. Each entry contains termsPerTopic terms
				 * that describe the topic to which the corresponding testing document was assigned.
				 */
				String [] topics = null;
				
				//topics = Lda.LDATopics(trainingdocsFilename, testdocsFilename, numberOfTopics, termsPerTopic);
				
				chk1 = new BurstySimDater();
				Simple simple = new Simple();
				
				boolean checkAlg = true;
				
				if (checkAlg){
					chk1.readDocs(testdocsFilename, startDay);
					
				}
				else{
					simple.readTestDocs(testdocsFilename, startDay);
				}
				
				System.out.println("Have read testSet file");

				
				if (checkAlg){
					//chk1.readBurstsFile(burstsFilename);
				}
				else{
					simple.readBurstsFile(burstsFilename);	
				}
				System.out.println("Have read bursts file!");

				lucIndex2.computeIdfs();
				
				boolean randomInterval = false;
				boolean weightScores = false;
				boolean union = true;
				boolean weight = true;
				String cliqueResultsFilename = "cliqueResults.txt";
				String cliqueFilename = "cliques.txt";
				String graphFilename = "graphs.txt";
				int p = 1;
				int knn = 5000;
				
				int ktfidf = Integer.parseInt(args[0]);
				int x = Integer.parseInt(args[1]);
				int kJaccard =  Integer.parseInt(args[2]);
				extendLength =  Integer.parseInt(args[3]);
				int kCliques =  Integer.parseInt(args[4]);
				
				int selector = BurstySimDater.TF;
				int scoreType = BurstySimDater.NODECNTSCORE;
				String resultsFilename = "results.txt";
				
				double minJaccard = 0.1;
				
				
				if (checkAlg){
					chk1.jaccardAlgorithm(selector, kJaccard, minJaccard, x, ktfidf, extendLength, lucIndex2, scoreType, kCliques);
//					chk1.rankedWeightedCliques(selector, ktfidf, extendLength, lucIndex2, scoreType, kCliques);
//					chk1.checkAlgorithm(extendLength, resultsFilename, lucIndex2,
//						ktfidf, randomInterval, CheckBurstiness.NODECNTSCORE, weightScores, p, knn, topics, selector);
				}
				else{
				
					simple.graphAlgorithm(graphFilename, cliqueFilename, cliqueResultsFilename, weight, union, lucIndex2, ktfidf, kCliques, selector);
				}
				
			}

		} catch (IOException e) {

			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}


	}	
	



}
