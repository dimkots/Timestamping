package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;


import structures.BurstInterval;
import structures.BDocument;
import structures.TDocument;
import utilities.Constants;
import utilities.Day;
import utilities.Index;
import utilities.Sorter;

public class Simple {


	private ArrayList<TDocument> testDocs;	
	private HashMap<String, ArrayList<BurstInterval>> terms;

	private HashMap<String, HashMap<Integer, Double>> dataset;

	private int timeStart = 1;
	private int timeEnd = 4000;
	private int K = 10;

	private int maxtime = 0;
	private int hitDay1;
	private int hitDay0;
	private int hitDay2;
	private int hitDay3;
	private double avgFrameLength;
	private int hitRelaxed2l3;
	private int hitRelaxed1l3;
	private int hitRelaxed1l5;
	private int hitRelaxed1l40;
	private int noBursts;
	private double goodMethod;

	public int maxTfIdfIndex = 0;

	public Simple(String docsFilename, Day startDay) throws IOException {
		super();

		dataset = new HashMap<String, HashMap<Integer, Double>>();
		this.hitDay1 = 0;
		this.hitDay0 = 0;
		this.hitDay2 = 0;
		this.hitDay3 = 0;
		this.hitRelaxed2l3 = 0;
		this.hitRelaxed1l3 = 0;
		this.hitRelaxed1l5 = 0;
		this.hitRelaxed1l40 = 0;
		this.noBursts = 0;
		this.goodMethod = 0;
		readDocsFile(docsFilename, startDay);

	}


	public Simple() {
		super();

		dataset = new HashMap<String, HashMap<Integer, Double>>();
		this.hitDay1 = 0;
		this.hitDay0 = 0;
		this.hitDay2 = 0;
		this.hitDay3 = 0;
		this.hitRelaxed2l3 = 0;
		this.hitRelaxed1l3 = 0;
		this.hitRelaxed1l5 = 0;
		this.hitRelaxed1l40 = 0;
		this.noBursts = 0;
		this.goodMethod = 0;
	}









	public HashMap<String, ArrayList<BurstInterval>> getTerms() {
		return terms;
	}


	public double sumSeriesAlgorithm(int frameDays, int extDays, Index index, int ktfidf) throws IOException{
		String [] tDocTerms = null;

		//Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();

		this.hitDay1 = 0;
		this.hitDay0 = 0;
		this.hitDay2 = 0;
		this.hitDay3 = 0;
		this.noBursts = 0;
		this.avgFrameLength = 0;



		//System.out.println("Computed idfs");
		while (j<iterations){

			TDocument tDoc = this.testDocs.get(j);
			j++;


			//Get the bursty intervals for the terms of the doc
			tDocTerms = tDoc.getRepresentativeTerms();
			//tDocTerms = tDoc.getTopKTerms(ktfidf, index);


			double[][] burstSeries = produceBurstSeries(tDocTerms);

			// double[] avgSeries = computeAvg(burstSeries);
			double[] sumSeries = computeSum(burstSeries);

			int[] sumIndices = new int[sumSeries.length];

			for (int k=0;k<sumIndices.length;k++)
				sumIndices[k]=k;

			Sorter.quicksort(sumSeries, sumIndices);

			int[] days = new int[sumSeries.length];

			int i = sumSeries.length - 1;
			while (Sorter.sumArray(days) < frameDays && i>=0) {

				int targetDay = sumIndices[i];
				days[targetDay] = 1;
				for (int k = 0; k < extDays; k++) {

					if (Sorter.sumArray(days) >= frameDays)
						break;

					if (targetDay - k >= 0 )
						days[targetDay - k] = 1;
					if (targetDay + k < days.length)
						days[targetDay + k] = 1;
				}

				i--;
			}

			this.avgFrameLength+= Sorter.sumArray(days);

			int actualTimestamp = tDoc.getTimestamp();

			if (days[actualTimestamp]==1)
				this.hitDay0++;


		}

		return (double)this.hitDay0/10;
	}


