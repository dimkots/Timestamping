package experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import structures.TDocument;
import utilities.Day;
import utilities.Index;

public class MaxEntExp {

	private Day startDay;
	private int timespan;
	//private int docCount;
	private String datasetName;
	private String testdocsFilename;
	private String trainingdocsFilename;
	private Directory indexDir;
	private int granularity;
	private String maxEntTestFilename;
	private String maxEntTrainingFilename;
	
	public void updateDatasetInfo(int datasetId) {
		switch(datasetId){
		//SF-Call1
		case 1: 
			startDay = new Day(1900,0,1);
			timespan = 730;
			datasetName = "call1";
			break;
		//SF-Call2
		case 2:
			startDay = new Day(1903,0,1);
			timespan = 730;
			datasetName = "call2";
			break;
		//SF-Call3
		case 3: 
			startDay = new Day(1908,0,1);
			timespan = 730;
			datasetName = "call3";
			break;
		//Topix
		case 4:
			//20080925
			startDay = new Day(2008,8,25);
			timespan = 370;
			datasetName = "topixAll";
			break;
		//Topix-Canada
		case 5:
			startDay = new Day(2008,8,25);
			timespan = 370;
			datasetName = "topixCanada";
			break;
		//Slashdot
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
			startDay = new Day(1987,0,01);
			timespan = 370;
			datasetName = "NYT1987";
			break;
		case 10:
			startDay = new Day(2004,0,01);
			timespan = 370;
			datasetName = "NYT2004";
			break;
		case 11:
			startDay = new Day(1987,0,1);
			timespan = 3700;
			datasetName = "NYT10_60";
			break;
		case 12:
			startDay = new Day(1987,0,1);
			timespan = 370;
			datasetName = "1987NVA";
			break;
		case 13:
			startDay = new Day(1900,0,1);
			timespan = 3700;
			datasetName = "CALL";
			break;
		default: 
			System.exit(1);
			break;
		}
		
		testdocsFilename = datasetName+ "test.txt";
		trainingdocsFilename = datasetName+ "training.txt";
		maxEntTestFilename = "maxEnt90" + testdocsFilename;
		maxEntTrainingFilename = "maxEnt90" + trainingdocsFilename;
	}
	
	
	


	public void convertDataset(int ktfidf, int granularity) throws IOException {

		//First convert training set file
		Reader rin = new InputStreamReader(new FileInputStream(trainingdocsFilename), "UTF-8");
		BufferedReader in = new BufferedReader( rin );

		File maxEntTrainingFile = new File(maxEntTrainingFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(maxEntTrainingFile, false),"UTF-8" ));
		
		
//		String indexFolder = datasetName+ "Index";
//		File path = new File(System.getProperty("luceneIndex"), indexFolder);
//		indexDir = new SimpleFSDirectory(path);
//		Index index = new Index(indexDir);
		
		String line = null;
		String[] docTerms;
		String terms;
		TDocument tDoc; 
		int count = 0;
		int exceptionsTraining=0;
		
		int[] trainingHistogram = new int[122];
		while ((line = in.readLine()) != null) {

			count++;
			tDoc = new TDocument(line, startDay, granularity);
			int docTimestamp = tDoc.getTimestamp();
			int docClass = docTimestamp/granularity;
			
			if (docClass<0 || docClass>122 ){
				System.out.println(count + " " + tDoc.getDateStr() + " " + docClass );
				exceptionsTraining++;
				continue;
			}
			
			trainingHistogram[docClass]++;
			//docTerms = tDoc.getTopKTerms(ktfidf, index, null);
			docTerms = tDoc.getBodyTerms();
			terms = "";
			for (String docTerm : docTerms)
				terms += docTerm +" ";
			
			out.write(docClass + "\t" + terms+"\n");	
			
//			if (count>1000)
//				break;
		}

		out.close();
		in.close();	
		
		
		//Convert testing set file
		rin = new InputStreamReader(new FileInputStream(testdocsFilename), "UTF-8");
		in = new BufferedReader( rin );

		File maxEntTestingFile = new File(maxEntTestFilename);
		out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(maxEntTestingFile, false),"UTF-8" ));
		
		count=0;
		int exceptionsTesting = 0;
		int[] testingHistogram = new int[122];
		while ((line = in.readLine()) != null) {

			count++;
			tDoc = new TDocument(line, startDay, granularity);
			int docTimestamp = tDoc.getTimestamp();
			int docClass = docTimestamp/granularity;
			
			if (docClass<0 || docClass>122 ){
				System.out.println(count + " " + tDoc.getDateStr() + " " + docClass );
				exceptionsTesting++;
				continue;
			}
			
			testingHistogram[docClass]++;
			
//			docTerms = tDoc.getTopKTerms(ktfidf, index, null);
			docTerms = tDoc.getBodyTerms();
			
			terms = "";
			for (String docTerm : docTerms)
				terms += docTerm +" ";
			
			out.write(docClass + "\t" + terms+"\n");	
			
//			if (count>100)
//				break;
		}

		out.close();
		in.close();	
		
		System.out.println("Exceptions training:" + exceptionsTraining);
		System.out.println("Exceptions testing:" + exceptionsTesting);
		System.out.println("Trainining histogram:");
		
		for (int i : trainingHistogram)
			System.out.println(i);
		
		System.out.println("Testing histogram:" + testingHistogram.toString());
		for (int i : testingHistogram)
			System.out.println(i);
		
	}





	public void computePrecision(String filename) throws NumberFormatException, IOException {
		
		Reader rin = new InputStreamReader(new FileInputStream(filename), "UTF-8");
		BufferedReader in = new BufferedReader( rin );

		String line;
		int count = 0;
		int hits = 0;
		while ((line = in.readLine()) != null) {
			count++;
			
			String[] lineElements = line.split("\t");
			
			int actual = Integer.parseInt(lineElements[0]);
			int	predicted = Integer.parseInt(lineElements[1]);
			
			if (actual==predicted)
				hits++;
		}
		
		double precision = (double)hits/count;
		System.out.println("Precision: " + precision );
		in.close();
	}

}
