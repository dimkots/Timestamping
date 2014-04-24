package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import structures.BurstInterval;
import structures.TDocument;
import utilities.Day;
import utilities.Index;
import utilities.Interval;
import utilities.Preprocessor;
import algorithms.BurstySimDater;
import burstdetection.BurstEngine;

public class CombineExp {

	private static int granularity;	
	private String datasetFile;
	private Day startDay;
	private int timespan;
	private int docCount;
	private String datasetName;
	private String resultsFilename;
	private String testdocsFilename;
	private String trainingdocsFilename;
	private String burstsFilename;
	private Directory indexDir;
	
	
	private HashMap<String, HashMap<Integer, Double>> trainingSet;
	private ArrayList<TDocument> testDocs;
	
	
	private BurstySimDater burstySimDater;
	
	public CombineExp(int granularity) throws IOException {
		super();
		
		this.granularity = granularity;
		trainingSet = new HashMap<String, HashMap<Integer, Double>>();
		testDocs = new ArrayList<TDocument>();
		burstySimDater = new BurstySimDater();
	}
	
	
	
	
	

	/*
	 * This method adds the term 'term' that was 
	 * encountered on day 'day', 'freq' times.
	 * 
	 * Basically, if the term is already there, just increment
	 * the corresponding value. Otherwise, add the term with value = 1
	 */
	public void add(String term, int day, double freq){

		// If this is the first time we encounter a term
		if (!trainingSet.containsKey(term)){
			//Create a HashMap for this term
			HashMap<Integer,Double> termMap = new HashMap<Integer, Double>();
			termMap.put(day,freq);
			trainingSet.put(term, termMap);
		}
		// Else, just add the (day,freq) for this term
		else {
			HashMap<Integer,Double> termMap = trainingSet.get(term);

			// If it is the first time we encounter this term on this day
			// we just set the number of appearances to freq

			if (!termMap.containsKey(day))
				termMap.put(day,freq);
			// Just increment the number of appearances
			else {
				double previousFreq = termMap.get(day);
				termMap.put(day,previousFreq+freq);
			}
		}

	}
	
	
	

	public void updateDatasetInfo(int datasetId) {
		switch(datasetId){
		//SF-Call1
		case 1: 
			datasetFile = "/media/Data1/Datasets/Timestamping/NVA/call1rawAllNVA.txt";
//			datasetFile = "/media/Data1/Datasets/Timestamping/call1.txt";
			startDay = new Day(1900,0,1);
			timespan = 730;
			docCount = 117316;
			datasetName = "call1";
			resultsFilename = "call1_Results.txt";
			break;
		//SF-Call2
		case 2:
			datasetFile = "/media/Data1/Datasets/Timestamping/call2.txt";
			startDay = new Day(1903,0,1);
			timespan = 730;
			docCount = 82128;
			datasetName = "call2";
			resultsFilename = "call2_tok_nostopResults.txt";
			break;
		//SF-Call3
		case 3: 
			datasetFile = "/media/Data1/Datasets/Timestamping/call3.txt";
			startDay = new Day(1908,0,1);
			timespan = 730;
			docCount = 88694;
			datasetName = "call3";
			resultsFilename = "call3_tok_nostopResults.txt";
			break;
		//Topix
		case 4:
			//20080925
			datasetFile = "/media/Data1/Datasets/Timestamping/topixAll.txt";
			startDay = new Day(2008,8,25);
			timespan = 370;
			docCount = 60451;
			datasetName = "topixAll";
			resultsFilename = "topixAllResults.txt";
			break;
		//Topix-Canada
		case 5:
			datasetFile = "/media/Data1/Datasets/Timestamping/topixCanada.txt";
			startDay = new Day(2008,8,25);
			timespan = 370;
			docCount = 3152;
			datasetName = "topixCanada";
			resultsFilename = "topixCanadaResults.txt";
			break;
		case 6:
			datasetFile = "/media/Data1/Datasets/topix/topixIndia.txt";
			startDay = new Day(2008,8,25);
			timespan = 370;
			docCount = 2596;
			datasetName = "topixIndia";
			resultsFilename = "indiaResults.txt";
			break;
		case 7:
			datasetFile = "/media/Data1/Datasets/topix/topixSouthAfrica.txt";
			startDay = new Day(2008,8,25);
			timespan = 370;
			docCount = 2389;
			datasetName = "topixAfrica";
			resultsFilename = "southAfricaResults.txt";
			break;
		case 8:
			datasetFile = "/media/Data1/Datasets/topix/topixAustralia.txt";
			startDay = new Day(2008,8,25);
			timespan = 370;
			docCount = 1351;
			datasetName = "topixAustralia";
			resultsFilename = "australiaResults.txt";
			break;
		case 9:
			datasetFile = "/media/Data1/Datasets/NYT/1987.txt";
			startDay = new Day(1987,0,01);
			timespan = 370;
			docCount = 73280;
			datasetName = "NYT1987";
			resultsFilename = datasetName+"BurstyResults.txt";
			break;
		case 10:
			datasetFile = "/media/Data1/Datasets/NYT/2004.txt";
			startDay = new Day(2004,0,01);
			timespan = 370;
			docCount = 73280;
			datasetName = "NYT2004";
			resultsFilename = datasetName+"BurstyResults.txt";
			break;
		case 11:
//			datasetFile = "/media/Data1/Datasets/NYT/19871996NVA.txt";
//			datasetFile = "19871996_60.txt";
			startDay = new Day(1987,0,1);
			timespan = 3700;
//			docCount = 66574; //10
//			docCount = 133148; //20
//			docCount = 199722; //30
//			docCount = 266296; //40
//			docCount = 332870; //50
//			docCount = 399444; //60
//			docCount = 532592; //80
			docCount = 665742; //100
			datasetName = "NYT10_100";
			resultsFilename = datasetName+"BurstyResults.txt";
			break;
		case 12:
//			datasetFile = "/media/Data1/Datasets/NYT/19871996NVA.txt";
			datasetFile = "SFCallNVA.txt";
			startDay = new Day(1900,0,1);
			timespan = 3700;
			docCount = 399721;
			datasetName = "CALL";
			resultsFilename = datasetName+"BurstyResults.txt";
			break;
		default: 
			System.out.println("Id must be between 1 and 6 (inclusive)");
			System.exit(1);
			break;
		}
		
		testdocsFilename = datasetName+ "test.txt";
		trainingdocsFilename = datasetName+ "training.txt";
		burstsFilename = datasetName + "bursts.txt";
	}