	public void overlapsAlgorithm(){

		String [] tDocTerms = null;

		//Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();
		int K = 5;
		int frameDays = 30;
		int extDays = 5;

		while (j<iterations){

			TDocument tDoc = this.testDocs.get(j);
			j++;

			//Get the bursty intervals for the terms of the doc
			tDocTerms = tDoc.getRepresentativeTerms();

			double[][] burstSeries = produceBurstSeries(tDocTerms);

			//double[] avgSeries = computeAvg(burstSeries);			
			double[] sumSeries = computeSum(burstSeries);


			int[] sumIndices = new int[sumSeries.length];

			Sorter.quicksort(sumSeries, sumIndices);


			int[] days = new int[sumSeries.length];

			int i = sumSeries.length-1;
			while (Sorter.sumArray(days)<frameDays){

				int targetDay = sumIndices[i];
				days[targetDay]=1;
				for (int k=0;k<extDays;k++){
					if (targetDay-k>=0)
						days[targetDay-k]=1;
					if (targetDay+k<days.length)
						days[targetDay+k]=1;
				}

				i--;
			}




			//			System.out.println("Document with number:" + randomNumber +" at: "+ tDoc.getTimestamp()+ " in year: "+ computeYear(tDoc.getTimestamp()));
			//			System.out.println(body);
			noBursts++;
			for ( i=0;i<sumSeries.length;i++){
				if (sumSeries[i]>0){
					noBursts--;
					break;
				}
			}

			int[] index = new int[timeEnd - timeStart + 1];
			for ( i = 0; i < index.length; i++)
				index[i]=i;

			for ( i = timeStart-1; i<timeEnd; i++)
				if (sumSeries[i]>1){
					//					System.out.print(i+","+sumSeries[i]+"  ");
				}

			if (sumSeries[tDoc.getTimestamp()]>0)
				this.hitDay1++;
			if (sumSeries[tDoc.getTimestamp()]>1)
				this.hitDay2++;
			if (sumSeries[tDoc.getTimestamp()]>2)
				this.hitDay3++;

			if (tDoc.getTimestamp()>0 && (sumSeries[tDoc.getTimestamp()]>1 || sumSeries[tDoc.getTimestamp()+1]>1 || sumSeries[tDoc.getTimestamp()-1]>1))
				this.hitRelaxed2l3++;

			if (tDoc.getTimestamp()>0 && (sumSeries[tDoc.getTimestamp()]>0 || sumSeries[tDoc.getTimestamp()+1]>0 || sumSeries[tDoc.getTimestamp()-1]>0))
				this.hitRelaxed1l3++;

			if (tDoc.getTimestamp()>1 && (sumSeries[tDoc.getTimestamp()]>0 || sumSeries[tDoc.getTimestamp()+1]>0 || sumSeries[tDoc.getTimestamp()-1]>0
					|| sumSeries[tDoc.getTimestamp()+2]>0 || sumSeries[tDoc.getTimestamp()-2]>0))
				this.hitRelaxed1l5++;


			for ( i=0; i<=20;i++){

				if (tDoc.getTimestamp()>20 && (sumSeries[tDoc.getTimestamp()+i]>0 || sumSeries[tDoc.getTimestamp()+i] >0)){
					this.hitRelaxed1l40++;
					break;
				}

			}

			//			System.out.println("\nTop-K timestamps: ");

			double[] sumSeriesClone = sumSeries.clone();
			Sorter.quicksort(sumSeriesClone, index);

			for ( i = 0; i<this.K; i++){
				//				System.out.print(index[index.length-1-i]+","+sumSeriesClone[sumSeriesClone.length-1-i]+"  ");
			}

			sumSeriesClone = sumSeries.clone();
			double[] years = estimatePubYear(sumSeriesClone);
			index = new int[11];
			for ( i = 0; i < index.length; i++)
				index[i]=i;
			Sorter.quicksort(years, index);
			//	System.out.println("\nYears: ");
			for ( i = 0; i<this.K; i++){
				//	System.out.print(index[index.length-1-i]+","+years[years.length-1-i]+"  ");
			}


			//			
			//			System.out.println("\n\n");


		}


		System.out.println("\nHit1:" + this.hitDay1);
		System.out.println("Hit2:" + this.hitDay2);
		System.out.println("Hit3:" + this.hitDay3);
		System.out.println("Relaxed Hit 2-3:" + this.hitRelaxed2l3);
		System.out.println("Relaxed Hit 1-3:" + this.hitRelaxed1l3);
		System.out.println("Relaxed Hit 1-5:" + this.hitRelaxed1l5);
		System.out.println("Relaxed Hit 1-40:" + this.hitRelaxed1l40);
		System.out.println("Articles with no bursts:" + this.noBursts);
	}


