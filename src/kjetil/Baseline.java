package kjetil;

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
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import structures.TDocument;
import utilities.Constants;
import utilities.Day;
import utilities.Index;
import utilities.Preprocessor;
import utilities.Sorter;
import algorithms.Simple;
import burstdetection.BurstEngine;

/**
 * @author Jim
 *
 */
public class Baseline {

	private ArrayList<TDocument> testDocs;	

	private HashMap<String, HashMap<Integer, Double>> dataset;
	private HashMap<String, HashMap<Integer,Integer>> tokens;
	private HashMap<String, Integer> corpusTokens;
	public HashMap<Integer, Integer> partLength;
	private HashMap<Integer, ArrayList<TDocument>> corpusDocuments;
	private Day firstDay;
	
	public Baseline(Day firstDay) throws IOException {
		super();
		dataset = new HashMap<String, HashMap<Integer, Double>>();
		tokens = new HashMap<String, HashMap<Integer, Integer>>();
		corpusTokens = new HashMap<String, Integer>();
		partLength = new HashMap<Integer, Integer>();
		corpusDocuments = new HashMap<Integer, ArrayList<TDocument>>();
		this.firstDay = firstDay;
	}
		
	private void readDocsFile(String docsFilename){
		
		File docsFile = new File(docsFilename);

		try {
			Reader rin = new InputStreamReader(new FileInputStream(docsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
	        String line;

	        while ((line = in.readLine()) != null){

	        	TDocument tDoc = new TDocument(line, firstDay, true, true);
	        	
	        	int timestamp = tDoc.getTimestamp();
	        	HashMap<String, Integer> body = tDoc.getBody();
        		String[] bodyTerms = tDoc.getBodyTerms();
	        	
        		for (String term : bodyTerms){

    				int freq = body.get(term);
    				this.add(term, timestamp, freq);    			
    			}
	        }
	        
	        in.close();
	        rin.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
	}
	
	
	
	
	
	/*
	 * This method adds the term 'term' that was 
	 * encountered on day 'day', 'freq' times.
	 * 
	 * Basically, if the term is already there, just increment
	 * the corresponding value. Otherwise, add the term with value = 1
	 */
	private void add(String term, int day, double freq){
		
		// If this is the first time we encounter a term
		if (!dataset.containsKey(term)){
			//Create a HashMap for this term
			HashMap<Integer,Double> termMap = new HashMap<Integer, Double>();
			termMap.put(day,freq);
			dataset.put(term, termMap);
		}
		// Else, just add the (day,freq) for this term
		else {
			HashMap<Integer,Double> termMap = dataset.get(term);
			
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
	
	private Day getStartDay(int intervalIndex, int partDays){
		Day day = new Day();
		return day;
	}
	
	public void baselineAlgorithm(Index index, int ktfidf, int termSelector, int partCnt, int partDays) throws IOException{
		String [] tDocTerms = null;

		//Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();
				
		int corpusLength = 0;
		
		
		//get the corpus length
		for (int i=0;i<partCnt;i++){
			Integer length = this.partLength.get(i);
			
			if (length!=null)
				corpusLength+= length; 
		}
		

		int[] hit= new int[6];
		
		File hitsFile = new File("kjetilHits.txt");
		BufferedWriter hitsOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hitsFile, false),"UTF-8" ));

		
		int[] hist = new int[partCnt+1];
	    while (j<iterations){

		    TDocument tDoc = this.testDocs.get(j);
		    j++;
		    
		    int actualTimestamp =  tDoc.getTimestamp()/partDays; 
		    System.out.println("Actual timestamp: " + actualTimestamp);
		    
		    double[] score = new double[partCnt]; 
		    
		    //Get the bursty intervals for the terms of the doc
			//tDocTerms = tDoc.getRepresentativeTerms();
		    
		   // tDocTerms = tDoc.getBodyTerms();
			tDocTerms = tDoc.getTopKTerms(ktfidf, index, null);

			int docLength = tDoc.getDocLength();
			
			
			HashMap<String, Integer> tDocBody = tDoc.getBody();
			int scoreIndex = 0;
			
			
			//for each partition
			for (int i=0;i<partCnt;i++){
			
				System.out.println("partition: " +i);
				int partLength = this.partLength.get(i);
				
				
				//for each word
				for (String term : tDocTerms){
					
					double tf = tDocBody.get(term);
					double pwd = tf/docLength;
					
					Integer tfInP = null;
					
					//If term does not exist in the corpus
					if (tokens.get(term)==null){
						tfInP = 0;
					}
					else{
						tfInP = tokens.get(term).get(i);	
					}
						
					
					double pwC; 
					//If term does not exist in the corpus
					if (corpusTokens.get(term)==null){
						pwC = tf/corpusLength;
					}
					else{
						int totalTf = corpusTokens.get(term);
						pwC = (double)totalTf / corpusLength;
					}
					
					
					double pwp; 
					
					//Dirichlet smooth it
//					if (tfInP==null || tfInP==0){
//						//month: 10, 50
//						double m = Math.pow(10, 1);
//						double nom = m*pwC;
//						double denom = partLength+m; 
//						
//						pwp = nom/denom;
//					}
//					else
//						pwp = (double)tfInP / partLength;

					//linearInterpolation
					
					
					
					//?? interpolation i assume
					double l = 0.1;
					if (tfInP==null || tfInP==0){
						pwp = l*pwC;	
					}
					else {
						pwp = (double)tfInP / partLength;
						pwp = (1-l)*pwp + l*pwC;
					}
					
					double nllr =  pwd * Math.log10(pwp/pwC);
					//nllrValue = pTermInDoc* Math.log10((((1.0-lambda)*pTermInCorpusPar+lambda*pTermInGlobal)/pTermInGlobal));
					score[scoreIndex]+= pwd * Math.log10(pwp/pwC);
					
					
				}
				
				scoreIndex++;
			}
			
//			for (int i =0;i<score.length;i++){
//				System.out.println(i + "\t" + score[i]);
			
//			}
			int[] indices = new int[score.length];
			for (int i=0;i<indices.length;i++)
				indices[i]=i;
			
			Sorter.quicksort(score, indices);
			
			//scoreIndex 0 -> firstPartition
			//scoreIndex i -> firstPartition+i
			
//			System.out.println("Estimated partitions:" + (indices[indices.length-1]) + " , " + (indices[indices.length-2]) 
//					+ " , " + (indices[indices.length-3])  );
//			

			int predictedInterval = getNLLRInterval(tDoc, corpusLength, partCnt, ktfidf, index);
			
			if (predictedInterval == actualTimestamp)
				hit[0]++;
		
		}
	    
	    
	    for (int i=0;i<6;i++){
	    	System.out.println("Hit "+i+":\t" + hit[i]);
	    }
	    
	    
	    System.out.println();
	    int sum = 0;
	    for (int i=0;i<6;i++){
	    	sum+= hit[i];
	    	System.out.println("Hit "+i+":\t" + (double)sum/iterations);
	    }
	 
	    hitsOut.close();
	    System.out.println("\n\n");
//	    for (int i=0;i<=partCnt;i++){
//	    	System.out.println(i+"s:"+ hist[i-1]);
//	    }

	   
	}


	
	public int getNLLRInterval(TDocument tDoc, int corpusLength, int partCnt, int ktfidf, Index index ) throws IOException{
		
		double[] score = new double[partCnt]; 
		
		String[] tDocTerms = tDoc.getTopKTerms(ktfidf, index, null);
		
		
		int docLength = tDoc.getDocLength();
		
		
		HashMap<String, Integer> tDocBody = tDoc.getBody();
		int scoreIndex = 0;
		
		
		//for each partition
		for (int i=0;i<partCnt;i++){
		
			System.out.println("partition: " +i);
			int partLength = this.partLength.get(i);
			
			
			//for each word
			for (String term : tDocTerms){
				
				double tf = tDocBody.get(term);
				double pwd = tf/docLength;
				
				Integer tfInP = null;
				
				//If term does not exist in the corpus
				if (tokens.get(term)==null){
					tfInP = 0;
				}
				else{
					tfInP = tokens.get(term).get(i);	
				}
					
				
				double pwC; 
				
				//If term does not exist in the corpus
				if (corpusTokens.get(term)==null){
					pwC = tf/corpusLength;
				}
				else{
					int totalTf = corpusTokens.get(term);
					pwC = (double)totalTf / corpusLength;
				}
				
				
				double pwp; 
				double l = 0.1;
				if (tfInP==null || tfInP==0){
					pwp = l*pwC;	
				}
				else {
					pwp = (double)tfInP / partLength;
					pwp = (1-l)*pwp + l*pwC;
				}
				
				double nllr =  pwd * Math.log10(pwp/pwC);
				//nllrValue = pTermInDoc* Math.log10((((1.0-lambda)*pTermInCorpusPar+lambda*pTermInGlobal)/pTermInGlobal));
				score[scoreIndex]+= pwd * Math.log10(pwp/pwC);
				
				
			}
			
			scoreIndex++;
		}
		
		int[] indices = new int[score.length];
		for (int i=0;i<indices.length;i++)
			indices[i]=i;
		
		Sorter.quicksort(score, indices);
		
		return indices[indices.length-1];
	}

	
	// weeks: 5 to 47
	public void documentLevel(Index index, int ktfidf, int partCnt, int partDays, int firstPartition, int n) throws IOException{

		int j = 0;
		int iterations = this.testDocs.size();
				
		int corpusLength = 0;
		
		for (int i=firstPartition;i<partCnt;i++)
			corpusLength+= this.partLength.get(i);
		
		int hit1=0, hit2=0, hit3=0, hit4=0, hit5=0, hit6=0, hit7=0, hit8=0, hit9=0, hit10=0;
		
		int[] hist = new int[partCnt-firstPartition];
		
		//Compute how many training documents are there.
		int corpusSize = 0;
		int[] docsPerPart = new int[partCnt-firstPartition];
		for (int i=firstPartition;i<partCnt;i++){
			docsPerPart[i-firstPartition] = this.corpusDocuments.get(i).size();
			corpusSize += docsPerPart[i-firstPartition];
		}
			
		//For each testing document
	    while (j<iterations){

	    	double[] docScores = new double[corpusSize];
	    	int[] partitions = new int[corpusSize];
	    	
		    TDocument X = this.testDocs.get(j);
		    j++;
		    
		    int actualTimestamp =  X.getTimestamp()/partDays; 
		    System.out.println("Actual timestamp: " + actualTimestamp);
		    
		    int docCnt = 0;
		    for (int i=firstPartition;i<partCnt;i++){
				
		    	ArrayList<TDocument> docList = this.corpusDocuments.get(i); 
		    	
		    	for (TDocument Dj : docList){
		    		
		    		double tempScore = 0;;
		    		
		    		String[] Xwords = X.getBodyTerms();
		    		
		    		for (String w : Xwords){
		    			double PwX = (double)X.getBody().get(w) / X.getDocLength();
						double PwC = (double)corpusTokens.get(w) / corpusLength;
		    			
						double PwDj = 0.0;
		    			
		    			Integer tfInDj = Dj.getBody().get(w);
		    			
		    			//If not exists, linear interpolation
		    			double l = 0.5;
		    			if (tfInDj==null || tfInDj==0){
		    				PwDj = (1-l)*PwC;	
						}
						else {
							PwDj = tfInDj.doubleValue() / X.getDocLength();
							PwDj = l*PwDj + (1-l)*PwC;
						}
		    			
		    			tempScore+= PwX * Math.log10(PwDj/PwC);
		    			
		    		}//end for String w
		    		
		    		docScores[docCnt] = tempScore;
		    		partitions[docCnt++] = i;
		    		
		    	} //end for Dj
				
			}//end for partition 

			
			Sorter.quicksort(docScores, partitions);
			
			double[] partScores = new double[partCnt-firstPartition];
			
			for (int i=0;i<n;i++){
				int partIndex = partitions[partitions.length-1-i]-firstPartition;
				partScores[partIndex]+= docScores[docScores.length-1-i];
			}
			
			int[] indices = new int[partScores.length];
			for (int i=0;i<indices.length;i++)
				indices[i]=i;
			Sorter.quicksort(partScores, indices);
			
			System.out.println("Estimation:" + (indices[indices.length-1]+firstPartition));
			
			if (indices[indices.length-1]+firstPartition==actualTimestamp)
				hit1++;
			
			//scoreIndex 0 -> firstPartition
			//scoreIndex i -> firstPartition+i
			
			
			

		}
	    
	    System.out.println("Hit1:" + hit1);
//	    System.out.println("Hit2:" + hit2);
//	    System.out.println("Hit3:" + hit3);
//	    System.out.println("Hit4:" + hit4);
//	    System.out.println("Hit5:" + hit5);
//	    System.out.println("Hit6:" + hit6);
//	    System.out.println("Hit7:" + hit7);
//	    System.out.println("Hit8:" + hit8);
//	    System.out.println("Hit9:" + hit9);
//	    System.out.println("Hit10:" + hit10);
	 
	    
	    System.out.println("\n");
	    for (int i=firstPartition;i<=partCnt;i++){
	    	System.out.println(i+"s:"+ hist[i-1]);
	    }

	   
	}

	
	
	/* 	Reads a file with test documents, where each line corresponds to a document */
	public void readTestDocs(String testdocsFilename) {
		
		this.testDocs = new ArrayList<TDocument>();
		File docsFile = new File(testdocsFilename);

		try {
			Reader rin = new InputStreamReader(new FileInputStream(docsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
	        String line;

	        while ((line = in.readLine()) != null){
	        	TDocument tDoc = new TDocument(line, firstDay,1);//, true, true);
    			this.testDocs.add(tDoc);
	        }
	        
	        in.close();
	        rin.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
    //training file
	public int partitionCorpus(String datafilename, String directory, int granularity, int timespan) throws IOException{
		
		int partDays;
		int firstPartition = Integer.MAX_VALUE;
		
		
		partDays = granularity;
		
		
		File dir = new File(directory);
		
		if (!dir.exists()){
			dir.mkdirs();
		}
		else{
			this.delete(dir);
			dir.mkdirs();
		}
		
		File datafile = new File(datafilename);
		int invalid = 0;
		try {
			Reader rin = new InputStreamReader(new FileInputStream(datafile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
	        String line;

	     
	        
	        int partCnt = timespan/partDays+1;
	        
	        int[] partDocs = new int[partCnt];
	        
	        BufferedWriter[] outs = new BufferedWriter[partCnt];
	        for (int i = 0; i<partCnt; i++){
	        	File partitionFile = new File(directory+"p"+ i+".txt");
	        	System.out.println("p"+i);
				outs[i] = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(partitionFile, false),"UTF-8" ));
				
	        }

	        while ((line = in.readLine()) != null){
	
	        	//TDocument tDoc = new TDocument(line, firstDay, true, true);
	        	//dkotzias
	        	TDocument tDoc = new TDocument(line, firstDay, 1);
	        	int timestamp = tDoc.getTimestamp();
	        	
	        	//System.out.println("timestamp: " + timestamp +"\t" + line);
	        	if (timestamp>4100){
	        		invalid++;
	        		continue;
	        	}
	        	
	        	int partition = timestamp/partDays;
    			
	        	if (partition < firstPartition)
	        		firstPartition = partition;
	        	
	        	
	        	outs[partition].write(line + "\n"); 
	    		partDocs[partition]++;
	    		
	        }
	        
	        in.close();
	        rin.close();
	        System.out.println(invalid);
	        for (int i = 0; i<partCnt; i++)
				outs[i].close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return firstPartition;
	}

	void delete(File f) throws IOException {
		  if (f.isDirectory()) {
		    for (File c : f.listFiles())
		      delete(c);
		  }
		  if (!f.delete())
		    throw new FileNotFoundException("Failed to delete file: " + f);
		}


	public void processDocuments(String directory, int firstPartition) {
		
		File dir = new File(directory);
		String[] children = dir.list();
		
		if (children == null)
		    System.exit(-1);

		try {
			
			
			
			//read all partition files
			for (int i=0; i<children.length; i++) {
				
				int length = 0; 
				
				
				
				String filename = directory+children[i];
			
				//Get partition number. Children[i] = "p10.txt"
				int dotIndex = children[i].indexOf(".");
				String subs = children[i].substring(1, dotIndex);
				int partNumber = Integer.parseInt(subs);
				
				
				
				if (partNumber<firstPartition)
					continue;
				
				
				ArrayList<TDocument> docList = new ArrayList<TDocument>();
				
				//partNumber ranges from 5 to 47 when week
				
				//partNumber ranges from 1 to 11 when month.
				
				Reader rin = new InputStreamReader(new FileInputStream(filename), "UTF-8");
				BufferedReader in = new BufferedReader( rin );
				
				String line;

				
				
		        while ((line = in.readLine()) != null){
		        
		        	TDocument tDoc = null;
		        	
//		        	if (dataType==1)
//		        		tDoc = new NewsDocument(line);
//		        	else if (dataType==2)
//		        		tDoc = new CallDocument(line, 0);
		        	
//		        	docList.add((NewsDocument)tDoc);
		        	
		        	String[] docTerms = tDoc.getBodyTerms();
		        	HashMap<String, Integer> docBody = tDoc.getBody();
		        	
		        	for (String term : docTerms){
	
		        		if (term.equals("")) continue;
		    			
		        		int tf = docBody.get(term);
		        		length+= tf;
		        		
		    			HashMap<Integer, Integer> termMap = tokens.get(term);
		    			if (termMap == null){
		    				termMap = new HashMap<Integer,Integer>();
		    				
		    				termMap.put(partNumber, tf);
		    				tokens.put(term, termMap);
		    			}
		    			else{   				
		    				Integer mtf = termMap.get(partNumber);
		    				if (mtf == null) termMap.put(partNumber, tf);
		    				else termMap.put(partNumber, mtf + tf);
		    			}
		    			
		    			
		    			Integer Ctf = corpusTokens.get(term);
	    				if (Ctf == null) corpusTokens.put(term, tf);
	    				else corpusTokens.put(term, Ctf + tf);
		    		
		        	}//endFor terms
		        }//endWhile line
		        
		        corpusDocuments.put(partNumber, docList);
		        this.partLength.put(partNumber, length);
		       
			}//endFor children
		}//endTry
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
	}
	
	
public void processPartitions(String directory, Day startDay) {
		
		File dir = new File(directory);
		String[] children = dir.list();
		
		if (children == null)
		    System.exit(-1);

		try {
			
			
		
			
			
			//read all partition files
			//for each partition
			for (int i=0; i<children.length; i++) {
				
				int length = 0; 
				
				System.out.println("Partition " + i +" ");
				String filename = directory+children[i];
			
				//Get partition number. Children[i] = "p10.txt"
				int dotIndex = children[i].indexOf(".");
				String subs = children[i].substring(1, dotIndex);
				int partNumber = Integer.parseInt(subs);
	
				
				//partNumber ranges from 1 to 11 when month.
				
				Reader rin = new InputStreamReader(new FileInputStream(filename), "UTF-8");
				BufferedReader in = new BufferedReader( rin );
				
				String line;

				
				//for each document in that partition
		        while ((line = in.readLine()) != null){
		        
		        	TDocument tDoc = new TDocument(line, startDay, 1);//, true, true);
		        	
		        	String[] docTerms = tDoc.getBodyTerms();
		        	HashMap<String, Integer> docBody = tDoc.getBody();
		        	
		        	//for each word
		        	for (String term : docTerms){
	
		        		if (term.equals("")) continue;
		    			
		        		int tf = docBody.get(term);
		        		
		        		///update the partition length
		        		length+= tf;
		        		
		        		
			        		///????????
			    			HashMap<Integer, Integer> termMap = tokens.get(term);
			    			if (termMap == null){
			    				termMap = new HashMap<Integer,Integer>();
			    				
			    				termMap.put(partNumber, tf);
			    				tokens.put(term, termMap);
			    			}
			    			else{   				
			    				Integer mtf = termMap.get(partNumber);
			    				if (mtf == null) termMap.put(partNumber, tf);
			    				else termMap.put(partNumber, mtf + tf);
			    				
			    				
			    				//doesnt token need to be updated here?
			    			}
			    			
		    			
		    			
		    			//update the word frequency in the corpus
		    			Integer Ctf = corpusTokens.get(term);
	    				if (Ctf == null) corpusTokens.put(term, tf);
	    				else corpusTokens.put(term, Ctf + tf);
		    		
		        	}//endFor terms
		        }//endWhile line
		        
		        this.partLength.put(partNumber, length);
		       
			}//endFor children
		}//endTry
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
	}
	

}