	public void createTestTrainingFiles(double trainingSetPerc) {
		System.out.print("Creating test and training files...");
		Preprocessor.createTestTrainingFiles(datasetFile, testdocsFilename, trainingdocsFilename, trainingSetPerc, docCount);
		System.out.println("Finished!");
	}

	public void buildIndex() throws IOException {
		System.out.print("Building Lucene index...");
		indexDir = Index.buildLuceneIndex(datasetName, "luceneIndex", trainingdocsFilename, startDay, granularity, this);
		System.out.println("Finished!");
	}
	
	
	public void readTrainingSet() throws IOException{
		System.out.print("Reading training set...");
		File docsFile = new File(trainingdocsFilename);

		try {
			Reader rin = new InputStreamReader(new FileInputStream(docsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line;
			int count = 0;
			TDocument tDoc;
			while ((line = in.readLine()) != null){

//				System.out.println(count++);
				tDoc = new TDocument(line, startDay, granularity);
				int timestamp = tDoc.getTimestamp();

				String[] terms = tDoc.getBodyTerms();
				HashMap<String, Integer> map = tDoc.getBody();
				//For each term
				for (String term : terms){
					int freq = map.get(term);
					this.add(term, timestamp, freq);
				}
			}

			in.close();
			rin.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		String indexFolder = datasetName+ "Index";
		File path = new File(System.getProperty("luceneIndex"), indexFolder);
		indexDir = new SimpleFSDirectory(path);
		
		System.out.println("Finished!");
	}




	public void readTestingSet() {
		System.out.print("Reading testing set...");
		File docsFile = new File(testdocsFilename);

		try {
			Reader rin = new InputStreamReader(new FileInputStream(docsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line;
			TDocument tDoc;
			while ((line = in.readLine()) != null){
				tDoc = new TDocument(line, startDay, granularity);
				this.testDocs.add(tDoc);	    		

			}

			in.close();
			rin.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Finished!");
	}
		
	



	public void experiment(int kJaccard, int ktfidf, int cliques, int x ) throws Exception {
		
		System.out.println("Running timestamping experiment...");
		
		Index index = new Index(indexDir);
		
		System.out.print("Computing idfs...");
		index.computeIdfs();
		System.out.println("Finished!");
		
		BurstySimDater burstySimDater = new BurstySimDater();
		
		burstySimDater.setFilenames("graphs.txt", "cliques.txt");
		burstySimDater.computeBursts(trainingdocsFilename, burstsFilename, trainingSet);
		burstySimDater.readBurstsFile(burstsFilename);

		//For each testing document
		int j = 0;
		int hits = 0;
		int sumLength = 0;
		while (j<this.testDocs.size()){
			
			if(j%10==0)
				System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;

			int actualTimestamp = tDoc.getTimestamp();
			
			Interval bPredictedInterval = burstySimDater.predictInterval(tDoc, timespan, kJaccard, ktfidf, index, cliques, x, trainingSet);
			
			if (bPredictedInterval==null)
				continue;
			sumLength+= bPredictedInterval.getLength();
			
//			System.out.print("\n"+actualTimestamp + " ("+bPredictedInterval.getStart() + "," + bPredictedInterval.getEnd()+") ");
			
			if (bPredictedInterval.contains(actualTimestamp)){
//				System.out.print(" HIT");
				hits++;
			}
			
				
			
		}
		//burstySimDater.jaccardAlgorithm(datasetName, resultsFilename, timespan, kJaccard, x, ktfidf, index, BurstySimDater.NODECNTSCORE, cliques, trainingSet);
		
		double predictionInterval = (double)sumLength/this.testDocs.size();
		System.out.println("Hits: "+ hits);
		double precision = (double)hits/this.testDocs.size();
		System.out.println("Precision: " + precision);
		System.out.println("Prediction interval: "+ predictionInterval);
		System.out.println("End of experiment!");
		
	}	
	
	
}