	/* With a simple ranking according to sum(over the year) of sums(for each timestamp)*/
	private double[] estimatePubYear(double[] sumSeries) {

		double[] years = new double[11];
		double sum = 0;
		int year = 0;

		for (int i = timeStart-1; i<timeEnd; i++){

			if ((i-1)%365==0){

				years[year] = sum;
				sum = 0;
				year++;
			}

			sum+= sumSeries[i];

		}

		return years;
	}


	private int computeYear(int timestamp){
		int year = timestamp/365;

		return year;
	}

	private double[][] produceBurstSeries(String[] tDocTerms) {

		double[][] burstSeries = new double[tDocTerms.length][750]; 
		int i = 0, j = 0;
		boolean hasBurst = false;
		//Fill the table with zeros
		for (i=0;i<tDocTerms.length;i++)
			for (j= 0 ;j<350;j++)
				burstSeries[i][j]= 0;

		i = 0;
		for (String term : tDocTerms){
			ArrayList<BurstInterval> bursts = terms.get(term);

			if (bursts==null) continue;

			hasBurst = true;

			for (BurstInterval bint: bursts)

				for (j=bint.getStart();j<=bint.getEnd();j++)
					burstSeries[i][j] = bint.getScore();	



			i++;
		}

		if (!hasBurst) this.noBursts++;

		return burstSeries;
	}

	private double[] computeAvg(int[][] burstSeries){
		double[] avg = new double[timeEnd - timeStart + 1];

		int time = 0;
		int sum = 0;

		for (time = timeStart-1 ; time < timeEnd; time++){
			for (int j = 0; j<burstSeries.length; j++)
				sum+= burstSeries[j][time];
			avg[time] = (double)sum / burstSeries.length;
			sum = 0;
		}

		return avg;
	}


	private double[] computeSum(double[][] burstSeries){
		double[] sumSeries = new double[750];

		int time = 0;
		int sum = 0;

		for (time = 0 ; time < 750; time++){
			for (int j = 0; j<burstSeries.length; j++)
				sum+= burstSeries[j][time];
			sumSeries[time] = sum;
			sum = 0;
		}

		return sumSeries;
	}

