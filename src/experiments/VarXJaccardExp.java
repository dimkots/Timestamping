package experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

public class VarXJaccardExp {

	//kjetil: 20000101-20081130
	//topix: 2008/09/01 - 2008,8,1
	//call1: 1900/01/01 - 1900,0,1
	//call2: 1903/01/01 - 1903,0,1
	//call3: 1908/01/01 - 1908,0,1
	//slashdot: 19980102 - 1998,0,2
	//engadget: 20040301 - 2004,02,01 to 20111101 - 2011,10,01
	
	private static String DFtermsFile = "DF_TERMS.txt";
	
	
	/*
	 * 1: days, 7: weeks, 30: months, 365: years
	 */
	private static int granularity = 1;

	public static void main(String[] args) {

		try {			
			File path = new File(System.getProperty("luceneIndex"), "index");
			Directory dir = new SimpleFSDirectory(path);

			boolean newbursts = true;
			int datasetId = Integer.parseInt(args[0]);
			String testdocsFilename = null;
			String trainingdocsFilename = "trainingFile.txt";
			String burstsFilename = "bursts.txt";
			String datasetFile = null;
			Day startDay = null;
			String datasetName = null;
			String resultsFilename = null;
			int timespan = 0, docCount = 0, testDocs = 0;
			
			
			//call1: 110387, call2: 133684, call3: 129732:
			//call1-jaccard: 85914
			switch(datasetId){
				//SF-Call1
				case 1: 
					datasetFile = "call1_tok_nostop_norare.txt";
					startDay = new Day(1900,0,1);
					timespan = 730;
					docCount = 110387;
					testDocs = 1000;
					datasetName = "call1";
					resultsFilename = "call1_Results.txt";
					break;
				//SF-Call2
				case 2:
					datasetFile = "call2_tok_nostop_norare.txt";
					startDay = new Day(1903,0,1);
					timespan = 730;
					docCount = 133684;
					testDocs = 1000;
					datasetName = "call2_tok_nostop";
					resultsFilename = "call2_tok_nostopResults.txt";
					break;
				//SF-Call3
				case 3: 
					datasetFile = "call3_tok_nostop_norare.txt";
					startDay = new Day(1908,0,1);
					timespan = 730;
					docCount = 129732;
					testDocs = 1000;
					datasetName = "call3_tok_nostop";
					resultsFilename = "call3_tok_nostopResults.txt";
					break;
				//Topix
				case 4:
					//20080925
					datasetFile = "nostop_topix.txt";
					startDay = new Day(2008,8,25);
					timespan = 370;
					docCount = 65540;
					testDocs = 1000;
					datasetName = "nostop_topix";
					resultsFilename = "nostop_topixResults.txt";
					break;
				//Topix-Canada
				case 5:
					datasetFile = "nostop_india.txt";
					startDay = new Day(2008,8,25);
					timespan = 370;
					docCount = 2444;
					testDocs = 300;
					datasetName = "nostop_india";
					resultsFilename = "nostop_indiaResults.txt";
					break;
				//Slashdot
				case 6:
					datasetFile = "sp_slashdot.txt";
					startDay = new Day(1998,0,2);
					timespan = 3650;
					docCount = 60087;
					testDocs = 6000;
					datasetName = "sp_slashdot";
					resultsFilename = "sp_slashdotBurstyResults.txt";
					break;
					//Kjetil
				case 7:
					datasetFile = "kjetildata.txt";
					startDay = new Day(2000,0,1);
					timespan = 3650;
					docCount = 20028;
					datasetName = "kjetil";
					resultsFilename = "kjetildataVarXresults.txt";
					break;
				case 8:
					datasetFile = "engadget.txt";
					startDay = new Day(2004,02,01);
					timespan = 30000;
					docCount = 108589;
					testDocs = 1000;
					datasetName = "engadget";
					resultsFilename = "engadgetBurstyResults.txt";
					break;
				case 9:
					datasetFile = "/media/Data1/Datasets/NYT/1987.txt";
					startDay = new Day(1987,0,01);
					timespan = 370;
					docCount = 73280;
					testDocs = 7000;
					datasetName = "NYT1987";
					resultsFilename = datasetName+"BurstyResults.txt";
					break;
				case 10:
					datasetFile = "/media/Data1/Datasets/NYT/2004.txt";
					startDay = new Day(2004,0,01);
					timespan = 370;
					docCount = 73280;
					testDocs = 7000;
					datasetName = "NYT2004";
					resultsFilename = datasetName+"BurstyResults.txt";
					break;
				case 11:
					datasetFile = "/media/Data1/Datasets/NYT/NYT19871996.txt";
					startDay = new Day(1987,0,01);
					timespan = 4100;
					docCount = 665742;
					testDocs = 5000;
					datasetName = "NYT10";
					resultsFilename = datasetName+"BurstyResults.txt";
					break;
				default: 
					System.out.println("Id must be between 1 and 6 (inclusive)");
					System.exit(1);
					break;
			}
			
		
			testdocsFilename = datasetName+ "test.txt";
			trainingdocsFilename = datasetName+ "training.txt";
//			Preprocessor.createTestTrainingFiles(datasetFile, testdocsFilename, trainingdocsFilename, testDocs, docCount);
			
			System.out.println("Building final Lucene index...");
			dir = Index.buildLuceneIndex(datasetName, "luceneIndex", trainingdocsFilename, startDay, true, granularity);
			System.out.println("Built final Lucene index!");
			
			Index lucIndex2 = new Index(dir);

			BurstySimDater chk1 = new BurstySimDater();
			
			chk1.readDocsFile(trainingdocsFilename, startDay, granularity);
			System.out.println("Read dataset file!");


			if (newbursts){
				Preprocessor.computeFreqSequences(chk1.getDataset(), trainingdocsFilename+"TS", false);
				System.out.println("Computed freq sequences");	
//				BurstEngine.computeMAX2Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, true);			
				//BurstEngine.computeMAX1Bursts(trainingdocsFilename+"TS", burstsFilename, 0, 0, true,1);
				BurstEngine.computeMAXKBurstsSplit(trainingdocsFilename+"TS", burstsFilename, 0, 0, 150, true);
				//System.exit(1);
			}

			HashMap<String, HashMap<Integer, Double>> dataset = chk1.getDataset();
			chk1 = new BurstySimDater();
			chk1.readDocs(testdocsFilename, startDay, granularity);
			System.out.println("Have read testSet file");

			chk1.readBurstsFile(burstsFilename);
			System.out.println("Have read bursts file!");

			lucIndex2.computeIdfs();	
			
			int kJaccard = Integer.parseInt(args[1]);
			int ktfidf = 50;
			int kCliques = 1;

			int scoreType = BurstySimDater.NODECNTSCORE;
			boolean bursts = true;
			
			int x = 30;
			for (int i = 0; i<1; i++){

				chk1.jaccardAlgorithm(datasetName, resultsFilename, timespan, kJaccard, x, ktfidf, lucIndex2, scoreType, kCliques, bursts, dataset);
				if (x==7)
					x=15;
				else
					x=x*2;
			}
			
			System.out.println(datasetName + " Done!");
		} catch (IOException e) {

			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}


	}	
	



}
