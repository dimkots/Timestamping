package experiments;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import utilities.Index;
import utilities.Preprocessor;
import algorithms.Simple;
import burstdetection.BurstEngine;

public class TestRepTerms {

	private static String burstsFilename = "MAX1burstsCall.txt";
	private static String dataBigfile = "dfGE20call1.txt";
	private static String testdocsFilename = "testNewsFile.txt";
	private static String trainingdocsFilename = "trainingNewsFile.txt";
	
	
	
	private static String graphFilename = "graphsScores.txt";
	private static String cliqueFilename = "cliquesScores.txt";
	private static String cliquesResultsFilename = "cliquesResultsScores.txt";
	
	public static void main(String[] args) {

		double sum10 = 0;
		double sum30 = 0;
		double sum60 = 0;
		double sum90 = 0;
		double sum120 = 0;
		
		for (int i = 0; i<1;i++){
			
			try {
			
				System.out.println(i);
				Preprocessor.createTestTrainingFiles(dataBigfile, testdocsFilename, trainingdocsFilename, 1000, 110387);
				System.out.println("Have created training and test files!");
				
				
				Simple s1 = new Simple(trainingdocsFilename, 2);
				System.out.println("Read training file!");
				
				Preprocessor.computeFreqSequences(s1.getDataset(), "timeSeries"+trainingdocsFilename, false);
				System.out.println("Computed freq sequences");
				
				BurstEngine.computeMAX1Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, false, 2);
				
				File path = new File(System.getProperty("luceneIndex"), "callIndex");
				   
				     
				Directory dir = new SimpleFSDirectory(path);
				Index lucIndex = new Index(dir);

				
				Simple s2 = new Simple();
				s2.readTestDocs(testdocsFilename, 2);
				System.out.println("Have read testDocs file!");

				s2.readBurstsFile(burstsFilename);
				System.out.println("Have read bursts file!");

				int ktfidf = 20;

				//s2.testCharTerms(lucIndex, ktfidf);
				
//				System.out.println("File "+ i+" - MAXCLIQUE");
//				s2.graphAlgorithm(graphFilename, cliqueFilename, cliquesResultsFilename, false, lucIndex, ktfidf);
//				
//				System.out.println("File "+ i+" - MAX WEIGHT CLIQUE");
//				s2.graphAlgorithm(graphFilename, cliqueFilename, cliquesResultsFilename, true, lucIndex, ktfidf);
				

				lucIndex.computeIdfs();
				s2.testRepTermsWithGraphs(graphFilename, cliqueFilename, cliquesResultsFilename, true, lucIndex, ktfidf);

				
				System.out.println("File "+ i+" - SUMSERIES");
				//System.out.println("Iteration:"+ i);
				
				
				
	
			
			//Stats.computeDocStats(s.getTestDocs());
			//System.out.println("done");
			
			//try {
				
				
				//1. Store the dataset into memory and an appropriate data structure
				//	s = new Simple(docsFilename, datasetType);
				//System.out.println("Simple created!");
							
				//2a. Compute the frequency sequences and store them on disk
				//s.computeFreqSequences(timeseriesFilename, false);
				
				//2b. Given the frequency sequences compute the bursty intervals
				
				//TODO: add scores to the bursty intervals
				
				//readBurstsFile(burstsFilename);
				
				//s.overlapsAlgorithm();
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		System.out.println(sum10/5);
		System.out.println(sum30/5);
		System.out.println(sum60/5);
		System.out.println(sum90/5);
		System.out.println(sum120/5);
//			catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}	
		
		//Stats.computeDocStats(s.getDocs());
		
//		Stats.computeBurstStats(s.getTerms());
//		
//		Stats.burstHistogram(s.getTerms());
	//}

}