	/* Reads a file with the bursty intervals, where each line corresponds to a term
	 * This method reads a file of the form 
	 * term1: i1, i2, ... , iN
	 * ... 
	 * termK: i1, i2, ... , iN
	 * 
	 * where ix = (ixStartTimestamp, ixEndTimestamp)
	 */
	public void readBurstsFile(String burstsFilename) {
		File burstsFile = new File(burstsFilename);
		this.terms = new HashMap<String, ArrayList<BurstInterval>>();

		try {
			Reader rin = new InputStreamReader(new FileInputStream(burstsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line;

			while ((line = in.readLine()) != null){
				String[] lineElements = line.split(":");
				String term = lineElements[0];

				ArrayList<BurstInterval> burstList = new ArrayList<BurstInterval>();
				String[] bursts = lineElements[1].split(" ");

				for (String burst : bursts){
					burstList.add(new BurstInterval(burst, term));
				}

				this.terms.put(term, burstList);
			}
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}



	void simpleTest(String outputFilename){
	}



	private void readDocsFile(String docsFilename, Day startDay){

		File docsFile = new File(docsFilename);

		try {
			Reader rin = new InputStreamReader(new FileInputStream(docsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line;
			while ((line = in.readLine()) != null){

				TDocument tDoc = new TDocument(line, startDay, true, false);
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

	public int getHitDay2() {
		return hitDay2;
	}


	public int getHitDay3() {
		return hitDay3;
	}




	/* 	Reads a file with test documents, where each line corresponds to a document
	 * 	This method reads a file of the form 
	 *	line is of the form: 
	 * 	Country\t41\tSun Oct 12 2008\tlimit:1 expert:1 of:1 welcom:1 deci:1 insur:1 deposit:1 presid:1 on:1 foreign:1 increa:1 payout:1 	2009:6 
	 * 	timestemp day month date year tterm1:ttfreq1 .. ttermN:ttfreqN \tbterm1:btfreq1 .. btermM:btfreqM \t topicsString */

	public void readTestDocs(String testdocsFilename, Day startDay) {

		this.testDocs = new ArrayList<TDocument>();
		File docsFile = new File(testdocsFilename);

		try {
			Reader rin = new InputStreamReader(new FileInputStream(docsFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line;

			while ((line = in.readLine()) != null){
				TDocument tDoc = new TDocument(line, startDay, true, true);
				this.testDocs.add(tDoc);	    		

			}

			in.close();
			rin.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}


	public ArrayList<TDocument> getTestDocs() {
		return testDocs;
	}



	public void testRepTermsWithGraphs(String graphFilename, String cliqueFilename, String cliquesResultsFilename, boolean weight, Index lucIndex, int K, int termSelector) throws IOException{

		String [] tDocTerms = null;

		//Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();

		this.hitDay1 = 0;
		this.hitDay0 = 0;
		this.hitDay2 = 0;
		this.hitDay3 = 0;
		this.noBursts = 0;
		this.avgFrameLength = 0;

		while (j<iterations){

			TDocument tDoc = this.testDocs.get(j);
			j++;

			//Get the bursty intervals for the terms of the doc
			//tDocTerms = tDoc.getRepresentativeTerms();
			tDocTerms = tDoc.getTopKTerms(K, lucIndex, termSelector);
			ArrayList<BurstInterval> burstList = new ArrayList<BurstInterval>();

			for (String term : tDocTerms){
				if (this.terms.get(term)==null)
					continue;
				burstList.addAll(this.terms.get(term));
			}

			if (burstList.isEmpty() || burstList.size()>=2000){
				this.noBursts++;
				continue;
			}


			Wclique.createGraphFile(burstList, graphFilename, weight, 0);

			Wclique.testWclique(graphFilename, cliqueFilename);


			/* burst is of the form: (start,end,score) */
			ArrayList<BurstInterval> testBurstList = new ArrayList<BurstInterval>();
			testBurstList = (ArrayList<BurstInterval>) burstList.clone();
			int actualTimestamp = tDoc.getTimestamp();
			String burst = "("+actualTimestamp+","+actualTimestamp+",400000)";
			BurstInterval newBint = new BurstInterval( burst, "actualStamp");
			testBurstList.add(newBint);
			Wclique.createGraphFile(testBurstList, "test"+graphFilename, weight, 0);

			Wclique.testWclique("test"+graphFilename, "test"+cliqueFilename);

			interpretCliqueRepTerms(tDoc, burstList, testBurstList, cliqueFilename, cliquesResultsFilename, tDocTerms);

		}


		System.out.println("Not processed: " + this.noBursts);
		System.out.println("Avg jaccard:" + (double)this.goodMethod/(1000-this.noBursts));
		System.out.println((double)this.hitDay0);
		System.out.println((double)this.hitDay1);
		System.out.println((double)this.hitDay2);
		System.out.println((double)this.hitDay3);
		//System.out.println("No bursts:" + this.noBursts);
		System.out.println(this.avgFrameLength/1000);
		//System.out.println("Time:" + this.maxtime);


	}


	public void testCharTerms(Index lucIndex, int K,int termSelector) throws IOException {

		// Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();


		String [] tDocTerms = null;

		double hitRateAcc = 0;
		int noBurst = 0;
		lucIndex.computeIdfs();
		while (j < iterations) {

			TDocument tDoc = this.testDocs.get(j);
			j++;

			int X = tDoc.getTimestamp();

			// Get the bursty intervals for the terms of the doc
			// tDocTerms = ((NewsDocument)tDoc).getRepresentativeTerms();
			tDocTerms = tDoc.getTopKTerms(K, lucIndex, termSelector);


			int totalTerms = tDocTerms.length;
			if (totalTerms==0)
				continue;
			int hit = 0;
			int termNoBurst = 0;
			boolean stampWritten = false;
			for (String term : tDocTerms) {
				if (this.terms.get(term) == null)
					continue;
				ArrayList<BurstInterval> burstList = this.terms.get(term);

				if (burstList.isEmpty() || burstList==null)
					termNoBurst++;
				for (BurstInterval bint : burstList){
					if (bint.getEnd()+3>=X && bint.getStart()-3<=X){
						if (!stampWritten){
							System.out.println("\n" + j+ " Actual Timestamp: "+X);
							stampWritten=true;
						}
						System.out.println(bint.getTerm()+" start:"+bint.getStart()+" end:"+bint.getEnd());
						hit++;
						break;
					}
				}

			}

			if (termNoBurst==totalTerms)
				noBurst++;

			double hitRate = (double)hit/totalTerms;

			hitRateAcc+= hitRate;

		}

		System.out.println("Total Hit Rate: " + hitRateAcc/1000);
		System.out.println("No Burst: " + noBurst);



	}


	public void graphAlgorithm(String graphFilename, String cliqueFilename, String cliquesResultsFilename, boolean weight, boolean union, Index lucIndex, int K, int cliques, int termSelector) throws IOException{
		String [] tDocTerms = null;

		//Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();

		this.hitDay1 = 0;
		this.hitDay0 = 0;
		this.hitDay2 = 0;
		this.hitDay3 = 0;
		this.noBursts = 0;
		
		int manybursts = 0;
		this.avgFrameLength = 0;

		int[] cliqueSuccess = new int[cliques];


		int realHits = 0;
		
		while (j<iterations){
			System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;

			//Get the bursty intervals for the terms of the doc
			//tDocTerms = tDoc.getRepresentativeTerms();
			tDocTerms = tDoc.getTopKTerms(K, lucIndex, termSelector);
			ArrayList<BurstInterval> burstList = new ArrayList<BurstInterval>();

			int totalBurstCnt = 0;
			for (String term : tDocTerms){
				if (this.terms.get(term)==null)
					continue;
				totalBurstCnt+= this.terms.get(term).size();
			}


			for (String term : tDocTerms){
				if (this.terms.get(term)==null)
					continue;

				int burstCnt = this.terms.get(term).size();

				double score =Math.pow((double)totalBurstCnt/burstCnt,3);
				//				for (BurstInterval bint : this.terms.get(term))
				//					bint.setScore(score);
				//				
				burstList.addAll(this.terms.get(term));

			}


			if (burstList.isEmpty()){
				this.noBursts++;
				continue;
			}
			
			if (burstList.size()>=10000){
				manybursts++;
				continue;
			}



			boolean found = false;

			System.out.println(j+" Hits before:" + this.hitDay0);
			for (int i=0;i<cliques;i++){
				if (burstList.size()==0)
					continue;
				
				Wclique.createGraphFile(burstList, graphFilename, weight, 0);
				//System.out.println(j+" " + i);
				Wclique.testWclique(graphFilename, cliqueFilename);
				//System.out.print(i+". ");
				int prevHit = this.hitDay0;
				found = interpretCliqueResults(tDoc, burstList, cliqueFilename, cliquesResultsFilename, union, found);

				//This clique did the job
				if (this.hitDay0!=prevHit)
					cliqueSuccess[i]++;


			}
			
			if (found)
				realHits++;
			
			System.out.println("Hits after:" + this.hitDay0);
			//System.out.println();

		}

		
		System.out.print(this.avgFrameLength/(iterations-this.noBursts)+"\t");
		System.out.print((double)realHits/(iterations-this.noBursts)+"\t");
		
		System.out.print((double)this.hitDay0*100/(iterations-this.noBursts)+"\t");
		System.out.print(this.noBursts + "\t" + manybursts + "\t");
		for (int i=0;i<cliques;i++)
			System.out.print(cliqueSuccess[i]+"\t");

		System.out.println();
	}


	private boolean interpretCliqueResults(TDocument tDoc, ArrayList<BurstInterval> burstList, String cliqueFilename, String cliquesResultsFilename, boolean union, boolean foundAlready) {

		boolean found = false;

		try {
			File cliqueFile = new File(cliqueFilename);
			Reader rin = new InputStreamReader(new FileInputStream(cliqueFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );


			File resultsFile = new File(cliquesResultsFilename);
			FileWriter fw = new FileWriter(resultsFile,true);
			BufferedWriter out = new BufferedWriter(fw);


			String line;
			while ((line = in.readLine()) != null){
				//19 19 19 19 19 19 59 87 142 116
				String[] cliqueString = line.split(" ");
				int[] cliqueNodes = new int[cliqueString.length];

				for (int i=0;i<cliqueString.length;i++)
					cliqueNodes[i]= Integer.parseInt(cliqueString[i]);

				cliqueNodes = toUniqueArray(cliqueNodes);

				String[] cliqueTerms = new String[cliqueNodes.length];

				int start;
				int end;
				if (union){
					start = timeEnd;
					end = -1;
					for (int i=0;i<cliqueNodes.length;i++){
						cliqueTerms[i] = burstList.get(cliqueNodes[i]).getTerm();

						if (burstList.get(cliqueNodes[i]).getStart()<start)
							start = burstList.get(cliqueNodes[i]).getStart();

						if (burstList.get(cliqueNodes[i]).getEnd()>end)
							end = burstList.get(cliqueNodes[i]).getEnd();
					}	
				}
				else{
					start = -1;
					end = timeEnd;
					for (int i=0;i<cliqueNodes.length;i++){
						cliqueTerms[i] = burstList.get(cliqueNodes[i]).getTerm();

						if (burstList.get(cliqueNodes[i]).getStart()>start)
							start = burstList.get(cliqueNodes[i]).getStart();

						if (burstList.get(cliqueNodes[i]).getEnd()<end)
							end = burstList.get(cliqueNodes[i]).getEnd();
					}

				}

				start-=2;
				end+=2;

				Arrays.sort(cliqueNodes);


				for (int i=0;i<cliqueNodes.length;i++)
					burstList.remove(cliqueNodes[cliqueNodes.length-1-i]);


				out.write("AT: " + tDoc.getTimestamp() + " C: ("+start + "," +end +")\n");
				//System.out.println("AT: " + tDoc.getTimestamp() + " C: ("+start + "," +end +")");
				if (tDoc.getTimestamp()>this.maxtime)
					this.maxtime = tDoc.getTimestamp();

				if (tDoc.getTimestamp()>=start && tDoc.getTimestamp()<=end && !foundAlready){
					this.hitDay0++;
					found = true;
				}

				this.avgFrameLength+= (end - start+1);
			}

			in.close();
			rin.close();

			out.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

		return (found || foundAlready);

	}


	private double computeJaccard(List<String> repTerms, List<String> cliqueTerms){

		double jaccard = 0;

		int u = 0;
		int i = 0;

		ArrayList<String> unionList = new ArrayList<String>();
		for (String repTerm : repTerms){
			if (cliqueTerms.contains(repTerm))
				i++;

			if (!unionList.contains(repTerm))
				unionList.add(repTerm);
		}

		for (String cliqueTerm : cliqueTerms){
			if (!unionList.contains(cliqueTerm))
				unionList.add(cliqueTerm);

		}

		u = unionList.size()-1;

		jaccard = (double)i/u;
		return jaccard;

	}



	private void interpretCliqueRepTerms(TDocument tDoc, ArrayList<BurstInterval> burstList, ArrayList<BurstInterval> testBurstList, String cliqueFilename, String cliquesResultsFilename, String[] tDocTerms) {

		try {
			File cliqueFile = new File(cliqueFilename);
			Reader rin = new InputStreamReader(new FileInputStream(cliqueFile), "UTF-8");
			BufferedReader in = new BufferedReader( rin );

			File testCliqueFile = new File("test"+cliqueFilename);
			Reader testRin = new InputStreamReader(new FileInputStream(testCliqueFile), "UTF-8");
			BufferedReader testIn = new BufferedReader( testRin );

			File resultsFile = new File(cliquesResultsFilename);
			FileWriter fw = new FileWriter(resultsFile,true);
			BufferedWriter out = new BufferedWriter(fw);


			String line, testLine;
			while ((line = in.readLine()) != null){
				//19 19 19 19 19 19 59 87 142 116
				String[] cliqueString = line.split(" ");
				int[] cliqueNodes = new int[cliqueString.length];

				for (int i=0;i<cliqueString.length;i++)
					cliqueNodes[i]= Integer.parseInt(cliqueString[i]);

				cliqueNodes = toUniqueArray(cliqueNodes);

				String[] cliqueTerms = new String[cliqueNodes.length];

				testLine = testIn.readLine();
				String[] testcliqueString = testLine.split(" ");
				int[] testcliqueNodes = new int[testcliqueString.length];

				boolean actualTClique = false;

				for (int i=0;i<testcliqueString.length;i++){
					testcliqueNodes[i]= Integer.parseInt(testcliqueString[i]);
					String term = testBurstList.get(testcliqueNodes[i]).getTerm();

					if (term.equals("actualStamp"))
						actualTClique = true;


				}


				if (!actualTClique){
					System.out.println("Fail");
					this.noBursts++;
					return;
				}

				testcliqueNodes = toUniqueArray(testcliqueNodes);

				String[] testcliqueTerms = new String[testcliqueNodes.length];


				int start = -1;
				int end = timeEnd;
				for (int i=0;i<cliqueNodes.length;i++){
					cliqueTerms[i] = burstList.get(cliqueNodes[i]).getTerm();


					if (burstList.get(cliqueNodes[i]).getStart()>start)
						start = burstList.get(cliqueNodes[i]).getStart();


					if (burstList.get(cliqueNodes[i]).getEnd()<end)
						end = burstList.get(cliqueNodes[i]).getEnd();
				}	


				int testStart = -1;
				int testEnd = timeEnd;
				for (int i=1;i<testcliqueNodes.length;i++){
					testcliqueTerms[i] = testBurstList.get(testcliqueNodes[i]).getTerm();

					if (testBurstList.get(testcliqueNodes[i]).getStart()>testStart)
						testStart = testBurstList.get(testcliqueNodes[i]).getStart();

					if (testBurstList.get(testcliqueNodes[i]).getEnd()<testEnd)
						testEnd = testBurstList.get(testcliqueNodes[i]).getEnd();
				}	

				ArrayList<String> testcliqueList = new ArrayList<String>(Arrays.asList(testcliqueTerms));
				ArrayList<String> reptermsList = new ArrayList<String>(Arrays.asList(tDocTerms));

				this.goodMethod+= computeJaccard(reptermsList, testcliqueList);

				if (tDoc.getTimestamp()>this.maxtime)
					this.maxtime = tDoc.getTimestamp();

				if (tDoc.getTimestamp()>=start && tDoc.getTimestamp()<=end ){
					this.hitDay0++;
					out.write(tDoc.getTimestamp() + "\t" +start+" "+end+ "\t" + Arrays.toString(tDoc.getRepresentativeTerms()) + "\t" + Arrays.toString(cliqueTerms) + "\n");
				}

				if (tDoc.getTimestamp()>=start-10 && tDoc.getTimestamp()<=end+10 )
					this.hitDay1++;

				if (tDoc.getTimestamp()>=start-20 && tDoc.getTimestamp()<=end+20 )
					this.hitDay2++;


				if (tDoc.getTimestamp()>=start-40 && tDoc.getTimestamp()<=end+40 )
					this.hitDay3++;

				this.avgFrameLength+= (end - start+1);
			}

			in.close();
			rin.close();

			out.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}
















	//Return true if number num is appeared only once in the array – num is unique.
	private static boolean isUnique(int [] arry, int num)
	{
		for (int i = 0; i < arry.length;i++)
			if (arry [i] == num)
				return false;
		return true;
	}

	//Convert the given array to an array with unique values – without duplicates and Return it
	private static int[] toUniqueArray (int[] array)
	{
		int[]temp=new int[array.length];
		for(int i=0;i<temp.length;i++)
			temp[i]=-1;// in case u have value of 0 in the array
		int counter=0;
		for(int i=0;i<array.length;i++)
			if(isUnique(temp,array[i]))
				temp[counter++]=array[i];

		int []uniqueArray=new int[counter];
		System.arraycopy(temp, 0, uniqueArray, 0, uniqueArray.length);
		return uniqueArray;
	}


	public int getHitRelaxed1l5() {
		return hitRelaxed1l5;
	}


	public void setHitRelaxed1l5(int hitRelaxed1l5) {
		this.hitRelaxed1l5 = hitRelaxed1l5;
	}


	public HashMap<String, HashMap<Integer, Double>> getDataset() {
		return dataset;
	}




}


//	
//
//}
//	


