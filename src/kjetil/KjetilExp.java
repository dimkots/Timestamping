package kjetil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import utilities.Constants;
import utilities.Day;
import utilities.Index;
import utilities.Preprocessor;
import utilities.Sorter;
import algorithms.BurstySimDater;
import algorithms.Simple;
import burstdetection.BurstEngine;

public class KjetilExp {

	// kjetil: 20000101-20081130
	// topix: 2008/09/01 - 2008,8,1
	// call1: 1900/01/01 - 1900,0,1
	// call2: 1903/01/01 - 1903,0,1
	// call3: 1908/01/01 - 1908,0,1
	// slashdot: 19980102 - 1998,0,2
	// engadget: 20040301 - 2004,02,01 to 20111101 - 2011,10,01



	private static String projectDirectory = "dejong/";
	// granularity
	public static final int W1 = 1;
	public static final int W2 = 2;
	public static final int W4 = 3;
	public static final int W12 = 4;
	public static final int W24 = 5;
	public static final int W52 = 6;
	
	public static int partitionName = W4;

	// public static final int M12 = 5;

	public static void main(String[] args) {

		try {
			
			
			
			
			int daysReporting = 30;
			switch(daysReporting){
			case 7:
				partitionName = W1;
				break;
			case 15:
				partitionName = W2;
				break;
			case 30:
				partitionName = W4;
				break;
			case 90:
				partitionName = W12;
				break;
			case 180:
				partitionName = W24;
				break;
			case 370:
				partitionName = W52;
				break;
				
			default:
				partitionName = W4;
			}
			

			long timeStart = System.currentTimeMillis();

			int datasetId = Integer.parseInt(args[0]);
			String testdocsFilename = null;
			String trainingdocsFilename = null;
			Day startDay = null;
			String datasetName = null;
			
			int timespan = 0;
			// call1: 110387, call2: 133684, call3: 129732:
			// call1-jaccard: 85914

			switch (datasetId) {
			// SF-Call1
			case 1:
				startDay = new Day(1900, 0, 1);
				timespan = 730;
				datasetName = "call1";
				break;
			// SF-Call2
			case 2:
				startDay = new Day(1903, 0, 1);
				timespan = 730;
				datasetName = "call2";
				break;
			// SF-Call3
			case 3:
				startDay = new Day(1908, 0, 1);
				timespan = 730;
				datasetName = "call3";
				break;
			// Topix
			case 4:
				// 20080925
				startDay = new Day(2008,8,25);
				timespan = 370;
				datasetName = "topixAll";
				break;
			// Topix-Canada
			case 5:
				startDay = new Day(2008,8,25);
				timespan = 370;
				datasetName = "topixCanada";
				break;
			case 6:
				
				startDay = new Day(2008,8,25);
				timespan = 370;
				
				datasetName = "topixIndia";
				
				break;
			case 7:
				
				startDay = new Day(2008,8,25);
				timespan = 370;
				
				datasetName = "topixAfrica";
				
				break;
			case 8:
				
				startDay = new Day(2008,8,25);
				timespan = 370;
				
				datasetName = "topixAustralia";
				
				break;
			case 9:
				//datasetFile = "/home/dkotzias/Desktop/1987.txt";
				startDay = new Day(1987,0,01);
				timespan = 370;
				//docCount = 73280;
				//testDocs = 7000;
				datasetName = "NYT1987";
				//resultsFilename = datasetName+"BurstyResults.txt";
				break;
			case 10:
				//datasetFile = "/home/dkotzias/Desktop/2004.txt";
				startDay = new Day(2004,0,01);
				timespan = 370;
				//docCount = 73280;
				//testDocs = 7000;
				datasetName = "NYT2004";
				//resultsFilename = datasetName+"BurstyResults.txt";
				break;
			case 11:
				//datasetFile = "/home/dkotzias/Desktop/NYT10.txt";
				startDay = new Day(1987,0,01);
				timespan = 3700;
				//docCount = 744539;
				//testDocs = 35000;
				datasetName = "NYT10_100";
				//resultsFilename = datasetName+"BurstyResults.txt";
				break;
			case 12:
				//datasetFile = "/home/dkotzias/Desktop/NYT10.txt";
				startDay = new Day(1995,0,01);
				timespan = 4100;
				//docCount = 744539;
				//testDocs = 35000;
				datasetName = "NYT10NVA";
				//resultsFilename = datasetName+"BurstyResults.txt";
				break;
			default:
				System.out.println("Id must be between 1 and 6 (inclusive)");
				System.exit(1);
				break;
			}

			testdocsFilename = datasetName + "test.txt";
			trainingdocsFilename = datasetName + "training.txt";


			String indexFolder = datasetName+ "Index";
			File path = new File(System.getProperty("luceneIndex"), indexFolder);
			Directory dir = new SimpleFSDirectory(path);
			Index lucIndex2 = new Index(dir);

			Baseline b = new Baseline(startDay);			
			String partition = getPartitionName(partitionName);
			b.partitionCorpus(trainingdocsFilename, projectDirectory + partition, daysReporting, timespan);

			lucIndex2.computeIdfs();
			b.processPartitions(projectDirectory + partition, startDay);
			
			b.readTestDocs(testdocsFilename);
		//	System.out.println("Have read test file");

			int selector = BurstySimDater.TFIDF;
			int ktfidf = 200;
			b.baselineAlgorithm(lucIndex2, ktfidf, selector,timespan/getPartitionDays(partitionName)+1, getPartitionDays(partitionName));
			
			long timeEnd = System.currentTimeMillis();
			
			double runningTime = (double)(timeEnd - timeStart)/1000;
			
			System.out.println("Total running time: " + runningTime);
			
		} catch (IOException e) {

			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

//		for (int i = 0; i < 1; i++) {
//			int firstPartition = 5;
//			try {
//				System.out.println(i);
//				// Preprocessor.createTestTrainingFiles(projectDirectory+newsBigfile,
//				// projectDirectory+testdocsFilename,
//				// projectDirectory+trainingdocsFilename, 1000, 65540);
//				// System.out.println("Have created training and test files!");
//				
//				//
//				// firstPartition =
//				// b.partitionCorpus(projectDirectory+trainingdocsFilename,
//				// projectDirectory+"M1", 1, M1, 2700);
//
//				// creates a file for each partition and writes all the
//				// documents in there
//				firstPartition = b.partitionCorpus(projectDirectory
//						+ trainingdocsFilename, projectDirectory + "W1", W1,
//						700);
//				// System.out.println("Partitioned corpus! First: "+
//				// firstPartition);
//
//				int create_index = 0;
//				Directory dir;
//				// creates the lucene index or just reads it
//				if (create_index == 1) {
//					System.out.println("Building first Lucene index...");
//					dir = Index.buildLuceneIndex("luceneIndex",
//							projectDirectory + trainingdocsFilename, firstDay,
//							false);
//					System.out.println("Built first Lucene index!");
//				} else {
//					File path = new File(System.getProperty("luceneIndex"),
//							"topixIndex");
//					dir = new SimpleFSDirectory(path);
//				}
//
//				Index lucIndex = new Index(dir);
//
//				String partition = "w1\\";
//				System.out.println("Processing " + partition + " partition!");
//
//				// ////
//				// //////////////////////////////////
//				// ///////////////////////////////////
//
//				b.processPartitions(projectDirectory + partition,
//						firstPartition);
//
//				// b.processDocuments(projectDirectory+partition,
//				// firstPartition);
//
//				int k = 20;
//
//				b.readTestDocs(projectDirectory + testdocsFilename);
//				lucIndex.computeIdfs();
//				// b.baselineAlgorithm(lucIndex, k, 11, 30, firstPartition);
//				// //months
//				b.baselineAlgorithm(lucIndex, k, 48, 7, firstPartition); // weeks
//				b.documentLevel(lucIndex, k, 48, 7, firstPartition, 10);
//				//
//				// System.out.println("Have read testDocs file!");
//
//				// int k = 20;
//				// b.baselineAlgorithm(lucIndex, k);

			

	}
	
	
	
	
	public static String getPartitionName(int partition){
		
		if(partition == W1 ) return "w1/";
		else if(partition == W2) return "w2/";
		else if(partition == W4) return "w4/";
		else if(partition == W12) return "w8/";
		else if(partition == W24) return "w24/";
		else if(partition == W52) return "w52/";
		
		return null;
	}
	
	public static int getPartitionDays(int partition){
		
		if(partition == W1 ) return 7;
		else if(partition == W2) return 15;
		else if(partition == W4) return 30;
		else if(partition == W12) return 90;
		else if(partition == W24) return 180;
		else if(partition == W52) return 370;
		
		return 0;
		
	}


}
