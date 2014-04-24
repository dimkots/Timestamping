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

public class VarKJaccardExp {

	//kjetil: 20000101-20081130
	//topix: 2008/09/01 - 2008,8,1
	//call1: 1900/01/01 - 1900,0,1
	//call2: 1903/01/01 - 1903,0,1
	//call3: 1908/01/01 - 1908,0,1
	//slashdot: 19980102 - 1998,0,2
	//engadget: 20040301 - 2004,02,01 to 20111101 - 2011,10,01
	
	private static String DFtermsFile = "DF_TERMS.txt";

	public static void main(String[] args) {

		try {			
			File path = new File(System.getProperty("luceneIndex"), "index");
			Directory dir = new SimpleFSDirectory(path);
			boolean fromScratch = true;
			boolean otherTestset = false;
			boolean newbursts = true;
			int folds = 10;
			int datasetId = Integer.parseInt(args[0]);
			String testdocsFilename = null;
			String trainingdocsFilename = "trainingFile.txt";
			String burstsFilename = "bursts.txt";
			String datasetFile = null;
			Day startDay = null;
			String datasetName = null;
			String resultsFilename = null;
			int timespan = 0, docCount = 0; 
			//call1: 110387, call2: 133684, call3: 129732:
			//call1-jaccard: 85914
			switch(datasetId){
				//SF-Call1
				case 1: 
					datasetFile = "call1.txt";
					startDay = new Day(1900,0,1);
					timespan = 730;
					docCount = 110387;
					datasetName = "call1";
					resultsFilename = "call1results.txt";
					break;
				//SF-Call2
				case 2:
					datasetFile = "call2.txt";
					startDay = new Day(1903,0,1);
					timespan = 730;
					docCount = 133684;
					datasetName = "call2";
					resultsFilename = "call2results.txt";
					break;
				//SF-Call3
				case 3: 
					datasetFile = "call3.txt";
					startDay = new Day(1908,0,1);
					timespan = 730;
					docCount = 129732;
					datasetName = "call3";
					resultsFilename = "call3results.txt";
					break;
				//Topix
				case 4:
					datasetFile = "topix.txt";
					startDay = new Day(1900,0,1);
					timespan = 730;
					docCount = 65540;
					datasetName = "topix";
					resultsFilename = "topixresults.txt";
					break;
				//Topix-Canada
				case 5:
					datasetFile = "topixcanada.txt";
					startDay = new Day(1900,0,1);
					timespan = 730;
					docCount = 3326;
					datasetName = "topixcanada";
					resultsFilename = "topixcanadaresults.txt";
					break;
				//Slashdot
				case 6:
					datasetFile = "slashdot.txt";
					startDay = new Day(1998,0,2);
					timespan = 730;
					docCount = 60087;
					datasetName = "slashdot";
					resultsFilename = "slashdotresults.txt";
					break;
				//Kjetil
				case 7:
					datasetFile = "kjetildata.txt";
					startDay = new Day(2000,0,1);
					timespan = 3650;
					docCount = 20028;
					datasetName = "kjetildata";
					resultsFilename = "kjetildataresults.txt";
					break;
				default: 
					System.out.println("Id must be between 1 and 6 (inclusive)");
					System.exit(1);
					break;
			}
			
			String folderName = datasetName + "folds";
			//Delete all files from directory of specified dataset.
//			Preprocessor.deleteDir(folderName);
//			Preprocessor.deleteFile(resultsFilename);
			
//			Preprocessor.createCrossvalFiles(datasetFile, folderName, datasetName, folds);
			
			testdocsFilename = "testFile.txt";
			Preprocessor.createTestTrainingFiles(datasetFile, testdocsFilename, trainingdocsFilename, 1000, docCount);
			
			
			System.out.println("Building final Lucene index...");
			dir = Index.buildLuceneIndex(datasetName, "luceneIndex", trainingdocsFilename, startDay, true);
			System.out.println("Built final Lucene index!");
				
			Index lucIndex2 = new Index(dir);

			int kJaccard =  10;
			for (int i = 0; i<14;i++){
				
				System.out.println("Fold: "+i);
				int testNumber = i;
				//testdocsFilename = folderName+"\\"+datasetName+"f"+testNumber+".txt";
				//trainingdocsFilename = Preprocessor.mergefolds(folderName, datasetName, testNumber, folds);
				
				
				BurstySimDater chk1 = new BurstySimDater();
				
				chk1.readDocsFile(trainingdocsFilename, startDay);
				System.out.println("Read dataset file!");

				
				
				if (newbursts){
					Preprocessor.computeFreqSequences(chk1.getDataset(), trainingdocsFilename+"TS", false);
					System.out.println("Computed freq sequences");	
//					BurstEngine.computeMAX2Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, true);			
//					BurstEngine.computeMAX1Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, true,1);
					BurstEngine.computeMAXKBurstsSplit(trainingdocsFilename+"TS", burstsFilename, 0, 0, 150, true);
					//System.exit(1);
				}

				chk1 = new BurstySimDater();
				chk1.readDocs(testdocsFilename, startDay);
				System.out.println("Have read testSet file");

				chk1.readBurstsFile(burstsFilename);
				System.out.println("Have read bursts file!");

				lucIndex2.computeIdfs();	
				int ktfidf = 50;
				int kCliques = 40;
				int x = 30;
				
				int selector = BurstySimDater.TF;
				int scoreType = BurstySimDater.NODECNTSCORE;
				boolean bursts = true;
				chk1.jaccardAlgorithm(resultsFilename, timespan, selector, kJaccard, x, ktfidf, lucIndex2, scoreType, kCliques, bursts);
				if (kJaccard<100)
					kJaccard+=10;
				else
					kJaccard+=30;
				
			}
			
			
		} catch (IOException e) {

			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}


	}	
	



}
