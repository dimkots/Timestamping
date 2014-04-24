package algorithms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.print.Doc;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import burstdetection.BurstEngine;

import com.aliasi.spell.JaccardDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import clustering.DataPoint;
import clustering.IntervalClusterer;
import clustering.JCA;

import structures.BurstInterval;
import structures.TDocument;
import utilities.Day;
import utilities.DocVector;
import utilities.Index;
import utilities.Preprocessor;
import utilities.Sorter;
import utilities.TermVectorBasedSimilarity;
import wekaClustering.HAClusterer;

import utilities.Interval;
public class BurstySimDater {


	public static int MAXSCORE = 0;
	public static int AVGSCORE = 1;
	public static int MEDIANSCORE = 2;
	public static int SUMSCORE = 3;
	public static int NODECNTSCORE = 4;

	public static int TF = 0;
	public static int TFIDF = 1;
	public static int IDF = 2;


	private ArrayList<TDocument> testDocs;	
	private HashMap<String, ArrayList<BurstInterval>> terms;

	private HashMap<String, HashMap<Integer, Double>> dataset;

	private int timeStart = 1;
	private int timeEnd = 4500;
	private int K = 10;


	private String graphsFilename;
	private String cliquesFilename;

	public int maxTfIdfIndex = 0;
	private int currentBag;


	public BurstySimDater() {
		super();

		dataset = new HashMap<String, HashMap<Integer, Double>>();
	}










	public HashMap<String, ArrayList<BurstInterval>> getTerms() {
		return terms;
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


	public void computeBursts(String trainingdocsFilename, String burstsFilename, HashMap<String, HashMap<Integer, Double>> trainingSet) throws IOException {
		System.out.print("Computing appearance sequences for each token...");
		Preprocessor.computeFreqSequences(trainingSet, trainingdocsFilename+"TS", false);
		System.out.println("Finished!");	
		
		
//		BurstEngine.computeMAX2Bursts("timeSeries"+trainingdocsFilename, burstsFilename, 0, 0, true);			
//		BurstEngine.computeMAX1Bursts(trainingdocsFilename+"TS", burstsFilename, 0, 0, true,1);
		System.out.print("Computing bursts...");
		BurstEngine.computeMAXKBurstsSplit(trainingdocsFilename+"TS", burstsFilename, 0, 0, 150, true);
		System.out.println("Finished");

	}


















	public ArrayList<TDocument> getTestDocs() {
		return testDocs;
	}









	public void rankedWeightedCliques(int termSelector, int ktfidf, int extendLength, Index lucIndex, int scoreType, int kCliques) throws IOException, ParseException{

		//		File file = new File("SF-Call1-results.txt");
		//	    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF-8" ));
		//	    DecimalFormat f = new DecimalFormat("##.0000");  // this will helps you to always keeps in two decimal places

		String [] tDocTerms = null, tDocBodyTerms = null;
		int iterations = this.testDocs.size();
		int[] cliqueSizes = new int[ktfidf];
		int[] cliqueHits = null;

		int noBursts = 0, manybursts = 0, overlapsSum = 0, overlapTermsSum = 0, avgPercentageSum = 0, 
				realCliqueHits=0, realHits = 0, aHit = 0, cliqueLengthSumDatasetLevel = 0, predDays = 0, clusterHits = 0, clusterDays = 0, clusterOnlyHits=0;

		double avgDistSum = 0, hitAvgDistSum = 0;

		if (kCliques!=-1)
			cliqueHits = new int[kCliques];

		int[] dayHits = new int[730];

		//Get all terms from the index
		Map<String, Integer> corpusTerms = lucIndex.getTermsMap();

		//For each testing document
		int j = 0;
		while (j<iterations){

			System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;


			int[] days = new int[timeEnd-timeStart];
			//Discard very short documents
			if (tDoc.getBodyStr().length()<5)
				continue;

			tDocTerms = tDoc.getTopKTerms(ktfidf, lucIndex, this.dataset);
			tDocBodyTerms = tDoc.getBodysTerms();


			Document[] knnDocs = lucIndex.getKNNBasedOnText(tDoc, tDocTerms, 100);


			//printknnDocs(tDoc, knnDocs, lucIndex, out);


			//Get a reasonable interval arround the actual creation timestamp (+- 10-15 days)
			int actualTimestamp = tDoc.getTimestamp();
			int actualFrom = actualTimestamp-extendLength;
			int actualTo = actualTimestamp+extendLength;

			//Adjust the actual interval
			if (actualFrom<0) actualFrom = 0;
			if (actualTo>729) actualTo = 729;

			// holds the number of overlaps between a burst in the rep. terms and the actual interval
			int overlapsCnt = 0;

			// how many terms have an overlapping burst with the actual interval 
			int overlapTermsCnt = 0;

			// get the bursts for the rep terms
			ArrayList<BurstInterval> burstList = new ArrayList<BurstInterval>();

			//int totalBurstCnt = 0;
			double docBurstPercentageSum = 0;


			//out.write(j + "\t" + actualTimestamp + "\n");

			// for each characteristic term
			for (String term : tDocTerms){
				if (this.terms.get(term)==null)
					continue;

				// get its bursts
				ArrayList<BurstInterval> bursts = this.terms.get(term);

				int termBurstyDays = 0;
				for (BurstInterval bint : bursts){
					int length = bint.getEnd()-bint.getStart()+1;
					termBurstyDays+= length;
				}

				docBurstPercentageSum += (double)termBurstyDays/730;	
				burstList.addAll(bursts);
			}

			double docAvgBurstPercentagePerTerm = docBurstPercentageSum/tDocTerms.length;

			if (!Double.isNaN(docAvgBurstPercentagePerTerm))
				avgPercentageSum+= docAvgBurstPercentagePerTerm;

			if (burstList.isEmpty()){
				noBursts++;
				continue;
			}

			if (burstList.size()>=10000){
				manybursts++;
				continue;
			}

			//create bursty interval graph and find all maximal cliques
			SimpleGraph<BurstInterval, DefaultEdge> sg = BurstySimDater.createGraph(burstList);
			BronKerboschCliqueFinder<BurstInterval, DefaultEdge> cf = new BronKerboschCliqueFinder<>(sg);
			List<Set<BurstInterval>> cliques = (List<Set<BurstInterval>>) cf.getAllMaximalCliques();

			int[] rankedIndices = BurstySimDater.scoreAndRankCliques(cliques, scoreType, this.terms);

			int lastClique;
			if (kCliques!=-1)
				lastClique = kCliques; 
			else
				lastClique = rankedIndices.length;

			if (lastClique>rankedIndices.length)
				lastClique = rankedIndices.length;

			int cliqueLengthSumDocLevel = 0, cliqueStart, cliqueEnd;

			System.out.print("Extended Target Interval: ("+ actualFrom +","+ actualTo +") \n");
			System.out.print("AT: "+ actualTimestamp + " Cliques: ");

			boolean realHit = false;
			boolean exHit = false;
			boolean foundClusterHit = false;

			List<Interval> cliqueIntervals = new ArrayList<Interval>();

			double distanceSum = 0;


			//check the first kCliques 
			for (int i=0;i<lastClique;i++){

				//				out.write(j + "\t" + actualTimestamp + "\t");

				Set<BurstInterval> tempClique = cliques.get(rankedIndices[i]);

				// compute the clique interval
				cliqueStart = Integer.MAX_VALUE;
				cliqueEnd = -1;

				for (BurstInterval bint : tempClique){
					if (bint.getStart()<cliqueStart)
						cliqueStart = bint.getStart();

					if (bint.getEnd()>cliqueEnd)
						cliqueEnd = bint.getEnd();
				}


				cliqueStart-=2;
				cliqueEnd+=2;


				//				out.write(cliqueStart+","+cliqueEnd+"\t");

				//				if (cliqueEnd-cliqueStart > 90){
				//					int middlePoint = (cliqueStart+cliqueEnd)/2;
				//				
				//					cliqueStart = middlePoint-20;
				//					cliqueEnd = middlePoint+20;
				//				}

				if (cliqueStart<0)
					cliqueStart = 0;
				if (cliqueEnd>729)
					cliqueEnd = 729;


				//				int docsCnt = 1000;
				//				ArrayList<Document> docList = lucIndex.getDocsInInterval(cliqueStart, cliqueEnd, docsCnt);
				//				
				//				
				//				
				//				DocVector testDocVector = new DocVector(corpusTerms);
				//				HashMap<String, Integer> bodyMap = tDoc.getBody();
				//				
				//				for (int t = 0; t < tDocBodyTerms.length; t++) {
				//					String term = tDocBodyTerms[t];
				//					testDocVector.setEntry(term, bodyMap.get(term));
				//				}
				//				
				//				testDocVector.normalize();
				//				
				//				
				//				
				//				double[] docSimilarities = new double[docList.size()];
				//				int docCnt = 0;
				//				
				//				
				//				for (Document doc : docList){
				//					
				//					DocVector docVector = lucIndex.getTermFreqVector(Integer.parseInt(doc.get("docId")), corpusTerms);
				//					docSimilarities[docCnt] = -TermVectorBasedSimilarity.getCosineSimilarity(testDocVector, docVector);
				//					docCnt++;
				//				}
				//				
				//				
				//				int[] docIndices = new int[docList.size()];
				//				for (int t=0; t< docIndices.length; t++)
				//					docIndices[t]=t;
				//				
				//				Sorter.quicksort(docSimilarities, docIndices);
				//				
				//				int ksimilardocs = Math.min(docList.size(), 30);
				//				
				//				for (int t=0;t<ksimilardocs;t++){
				//					
				//					Document doc = docList.get(docIndices[t]);
				//					int docTimestamp = Integer.parseInt(doc.get("timestamp"));
				//					
				//					distanceSum += Math.abs(actualTimestamp-docTimestamp);




				//					out.write(doc.get("docId")+"@"+(f.format(-docSimilarities[t]))+"@"+doc.get("timestamp")+"\t");
				//				}

				//				out.write("\n");

				//				double avgDist = distanceSum/ksimilardocs;

				//				avgDistSum+= avgDist;


				//				int size = tempClique.size();

				//				Interval cliqueInt = new Interval(cliqueStart, cliqueEnd);
				//				cliqueInt.setNodeCnt(size);
				//				cliqueIntervals.add(cliqueInt);
				//				
				cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);

				System.out.print("("+ cliqueStart +","+ cliqueEnd +") ");


				//if (i<2)
				for (int d=cliqueStart; d<=cliqueEnd; d++){
					days[d]=1;
				}

				//real hit
				if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){

					//hitAvgDistSum+= avgDist;
					realCliqueHits++;
					dayHits[actualTimestamp]++;
					cliqueHits[i]++;
					realHit = true;
					System.out.print("RH!!!");
				}

				//extended hit
				if (overlaps(actualFrom, actualTo, cliqueStart, cliqueEnd, 0)){
					System.out.print("EXH!!!");
					exHit = true;
				}

				//				if (size<=cliqueSizes.length)
				//					cliqueSizes[size-1]++;
			}


			double maxDist = 0.7;
			//List<Interval> clusterCenters = this.computeClusters(cliqueIntervals, maxDist);
			//List<Interval> clusterCenters = computeIntersections(cliqueIntervals);
			//List<Interval> intersections = computeWekaClusters(cliqueIntervals);
			//	


			if (realHit)
				System.out.print("REAL HIT!!!!");


			if (foundClusterHit){
				System.out.print("CLUSTER HIT!!!! ");
				clusterHits++;
			}

			if (foundClusterHit && !realHit)
				clusterOnlyHits++;

			System.out.println();

			if (realHit || foundClusterHit)
				realHits++;

			if (realHit == true || exHit == true || foundClusterHit==true){
				System.out.print("A HIT!!!!");
				aHit++;
			}

			for (int d=0;d<days.length;d++)
				predDays+=days[d];

			System.out.println();

			cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/lastClique;

			ArrayList<String> overlappingTerms = new ArrayList<String>();
			ArrayList<BurstInterval> overlappingBursts = new ArrayList<BurstInterval>();
			for (BurstInterval bint : burstList){

				if (BurstySimDater.overlaps(bint.getStart(), bint.getEnd(), actualFrom, actualTo, 0)){
					overlapsCnt++;
					overlappingBursts.add(bint);

					if (!overlappingTerms.contains(bint.getTerm()))
						overlappingTerms.add(bint.getTerm());
				}
			}

			overlapTermsCnt = overlappingTerms.size();
			overlapTermsSum += overlapTermsCnt;
			overlapsSum+= overlapsCnt;

		}

		//		out.close();

		for (int k=0;k<K;k++)
			System.out.println("Avg " + (double)cliqueSizes[k]/iterations + " of size " + (k+1));

		System.out.println("\n\n");

		for (int k=0;k<kCliques;k++)
			System.out.println(k + "-th clique hits: " + cliqueHits[k]);

		System.out.println("Many bursts: " + manybursts);
		System.out.println("No bursts: " + noBursts);
		System.out.println("Hit percentage: " + (double)aHit/(iterations-noBursts));
		System.out.println("Real hit percentage: " + (double)realHits/(iterations-noBursts));

		System.out.println("k similar docs in cliques avg dist: " + (double)avgDistSum/(iterations-noBursts));
		System.out.println("k similar docs in cliques avg dist when HIT: " + (double)hitAvgDistSum/realCliqueHits);
		System.out.println("Cluster hit percentage: " + (double)clusterHits/(iterations-noBursts));
		System.out.println("Cluster Only hit percentage: " + (double)clusterOnlyHits/(iterations-noBursts));

		System.out.println("Avg cluster days: " + (double)clusterDays/(iterations-noBursts));
		System.out.println("Avg predDays: " + (double)predDays/(iterations-noBursts));
		System.out.println("Avg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));
		System.out.println("Burst percentage of top-k terms: " + (double)avgPercentageSum/(iterations-noBursts));
		System.out.println("Overlaps per doc: " + (double)overlapsSum/(iterations-noBursts));
		System.out.println("Overlaping terms per doc: " + (double)overlapTermsSum/(iterations-noBursts));

		//		for (Integer day : dayHits)
		//			System.out.println(day);

		System.out.println();
	}


	public static double jaccardBodySimilarity(TDocument tdoc1, Document doc2){
		double jaccardScore = 0;

		String[] tdoc1TermsArray = tdoc1.getBodyTerms();
		String[] tdoc2TermsArray = doc2.get("body").split(" ");

		int u = 0;
		int i = 0;

		List<String> tdoc1Terms = Arrays.asList(tdoc1TermsArray);
		List<String> tdoc2Terms = Arrays.asList(tdoc2TermsArray);



		ArrayList<String> unionList = new ArrayList<String>();
		for (String repTerm : tdoc1Terms){
			if (tdoc2Terms.contains(repTerm))
				i++;

			if (!unionList.contains(repTerm))
				unionList.add(repTerm);
		}

		for (String cliqueTerm : tdoc2Terms){
			if (!unionList.contains(cliqueTerm))
				unionList.add(cliqueTerm);

		}

		u = unionList.size();

		jaccardScore = (double)i/u;
		return jaccardScore;
	}





	public static double jaccardTitleSimilarity(TDocument tdoc1, Document doc2){
		double jaccardScore = 0;


		String[] tdoc1TermsArray = tdoc1.getTitleTerms();
		String[] tdoc2TermsArray = doc2.get("title").split(" ");


		int u = 0;
		int i = 0;

		List<String> tdoc1Terms = Arrays.asList(tdoc1TermsArray);
		List<String> tdoc2Terms = Arrays.asList(tdoc2TermsArray);



		ArrayList<String> unionList = new ArrayList<String>();
		for (String repTerm : tdoc1Terms){
			if (tdoc2Terms.contains(repTerm))
				i++;

			if (!unionList.contains(repTerm))
				unionList.add(repTerm);
		}

		for (String cliqueTerm : tdoc2Terms){
			if (!unionList.contains(cliqueTerm))
				unionList.add(cliqueTerm);

		}

		u = unionList.size();

		jaccardScore = (double)i/u;
		return jaccardScore;
	}
	public static double jaccardSimilarity(Document doc1, Document doc2){
		double jaccardScore = 0;

		String[] tdoc1TermsArray = doc1.get("body").split(" ");
		String[] tdoc2TermsArray = doc2.get("body").split(" ");

		int u = 0;
		int i = 0;

		List<String> tdoc1Terms = Arrays.asList(tdoc1TermsArray);
		List<String> tdoc2Terms = Arrays.asList(tdoc2TermsArray);



		ArrayList<String> unionList = new ArrayList<String>();
		for (String repTerm : tdoc1Terms){
			if (tdoc2Terms.contains(repTerm))
				i++;

			if (!unionList.contains(repTerm))
				unionList.add(repTerm);
		}

		for (String cliqueTerm : tdoc2Terms){
			if (!unionList.contains(cliqueTerm))
				unionList.add(cliqueTerm);

		}

		u = unionList.size();

		jaccardScore = (double)i/u;
		return jaccardScore;
	}


	public static double jaccardSimilarity(TDocument tdoc1, TDocument tdoc2){
		double jaccardScore = 0;

		String[] tdoc1TermsArray = tdoc1.getBodyTerms();
		String[] tdoc2TermsArray = tdoc2.getBodyTerms();

		int u = 0;
		int i = 0;

		List<String> tdoc1Terms = Arrays.asList(tdoc1TermsArray);
		List<String> tdoc2Terms = Arrays.asList(tdoc2TermsArray);



		ArrayList<String> unionList = new ArrayList<String>();
		for (String repTerm : tdoc1Terms){
			if (tdoc2Terms.contains(repTerm))
				i++;

			if (!unionList.contains(repTerm))
				unionList.add(repTerm);
		}

		for (String cliqueTerm : tdoc2Terms){
			if (!unionList.contains(cliqueTerm))
				unionList.add(cliqueTerm);

		}

		u = unionList.size();

		jaccardScore = (double)i/u;
		return jaccardScore;
	}

	public static boolean isInteger(String s) {
		try { 
			Integer.parseInt(s); 
		} catch(NumberFormatException e) { 
			return false; 
		}
		// only got here if we didn't return false
		return true;
	}

	public void jaccardAlgorithm(String datasetName, String resultsFilename, int timespan, int kJaccard, int x, int ktfidf, 
			Index lucIndex, int scoreType, int kCliques, HashMap<String, HashMap<Integer, Double>> trainingDataset) throws Exception{

		String [] tDocTerms = null, tDocBodyTerms = null;
		int iterations = this.testDocs.size();
		int[] distinctCliqueHits = null;
		int[] cliqueHits = null;
		int[] cliqueDays = null;
		int noBursts = 0, realHits = 0, cliqueLengthSumDatasetLevel = 0, predDays = 0;

		
		cliqueHits = new int[kCliques];
		distinctCliqueHits = new int[kCliques];
		cliqueDays = new int[kCliques];
	

		File resultsFile = new File(resultsFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFile, true),"UTF-8" ));	
		BufferedWriter dw = new BufferedWriter(new FileWriter(datasetName+"Distances.txt"));
		out.write("\nx\t"+ x +"\tk\t" + kJaccard +"\n");

		TokenizerFactory tokFactory = new IndoEuropeanTokenizerFactory();
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);
		File hitsFile = new File("burstHits.txt");
		BufferedWriter hitsOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hitsFile, false),"UTF-8" ));

		//For each testing document
		int j = 0;
		int docsTested = 0;
		while (j<iterations){

			double maxDist = 0;
			double minDist = 0;
			double avgDist = 0;

			int cliqueLengthSumDocLevel=0;
			if(j%1000==0)
				System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;


			int actualTimestamp = tDoc.getTimestamp();
			int[] days = new int[timeEnd-timeStart];

			//Discard very short documents
			if (tDoc.getBodyStr().length()<5)
				continue;




			tDocTerms = tDoc.getTopKTerms(ktfidf, lucIndex, trainingDataset);
			tDocBodyTerms = tDoc.getBodysTerms();


			//3. Get the top-k most similar docs to the current document
			Document[] knndocs = lucIndex.getKNNBasedOnText(tDoc,tDocTerms, kJaccard+1);


			if (knndocs==null)
				continue;

			int bodySimDocCnt = 0;
			List<Document> bodySimDocs = new ArrayList<Document>();

			//4. Iterate over them
			maxDist = 0;
			minDist = Double.MAX_VALUE;
			avgDist = 0;
			int kDocs = Math.min(kJaccard, knndocs.length);
			int actualSimDocs = 0;
			for(int i = 0;i<kDocs;i++){
				Document tempDoc = knndocs[i];

				bodySimDocs.add(tempDoc);
				double dist = jaccardD.proximity(tDoc.getBodyStr(),  knndocs[i].get("body"));

				if (dist>maxDist)
					maxDist = dist;
				if (dist<minDist)
					minDist = dist;

				avgDist+= dist;

				bodySimDocCnt++;
			}
			avgDist = avgDist / kDocs;

			boolean realHit = false;
			
			double scoreArray[] = new double[bodySimDocs.size()];
			int docIt = 0;

			int minStamp = 730;
			int maxStamp = 0;

			//For each doc in the top-k
			for (Document tempDoc : bodySimDocs){

				double docScore = 0;

				int docTimestamp = Integer.parseInt(tempDoc.get("timestamp"));


				if (docTimestamp>maxStamp)
					maxStamp = docTimestamp;
				if (docTimestamp<minStamp)
					minStamp = docTimestamp;
				//Find overlap
				String[] commonTerms = this.findCommonTerms(tDoc, tempDoc);


				//maybe tDocTerms here
				for (String term : commonTerms){

					ArrayList<BurstInterval> termBursts = this.terms.get(term);


					if (termBursts==null)
						continue;

					
					//Is there a burst at tempDoc timestamp?
					for (BurstInterval termBurst : termBursts){
						if (termBurst.getStart()<= docTimestamp && termBurst.getEnd()>=docTimestamp){
							docScore= docScore+1;
							break;
						}
					}


				}
				//maybe insert percentage here
				scoreArray[docIt] = docScore;

				docIt++;

			}

			
			docsTested++;

			boolean firstCliqueHit = false;
		
			int actualCliques = 0;
			for (int i=0;i<kCliques;i++){

				if (bodySimDocs.size()==0)
					break;

				Wclique.createNewGraphFile(bodySimDocs, scoreArray,  datasetName +"graphs.txt", x);
				Wclique.testWclique(datasetName + "graphs.txt", datasetName + "cliques.txt");

				File cliqueFile = new File( datasetName + "cliques.txt");
				Reader rin = new InputStreamReader(new FileInputStream(cliqueFile), "UTF-8");
				BufferedReader in = new BufferedReader( rin );

				String line;
				while ((line = in.readLine()) != null){
					//19 19 19 19 19 19 59 87 142 116
					String[] cliqueString = line.split(" ");
					int[] cliqueNodes = new int[cliqueString.length];

					boolean validFile = true;
					for (int c=0;c<cliqueString.length;c++){

						if (isInteger(cliqueString[c]))
							cliqueNodes[c]= Integer.parseInt(cliqueString[c]);
						else{
							validFile = false;
							break;
						}
					}

					if (!validFile)
						break;

					cliqueNodes = toUniqueArray(cliqueNodes);

					int cliqueEnd = -1;
					int cliqueStart = Integer.MAX_VALUE;

					for (int c=0;c<cliqueNodes.length;c++){
						Document doc = bodySimDocs.get(cliqueNodes[c]);
						int docStamp = Integer.valueOf(doc.get("timestamp"));

						if (docStamp<cliqueStart)
							cliqueStart = docStamp;

						if (docStamp>cliqueEnd)
							cliqueEnd = docStamp;
					}	

					actualCliques++;


					if (cliqueEnd-cliqueStart+1<x){
						int diff = x-(cliqueEnd-cliqueStart+1);
						int extend = diff/2;
						cliqueStart-= extend;
						cliqueEnd+= extend;
					}

					if (cliqueEnd<cliqueStart){
						System.out.println("LOL FAIL");
					}

					if (cliqueStart<0)
						cliqueStart = 0;
					if (cliqueEnd>timespan-1)
						cliqueEnd = timespan-1;

					cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);



					for (int d=cliqueStart; d<=cliqueEnd; d++){
						if (days[d]==0)
							days[d]=i+1;
					}


					int realDistance = 0;
					//real hit
					if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
						cliqueHits[i]++;
						realHit = true;
						if (i==0){
							hitsOut.write(j+",");				
							hitsOut.flush();
							firstCliqueHit = true;
							int hit = j-1;

						}


					}
					else{


						if(cliqueStart > actualTimestamp){
							realDistance = cliqueStart - actualTimestamp; 
						}
						else if(cliqueEnd < actualTimestamp){
							realDistance = actualTimestamp - cliqueEnd;
						}
						
					}



					dw.write(realDistance+"\t");



					


					Arrays.sort(cliqueNodes);

					for (int c=0;c<cliqueNodes.length;c++)
						bodySimDocs.remove(cliqueNodes[cliqueNodes.length-1-c]);

				}

				in.close();
				dw.write("\n");
			}

			//if found
			if (days[actualTimestamp]>0)
				distinctCliqueHits[days[actualTimestamp]-1]++;

			for (int d=0;d<days.length;d++)
				if (days[d]>0)
					cliqueDays[days[d]-1]++;

			if (realHit){

				realHits++;
			}

			if (firstCliqueHit){
				//out.write("YES\t"+maxDist+"\t"+minDist+"\t"+avgDist+"\t"+simDocsSpan+"\t"+firstCliqueStart+"\t"+firstCliqueEnd+"\t"+actualTimestamp+"\n");
				//hitsOut.write("YES\t"+burstCount+"\n");
			}
			else{
				//out.write("NO\t"+maxDist+"\t"+minDist+"\t"+avgDist+"\t"+simDocsSpan+"\t"+firstCliqueStart+"\t"+firstCliqueEnd+"\t"+actualTimestamp+"\n");
				//hitsOut.write("NO\t"+burstCount+"\n");
			}

			for (int d=0;d<days.length;d++)
				if (days[d]>0)
					predDays++;

			cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/actualCliques;
		}

		hitsOut.write('\n');
		hitsOut.flush();
		hitsOut.close();
		
		double sumDays = 0;
		double sumHitsPercentage = 0;
		//out.write("\nk\t" + kJaccard);

		iterations = docsTested;
		out.write("Tested:"+ iterations +"\tkJaccard: "+ kJaccard+"\tx: " +x+"\n");

		//out.write("\nClique\tHits\tExtra hits\tExtra Hits Percentage\tExtra Days\tGranularity\tHits Percentage");
		for (int k=0;k<kCliques;k++){
			sumDays+= (double)cliqueDays[k]/iterations;
			sumHitsPercentage += (double)distinctCliqueHits[k]/iterations; 


			out.write(k + "\t" + 
					cliqueHits[k]+ "\t" + 
					distinctCliqueHits[k] + "\t" + 
					(double)distinctCliqueHits[k]/iterations+ "\t" +
					(double)cliqueDays[k]/iterations + "\t" +
					sumDays + "\t" + 
					sumHitsPercentage+"\n");
		}

		out.write("\nReal hit percentage: " + (double)realHits/(iterations-noBursts));
		out.write("\nAvg predDays: " + (double)predDays/(iterations-noBursts));
		out.write("\nAvg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));

		out.close();
		dw.close();
	}


	public void compositeAlgorithm(String datasetName, String resultsFilename, int timespan, int termSelector, int kJaccard, int x, int ktfidf, Index lucIndex, int scoreType, int kCliques, boolean bursts) throws Exception{

		String [] tDocTerms = null, tDocBodyTerms = null;
		int iterations = this.testDocs.size();
		int[] distinctCliqueHits = null;
		int[] cliqueHits = null;
		int[] cliqueDays = null;
		int noBursts = 0, realHits = 0, cliqueLengthSumDatasetLevel = 0, predDays = 0;
		int actualCliquesSum = 0;

		if (kCliques!=-1){
			cliqueHits = new int[kCliques];
			distinctCliqueHits = new int[kCliques];
			cliqueDays = new int[kCliques];
		}	

		File resultsFile = new File(resultsFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFile, true),"UTF-8" ));	

		out.write("\nx\t"+ x +"\tk\t" + kJaccard +"\n");

		TokenizerFactory tokFactory = new IndoEuropeanTokenizerFactory();
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);
		File hitsFile = new File("burstHits.txt");
		BufferedWriter hitsOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hitsFile, true),"UTF-8" ));

		//For each testing document
		int j = 0;
		while (j<iterations){


			int cliqueLengthSumDocLevel=0;
			System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;


			int actualTimestamp = tDoc.getTimestamp();
			int[] days = new int[timeEnd-timeStart];

			//Discard very short documents
			if (tDoc.getBodyStr().length()<5)
				continue;

			tDocTerms = tDoc.getTopKTerms(ktfidf, lucIndex, this.dataset);
			tDocBodyTerms = tDoc.getBodysTerms();


			//3. Get the top-k most similar docs to the current document
			Document[] knndocs = lucIndex.getKNNBasedOnText(tDoc,tDocTerms, kJaccard+1);

			if (knndocs==null)
				continue;

			int bodySimDocCnt = 0;
			List<Document> bodySimDocs = new ArrayList<Document>();

			//4. Iterate over them
			int kDocs = Math.min(kJaccard, knndocs.length);
			for(int i = 0;i<kDocs;i++){
				Document tempDoc = knndocs[i];
				bodySimDocs.add(tempDoc);
				bodySimDocCnt++;
			}

			boolean realHit = false;
			System.out.print("AT: "+ actualTimestamp + " Cliques: ");

			if (bursts){

				double scoreArray[] = new double[bodySimDocs.size()];
				int docIt = 0;
				//For each doc in the top-k
				for (Document tempDoc : bodySimDocs){

					double docScore = 0;

					int docTimestamp = Integer.parseInt(tempDoc.get("timestamp"));
					//Find overlap
					String[] commonTerms = this.findCommonTerms(tDoc, tempDoc);


					//maybe tDocTerms here
					for (String term : commonTerms){

						ArrayList<BurstInterval> termBursts = this.terms.get(term);

						if (termBursts==null)
							continue;

						//Is there a burst at tempDoc timestamp?
						for (BurstInterval termBurst : termBursts){
							if (termBurst.getStart()<= docTimestamp && termBurst.getEnd()>=docTimestamp){
								docScore= docScore+1;
								break;
							}
						}


					}
					//maybe insert percentage here
					scoreArray[docIt] = docScore;
					//					for (String term : tDocTerms){
					//						if (this.terms.get(term)==null)
					//							continue;
					docIt++;

				}



				int actualCliques = 0;
				for (int i=0;i<kCliques;i++){

					if (bodySimDocs.size()==0)
						break;

					Wclique.createNewGraphFile(bodySimDocs, scoreArray,  datasetName +"graphs.txt", x);
					Wclique.testWclique(datasetName + "graphs.txt", datasetName + "cliques.txt");

					File cliqueFile = new File( datasetName + "cliques.txt");
					Reader rin = new InputStreamReader(new FileInputStream(cliqueFile), "UTF-8");
					BufferedReader in = new BufferedReader( rin );

					String line;
					while ((line = in.readLine()) != null){
						//19 19 19 19 19 19 59 87 142 116
						String[] cliqueString = line.split(" ");
						int[] cliqueNodes = new int[cliqueString.length];

						boolean validFile = true;
						for (int c=0;c<cliqueString.length;c++){

							if (isInteger(cliqueString[c]))
								cliqueNodes[c]= Integer.parseInt(cliqueString[c]);
							else{
								validFile = false;
								break;
							}
						}

						if (!validFile)
							break;

						cliqueNodes = toUniqueArray(cliqueNodes);

						int cliqueEnd = -1;
						int cliqueStart = Integer.MAX_VALUE;

						for (int c=0;c<cliqueNodes.length;c++){
							Document doc = bodySimDocs.get(cliqueNodes[c]);
							int docStamp = Integer.valueOf(doc.get("timestamp"));

							if (docStamp<cliqueStart)
								cliqueStart = docStamp;

							if (docStamp>cliqueEnd)
								cliqueEnd = docStamp;
						}	

						actualCliques++;
						actualCliquesSum++;
						//							cliqueStart-=2;
						//							cliqueEnd+=2;

						if (cliqueEnd-cliqueStart+1<x){
							int diff = x-(cliqueEnd-cliqueStart+1);
							int extend = diff/2;
							cliqueStart-= extend;
							cliqueEnd+= extend;
						}

						if (cliqueStart<0)
							cliqueStart = 0;
						if (cliqueEnd>timespan-1)
							cliqueEnd = timespan-1;

						cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);

						System.out.print("("+ cliqueStart +","+ cliqueEnd +") ");

						for (int d=cliqueStart; d<=cliqueEnd; d++){
							if (days[d]==0)
								days[d]=i+1;
						}

						//real hit
						if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
							cliqueHits[i]++;
							realHit = true;
							if (i==0){
								int hit = j-1;
								hitsOut.write(hit+",");
								hitsOut.flush();
							}
							System.out.print("RH!!!");
						}

						Arrays.sort(cliqueNodes);

						for (int c=0;c<cliqueNodes.length;c++)
							bodySimDocs.remove(cliqueNodes[cliqueNodes.length-1-c]);

					}

					in.close();
				}

				//if found
				if (days[actualTimestamp]>0)
					distinctCliqueHits[days[actualTimestamp]-1]++;

				for (int d=0;d<days.length;d++)
					if (days[d]>0)
						cliqueDays[days[d]-1]++;

				if (realHit){
					System.out.print("REAL HIT!!!!");
					realHits++;

				}



				for (int d=0;d<days.length;d++)
					if (days[d]>0)
						predDays++;

				cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/actualCliques;






			}
			else{

				//create documents graph and find all maximal cliques
				SimpleGraph<Document, DefaultEdge> sg = BurstySimDater.createJaccardGraph(bodySimDocs,x);
				BronKerboschCliqueFinder<Document, DefaultEdge> cf = new BronKerboschCliqueFinder<>(sg);
				List<Set<Document>> bodyCliques = (List<Set<Document>>) cf.getAllMaximalCliques();


				if (bodyCliques.isEmpty())
					continue;

				int[] rankedIndices = BurstySimDater.scoreAndRankJaccardCliques(bodyCliques, scoreType, this.terms);


				int lastClique;
				if (kCliques!=-1)
					lastClique = kCliques; 
				else
					lastClique = rankedIndices.length;

				if (lastClique>rankedIndices.length)
					lastClique = rankedIndices.length;

				int  cliqueStart, cliqueEnd;


				for (int i=0;i<lastClique;i++){

					Set<Document> tempClique = bodyCliques.get(rankedIndices[i]);

					// compute the clique interval
					int sumOfTimestamps = 0;

					cliqueEnd = -1;
					cliqueStart = Integer.MAX_VALUE;
					for (Document doc : tempClique){
						int docStamp = Integer.valueOf(doc.get("timestamp"));
						sumOfTimestamps+= docStamp;

						if (docStamp<cliqueStart)
							cliqueStart = docStamp;

						if (docStamp>cliqueEnd)
							cliqueEnd = docStamp;
					}
					//						cliqueStart-=1;
					//						cliqueEnd+=1;

					if ((cliqueEnd-cliqueStart+1)<30){
						int diff = cliqueEnd-cliqueStart+1;
						int rem = 30 - diff;
						cliqueStart-= rem/2;
						cliqueEnd+= rem/2;
					}

					if (cliqueStart<0)
						cliqueStart = 0;
					if (cliqueEnd>timespan-1)
						cliqueEnd = timespan-1;

					cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);

					System.out.print("("+ cliqueStart +","+ cliqueEnd +") ");

					for (int d=cliqueStart; d<=cliqueEnd; d++){
						if (days[d]==0)
							days[d]=i+1;
					}

					//real hit
					if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
						cliqueHits[i]++;
						realHit = true;
						System.out.print("RH!!!");
					}

				}

				//if found
				if (days[actualTimestamp]>0)
					distinctCliqueHits[days[actualTimestamp]-1]++;

				for (int d=0;d<days.length;d++)
					if (days[d]>0)
						cliqueDays[days[d]-1]++;

				if (realHit)
					System.out.print("REAL HIT!!!!");

				if (realHit)
					realHits++;

				for (int d=0;d<days.length;d++)
					if (days[d]>0)
						predDays++;

				cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/lastClique;
			}
			//			System.out.println(j+" sumDocLevel:"+ cliqueLengthSumDocLevel +" SumDatasetLevel:" + cliqueLengthSumDatasetLevel
			//					+ " AvgSumDatasetLevel:" + (double)cliqueLengthSumDatasetLevel/j 
			//					+ " predDays:" + predDays + " avgPredDays:" + (double)predDays/j);
		}

		System.out.println();

		hitsOut.write('\n');
		hitsOut.flush();
		hitsOut.close();

		double sumDays = 0;
		double sumHitsPercentage = 0;
		//out.write("\nk\t" + kJaccard);

		System.out.println("Clique\tHits\tExtra hits\tExtra Hits Percentage\tExtra Days\tGranularity\tHits Percentage");
		//out.write("\nClique\tHits\tExtra hits\tExtra Hits Percentage\tExtra Days\tGranularity\tHits Percentage");
		for (int k=0;k<kCliques;k++){
			sumDays+= (double)cliqueDays[k]/iterations;
			sumHitsPercentage += (double)distinctCliqueHits[k]/iterations; 
			System.out.println(
					k + "\t" + 
							cliqueHits[k]+ "\t" + 
							distinctCliqueHits[k] + "\t" + 
							(double)distinctCliqueHits[k]/iterations+ "\t" +
							(double)cliqueDays[k]/iterations + "\t" +
							sumDays + "\t" + 
							sumHitsPercentage);

			out.write(k + "\t" + 
					cliqueHits[k]+ "\t" + 
					distinctCliqueHits[k] + "\t" + 
					(double)distinctCliqueHits[k]/iterations+ "\t" +
					(double)cliqueDays[k]/iterations + "\t" +
					sumDays + "\t" + 
					sumHitsPercentage+"\n");
		}

		System.out.println();
		System.out.println("Real hit percentage: " + (double)realHits/(iterations-noBursts));
		System.out.println("Avg predDays: " + (double)predDays/(iterations-noBursts));
		System.out.println("Avg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));
		System.out.println("Avg cliques per doc: " + (double)actualCliquesSum/(iterations-noBursts));

		out.write("\nReal hit percentage: " + (double)realHits/(iterations-noBursts));
		out.write("\nAvg predDays: " + (double)predDays/(iterations-noBursts));
		out.write("\nAvg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));
		//		out.write("\n");

		out.close();
		System.out.println();
	}


	public void jimAlgorithm(String datasetName, String resultsFilename, int timespan, int termSelector, int kJaccard, int x, int ktfidf, Index lucIndex, int scoreType, int kCliques, boolean bursts) throws Exception{

		String [] tDocTerms = null, tDocBodyTerms = null;
		int iterations = this.testDocs.size();
		int[] distinctCliqueHits = null;
		int[] cliqueHits = null;
		int[] cliqueDays = null;
		int noBursts = 0, realHits = 0, cliqueLengthSumDatasetLevel = 0, predDays = 0;
		int actualCliquesSum = 0;

		if (kCliques!=-1){
			cliqueHits = new int[kCliques];
			distinctCliqueHits = new int[kCliques];
			cliqueDays = new int[kCliques];
		}	

		File resultsFile = new File(resultsFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFile, true),"UTF-8" ));	

		out.write("\nx\t"+ x +"\tk\t" + kJaccard +"\n");

		TokenizerFactory tokFactory = new IndoEuropeanTokenizerFactory();
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);

		//		TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
		//        JaccardDistance jaccard = new JaccardDistance(tokenizerFactory);

		File hitsFile = new File("burstHits.txt");
		BufferedWriter hitsOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hitsFile, true),"UTF-8" ));

		double lengthSum = 0;
		//For each testing document
		int j = 0;
		while (j<iterations){


			int cliqueLengthSumDocLevel=0;
			System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;


			int actualTimestamp = tDoc.getTimestamp();
			int[] days = new int[timeEnd-timeStart];

			//Discard very short documents
			if (tDoc.getBodyStr().length()<5)
				continue;

			tDocTerms = tDoc.getTopKTerms(ktfidf, lucIndex, this.dataset);
			tDocBodyTerms = tDoc.getBodysTerms();


			//3. Get the top-k most similar docs to the current document
			Document[] knndocs = lucIndex.getKNNBasedOnText(tDoc,tDocTerms, kJaccard+1);

			if (knndocs==null)
				continue;

			//find the jaccard similarity for each of the knn docs
			double [] jaccardScores = new double[timespan];
			int [] dayDocs = new int[timespan];
			int timeCounter = 0;
			int daysLength = 0;
			for(int i = 0; i < knndocs.length; i++){       

				int docTimestamp =  Integer.valueOf(knndocs[i].get("timestamp"));                
				//for each day get the sum of jaccard scores
				// double myDist = jaccardBodySimilarity(tDoc,knndocs[i]);
				double dist = jaccardD.proximity(tDoc.getBodyStr(),  knndocs[i].get("body"));//jaccard.distance(tDoc.getBodyStr(), knndocs[i].get("body"));

				if (Double.isNaN(dist))
					System.out.println("hi!");

				if (dist>=0.1){
					jaccardScores[docTimestamp]+= dist;
					dayDocs[docTimestamp]++;

				}
			}

			for (int i=0;i<timespan;i++)
				if (jaccardScores[i]>0)
					daysLength++;

			Vector<Float> seq = new Vector<Float>();
			for(int i = 0; i < jaccardScores.length; i++)
				if (dayDocs[i]==0)
					seq.add((float)0);
				else
					seq.add((float)jaccardScores[i]/dayDocs[i]);
			//seq.add((float)Math.pow(100*(float)jaccardScores[i]/dayDocs[i], 4));




			//  List<BurstInterval> daysBursts = BurstEngine.computeMAXKBurstsSplit(seq,1,150,false);

			boolean found = false;
			double length = 0;
			int mindist = timespan;
			float sum = 0;
			for (int i = 0; i<2; i++){
				if (actualTimestamp==0)
					sum+= seq.get(actualTimestamp+i);
				else
					sum+= seq.get(actualTimestamp+i) + seq.get(actualTimestamp-i);

			}
			if (sum>0)
				found = true;


			//            for (BurstInterval bint : daysBursts){
				//            	if (actualTimestamp>=bint.getStart() && actualTimestamp<=bint.getEnd()){
			//            		found = true;
			//            		mindist = 0;
			//            	}
			//            	int tempdist = Math.min(Math.abs(bint.getStart() - actualTimestamp), Math.abs(bint.getEnd() - actualTimestamp));
			//            	if(tempdist < mindist) mindist = tempdist;
			//            	length += bint.getEnd()-bint.getStart()+1;
			//            	
			//            	
			//            }

			if (found)
				System.out.println("FOUND!");
			else 
				System.out.println("min dist: " + mindist);

			System.out.println("Avg length:" + daysLength);

			lengthSum+=daysLength;

			//report the max burst of this as times created.
			//maybe create a function based on #of docs, number of bursty similar words and so on.
			//System.out.println("Burst count:" + daysBursts.size());
			System.out.println("Avg length:" + lengthSum/j);

			//			int bodySimDocCnt = 0;
			//			List<Document> bodySimDocs = new ArrayList<Document>();
			//
			//			//4. Iterate over them
			//			int kDocs = Math.min(kJaccard, knndocs.length);
			//			for(int i = 0;i<kDocs;i++){
			//				Document tempDoc = knndocs[i];
			//				bodySimDocs.add(tempDoc);
			//				bodySimDocCnt++;
			//			}
			//
			//			boolean realHit = false;
			//			System.out.print("AT: "+ actualTimestamp + " Cliques: ");
			//			
			//			if (bursts){
			//				
			//				double scoreArray[] = new double[bodySimDocs.size()];
			//				int docIt = 0;
			//				//For each doc in the top-k
			//				for (Document tempDoc : bodySimDocs){
			//					
			//					double docScore = 0;
			//					
			//					int docTimestamp = Integer.parseInt(tempDoc.get("timestamp"));
			//					//Find overlap
			//					String[] commonTerms = this.findCommonTerms(tDoc, tempDoc);
			//					
			//					
			//					//maybe tDocTerms here
			//					for (String term : commonTerms){
			//						
			//						ArrayList<BurstInterval> termBursts = this.terms.get(term);
			//						
			//						if (termBursts==null)
			//							continue;
			//						
			//						//Is there a burst at tempDoc timestamp?
			//						for (BurstInterval termBurst : termBursts){
			//							if (termBurst.getStart()<= docTimestamp && termBurst.getEnd()>=docTimestamp){
			//								docScore= docScore+1;
			//								break;
			//							}
			//						}
			//						
			//						
			//					}
			//					//maybe insert percentage here
			//					scoreArray[docIt] = docScore;
			////					for (String term : tDocTerms){
			////						if (this.terms.get(term)==null)
			////							continue;
			//					docIt++;
			//
			//					}
			//				
			//					
			//						
			//					int actualCliques = 0;
			//					for (int i=0;i<kCliques;i++){
			//						
			//						if (bodySimDocs.size()==0)
			//							break;
			//					
			//						Wclique.createNewGraphFile(bodySimDocs, scoreArray,  datasetName +"graphs.txt", x);
			//						Wclique.testWclique(datasetName + "graphs.txt", datasetName + "cliques.txt");
			//						
			//						File cliqueFile = new File( datasetName + "cliques.txt");
			//						Reader rin = new InputStreamReader(new FileInputStream(cliqueFile), "UTF-8");
			//						BufferedReader in = new BufferedReader( rin );
			//						
			//						String line;
			//						while ((line = in.readLine()) != null){
			//							//19 19 19 19 19 19 59 87 142 116
			//							String[] cliqueString = line.split(" ");
			//							int[] cliqueNodes = new int[cliqueString.length];
			//
			//							boolean validFile = true;
			//							for (int c=0;c<cliqueString.length;c++){
			//								
			//								if (isInteger(cliqueString[c]))
			//									cliqueNodes[c]= Integer.parseInt(cliqueString[c]);
			//								else{
			//									validFile = false;
			//									break;
			//								}
			//							}
			//							
			//							if (!validFile)
			//								break;
			//
			//							cliqueNodes = toUniqueArray(cliqueNodes);
			//
			//							int cliqueEnd = -1;
			//							int cliqueStart = Integer.MAX_VALUE;
			//							
			//							for (int c=0;c<cliqueNodes.length;c++){
			//								Document doc = bodySimDocs.get(cliqueNodes[c]);
			//								int docStamp = Integer.valueOf(doc.get("timestamp"));
			//								
			//								if (docStamp<cliqueStart)
			//									cliqueStart = docStamp;
			//								
			//								if (docStamp>cliqueEnd)
			//									cliqueEnd = docStamp;
			//							}	
			//
			//							actualCliques++;
			//							actualCliquesSum++;
			////							cliqueStart-=2;
			////							cliqueEnd+=2;
			//							
			//							if (cliqueEnd-cliqueStart+1<x){
			//								int diff = x-(cliqueEnd-cliqueStart+1);
			//								int extend = diff/2;
			//								cliqueStart-= extend;
			//								cliqueEnd+= extend;
			//							}
			//							
			//							if (cliqueStart<0)
			//								cliqueStart = 0;
			//							if (cliqueEnd>timespan-1)
			//								cliqueEnd = timespan-1;
			//							
			//							cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);
			//							
			//							System.out.print("("+ cliqueStart +","+ cliqueEnd +") ");
			//			
			//							for (int d=cliqueStart; d<=cliqueEnd; d++){
			//								if (days[d]==0)
			//									days[d]=i+1;
			//							}
			//			
			//							//real hit
			//							if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
			//								cliqueHits[i]++;
			//								realHit = true;
			//								if (i==0){
			//									int hit = j-1;
			//									hitsOut.write(hit+",");
			//									hitsOut.flush();
			//								}
			//								System.out.print("RH!!!");
			//							}
			//							
			//							Arrays.sort(cliqueNodes);
			//
			//							for (int c=0;c<cliqueNodes.length;c++)
			//								bodySimDocs.remove(cliqueNodes[cliqueNodes.length-1-c]);
			//
			//						}
			//						
			//						in.close();
			//					}
			//					
			//					//if found
			//					if (days[actualTimestamp]>0)
			//						distinctCliqueHits[days[actualTimestamp]-1]++;
			//					
			//					for (int d=0;d<days.length;d++)
			//						if (days[d]>0)
			//							cliqueDays[days[d]-1]++;
			//		
			//					if (realHit){
			//						System.out.print("REAL HIT!!!!");
			//						realHits++;
			//						
			//					}
			//					
			//		
			//		
			//					for (int d=0;d<days.length;d++)
			//						if (days[d]>0)
			//							predDays++;
			//		
			//					cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/actualCliques;
			//
			//						
			//				
			//				
			//				
			//				
			//				}
			//				else{
			//
			//					//create documents graph and find all maximal cliques
			//					SimpleGraph<Document, DefaultEdge> sg = CheckBurstiness.createJaccardGraph(bodySimDocs,x);
			//					BronKerboschCliqueFinder<Document, DefaultEdge> cf = new BronKerboschCliqueFinder<>(sg);
			//					List<Set<Document>> bodyCliques = (List<Set<Document>>) cf.getAllMaximalCliques();
			//
			//			
			//					if (bodyCliques.isEmpty())
			//						continue;
			//					
			//					int[] rankedIndices = CheckBurstiness.scoreAndRankJaccardCliques(bodyCliques, scoreType, this.terms);
			//		
			//		
			//					int lastClique;
			//					if (kCliques!=-1)
			//						lastClique = kCliques; 
			//					else
			//						lastClique = rankedIndices.length;
			//		
			//					if (lastClique>rankedIndices.length)
			//						lastClique = rankedIndices.length;
			//		
			//					int  cliqueStart, cliqueEnd;
			//		
			//
			//					for (int i=0;i<lastClique;i++){
			//		
			//						Set<Document> tempClique = bodyCliques.get(rankedIndices[i]);
			//		
			//						// compute the clique interval
			//						int sumOfTimestamps = 0;
			//		
			//						cliqueEnd = -1;
			//						cliqueStart = Integer.MAX_VALUE;
			//						for (Document doc : tempClique){
			//							int docStamp = Integer.valueOf(doc.get("timestamp"));
			//							sumOfTimestamps+= docStamp;
			//							
			//							if (docStamp<cliqueStart)
			//								cliqueStart = docStamp;
			//							
			//							if (docStamp>cliqueEnd)
			//								cliqueEnd = docStamp;
			//						}
			////						cliqueStart-=1;
			////						cliqueEnd+=1;
			//						
			//						if ((cliqueEnd-cliqueStart+1)<30){
			//							int diff = cliqueEnd-cliqueStart+1;
			//							int rem = 30 - diff;
			//							cliqueStart-= rem/2;
			//							cliqueEnd+= rem/2;
			//						}
			//						
			//						if (cliqueStart<0)
			//							cliqueStart = 0;
			//						if (cliqueEnd>timespan-1)
			//							cliqueEnd = timespan-1;
			//						
			//						cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);
			//		
			//						System.out.print("("+ cliqueStart +","+ cliqueEnd +") ");
			//		
			//						for (int d=cliqueStart; d<=cliqueEnd; d++){
			//							if (days[d]==0)
			//								days[d]=i+1;
			//						}
			//		
			//						//real hit
			//						if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
			//							cliqueHits[i]++;
			//							realHit = true;
			//							System.out.print("RH!!!");
			//						}
			//		
			//					}
			//				
			//					//if found
			//					if (days[actualTimestamp]>0)
			//						distinctCliqueHits[days[actualTimestamp]-1]++;
			//					
			//					for (int d=0;d<days.length;d++)
			//						if (days[d]>0)
			//							cliqueDays[days[d]-1]++;
			//		
			//					if (realHit)
			//						System.out.print("REAL HIT!!!!");
			//					
			//					if (realHit)
			//						realHits++;
			//		
			//					for (int d=0;d<days.length;d++)
			//						if (days[d]>0)
			//							predDays++;
			//		
			//					cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/lastClique;
			//				}
			//			System.out.println(j+" sumDocLevel:"+ cliqueLengthSumDocLevel +" SumDatasetLevel:" + cliqueLengthSumDatasetLevel
			//					+ " AvgSumDatasetLevel:" + (double)cliqueLengthSumDatasetLevel/j 
			//					+ " predDays:" + predDays + " avgPredDays:" + (double)predDays/j);
		}

		System.out.println();

		hitsOut.write('\n');
		hitsOut.flush();
		hitsOut.close();

		double sumDays = 0;
		double sumHitsPercentage = 0;
		//out.write("\nk\t" + kJaccard);

		System.out.println("Clique\tHits\tExtra hits\tExtra Hits Percentage\tExtra Days\tGranularity\tHits Percentage");
		//out.write("\nClique\tHits\tExtra hits\tExtra Hits Percentage\tExtra Days\tGranularity\tHits Percentage");
		for (int k=0;k<kCliques;k++){
			sumDays+= (double)cliqueDays[k]/iterations;
			sumHitsPercentage += (double)distinctCliqueHits[k]/iterations; 
			System.out.println(
					k + "\t" + 
							cliqueHits[k]+ "\t" + 
							distinctCliqueHits[k] + "\t" + 
							(double)distinctCliqueHits[k]/iterations+ "\t" +
							(double)cliqueDays[k]/iterations + "\t" +
							sumDays + "\t" + 
							sumHitsPercentage);

			out.write(k + "\t" + 
					cliqueHits[k]+ "\t" + 
					distinctCliqueHits[k] + "\t" + 
					(double)distinctCliqueHits[k]/iterations+ "\t" +
					(double)cliqueDays[k]/iterations + "\t" +
					sumDays + "\t" + 
					sumHitsPercentage+"\n");
		}

		System.out.println();
		System.out.println("Real hit percentage: " + (double)realHits/(iterations-noBursts));
		System.out.println("Avg predDays: " + (double)predDays/(iterations-noBursts));
		System.out.println("Avg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));
		System.out.println("Avg cliques per doc: " + (double)actualCliquesSum/(iterations-noBursts));

		out.write("\nReal hit percentage: " + (double)realHits/(iterations-noBursts));
		out.write("\nAvg predDays: " + (double)predDays/(iterations-noBursts));
		out.write("\nAvg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));
		//		out.write("\n");

		out.close();
		System.out.println();
	}

	private String[] findCommonTerms(TDocument tDoc, Document tempDoc) {

		String[] tdoc1TermsArray = tDoc.getBodyTerms();
		String[] tdoc2TermsArray = tempDoc.get("body").split(" ");

		List<String> tdoc1Terms = Arrays.asList(tdoc1TermsArray);
		List<String> tdoc2Terms = Arrays.asList(tdoc2TermsArray);

		List<String> stuff = new ArrayList<String>();
		stuff.addAll(tdoc1Terms);




		stuff.retainAll(tdoc2Terms);

		String[] stockArr = new String[stuff.size()];

		stockArr = stuff.toArray(stockArr);
		return stockArr;

	}










	public void simThresJaccardAlgorithm(String resultsFilename, int timespan, int termSelector, int kJaccard, int x, int ktfidf, Index lucIndex, int scoreType, int kCliques) throws IOException, ParseException{

		String [] tDocTerms = null, tDocBodyTerms = null;
		int iterations = this.testDocs.size();
		int[] distinctCliqueHits = null;
		int[] cliqueHits = null;
		int[] cliqueDays = null;
		int noBursts = 0, realHits = 0, cliqueLengthSumDatasetLevel = 0, predDays = 0;

		if (kCliques!=-1){
			cliqueHits = new int[kCliques];
			distinctCliqueHits = new int[kCliques];
			cliqueDays = new int[kCliques];
		}	

		File resultsFile = new File(resultsFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFile, true),"UTF-8" ));	

		TokenizerFactory tokFactory = new IndoEuropeanTokenizerFactory();
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);
		//For each testing document


		int j = 0;

		int totalSimDocCnt = 0, totalCliqueCnt = 0;
		while (j<iterations){


			int cliqueLengthSumDocLevel=0;
			System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;

			int[] days = new int[timeEnd-timeStart];
			//Discard very short documents
			if (tDoc.getBodyStr().length()<5)
				continue;

			tDocTerms = tDoc.getTopKTerms(ktfidf, lucIndex, this.dataset);
			tDocBodyTerms = tDoc.getBodysTerms();


			//3. Get the top-k most similar docs to the current document
			Document[] knndocs = lucIndex.getKNNBasedOnText(tDoc,tDocTerms, 2000);

			if (knndocs==null)
				continue;

			int bodySimDocCnt = 0;

			List<Document> titleSimDocs = new ArrayList<Document>();
			List<Document> bodySimDocs = new ArrayList<Document>();
			//4. Iterate over them
			for(int i = 0;i<kJaccard;i++){
				Document tempDoc = knndocs[i];
				double jaccardDist = jaccardD.proximity(tDoc.getBodyStr(), tempDoc.get("body"));
				//double jaccardBodySim = jaccardBodySimilarity(tDoc, tempDoc);
				//				if (jaccardDist>=minJaccard){
				//					bodySimDocs.add(tempDoc);
				//					bodySimDocCnt++;
				//				}
				bodySimDocs.add(tempDoc);
				bodySimDocCnt++;
			}


			totalSimDocCnt+= bodySimDocCnt;
			//Get a reasonable interval arround the actual creation timestamp (+- 10-15 days)
			int actualTimestamp = tDoc.getTimestamp();


			//create documents graph and find all maximal cliques
			SimpleGraph<Document, DefaultEdge> sg = BurstySimDater.createJaccardGraph(bodySimDocs,x);
			BronKerboschCliqueFinder<Document, DefaultEdge> cf = new BronKerboschCliqueFinder<>(sg);
			List<Set<Document>> bodyCliques = (List<Set<Document>>) cf.getAllMaximalCliques();


			if (bodyCliques.isEmpty())
				continue;

			totalCliqueCnt+= bodyCliques.size();
			int[] rankedIndices = BurstySimDater.scoreAndRankJaccardCliques(bodyCliques, scoreType, this.terms);


			int lastClique;
			if (kCliques!=-1)
				lastClique = kCliques; 
			else
				lastClique = rankedIndices.length;

			if (lastClique>rankedIndices.length)
				lastClique = rankedIndices.length;

			int  cliqueStart, cliqueEnd;

			System.out.print("AT: "+ actualTimestamp + " Cliques: ");
			boolean realHit = false;


			for (int i=0;i<lastClique;i++){

				Set<Document> tempClique = bodyCliques.get(rankedIndices[i]);

				// compute the clique interval
				int sumOfTimestamps = 0;

				cliqueEnd = -1;
				cliqueStart = Integer.MAX_VALUE;
				for (Document doc : tempClique){
					int docStamp = Integer.valueOf(doc.get("timestamp"));
					sumOfTimestamps+= docStamp;

					if (docStamp<cliqueStart)
						cliqueStart = docStamp;

					if (docStamp>cliqueEnd)
						cliqueEnd = docStamp;

				}


				double medoid = (double)sumOfTimestamps/tempClique.size();

				//				cliqueStart=(int)Math.ceil(medoid)-extendLength;
				//				cliqueEnd=(int)Math.ceil(medoid)+extendLength;

				cliqueStart-=1;
				cliqueEnd+=1;

				if (cliqueStart<0)
					cliqueStart = 0;
				if (cliqueEnd>timespan-1)
					cliqueEnd = timespan-1;

				cliqueLengthSumDocLevel+= (cliqueEnd - cliqueStart + 1);

				System.out.print("("+ cliqueStart +","+ cliqueEnd +") ");

				for (int d=cliqueStart; d<=cliqueEnd; d++){
					if (days[d]==0)
						days[d]=i+1;
				}

				//real hit
				if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
					cliqueHits[i]++;
					realHit = true;
					System.out.print("RH!!!");
				}

			}


			//if found
			if (days[actualTimestamp]>0)
				distinctCliqueHits[days[actualTimestamp]-1]++;

			for (int d=0;d<days.length;d++)
				if (days[d]>0)
					cliqueDays[days[d]-1]++;

			if (realHit)
				System.out.print("REAL HIT!!!!");

			if (realHit)
				realHits++;

			for (int d=0;d<days.length;d++)
				if (days[d]>0)
					predDays++;

			cliqueLengthSumDatasetLevel+= (double)cliqueLengthSumDocLevel/lastClique;

			//			System.out.println(j+" sumDocLevel:"+ cliqueLengthSumDocLevel +" SumDatasetLevel:" + cliqueLengthSumDatasetLevel
			//					+ " AvgSumDatasetLevel:" + (double)cliqueLengthSumDatasetLevel/j 
			//					+ " predDays:" + predDays + " avgPredDays:" + (double)predDays/j);
		}

		System.out.println();

		//		System.out.println("Clique\tHits\textra hits\textradays");
		//		for (int k=0;k<kCliques;k++)
		//			System.out.println(k + "\t" + cliqueHits[k]+"\t"+distinctCliqueHits[k]+"\t"+(double)cliqueDays[k]/iterations);


		double sumDays = 0;
		double sumHitsPercentage = 0;

		System.out.println("Clique\tHits\tExtra hits\tExtra Hits Percentage\tExtra Days\tGranularity\tHits Percentage");
		for (int k=0;k<kCliques;k++){
			sumDays+= (double)cliqueDays[k]/iterations;
			sumHitsPercentage += (double)distinctCliqueHits[k]/iterations; 
			System.out.println(
					k + "\t" + 
							cliqueHits[k]+ "\t" + 
							distinctCliqueHits[k] + "\t" + 
							(double)distinctCliqueHits[k]/iterations+ "\t" +
							(double)cliqueDays[k]/iterations + "\t" +
							sumDays + "\t" + 
							sumHitsPercentage);

			out.write(k + "\t" + 
					cliqueHits[k]+ "\t" + 
					distinctCliqueHits[k] + "\t" + 
					(double)distinctCliqueHits[k]/iterations+ "\t" +
					(double)cliqueDays[k]/iterations + "\t" +
					sumDays + "\t" + 
					sumHitsPercentage+"\n");
		}

		System.out.println();

		System.out.println("Real hit percentage: " + (double)realHits/(iterations-noBursts));
		System.out.println("Avg predDays: " + (double)predDays/(iterations-noBursts));
		System.out.println("Avg simDocCnt: " + (double)totalSimDocCnt/(iterations-noBursts));
		System.out.println("Avg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));

		out.write("\nk Jaccard: " + kJaccard);
		out.write("\nReal hit percentage: " + (double)realHits/(iterations-noBursts));
		out.write("\nAvg predDays: " + (double)predDays/(iterations-noBursts));
		out.write("\nAvg simDocCnt: " + (double)totalSimDocCnt/(iterations-noBursts));
		out.write("\nAvg cliqueCnt: " + (double)totalCliqueCnt/(iterations-noBursts));
		out.write("\nAvg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSumDatasetLevel/(iterations-noBursts));
		out.write("\n");

		out.close();
		System.out.println();
	}

	













	private void printknnDocs(TDocument tDoc, Document[] knnDocs, Index lucIndex, BufferedWriter out) throws IOException {

		String date = tDoc.getDateStr();
		String title = tDoc.getTitleStr();
		String body = tDoc.getBodyStr();

		out.write(date + "\t" + title + "\t" + body + "\n\n");

		for (Document d : knnDocs)			
			out.write(d.get("timestamp") + "\t" + d.get("docId") + "\t" + d.get("body") + "\n");

		out.write("\n\n\n");

	}








	private static int[] scoreAndRankJaccardCliques(List<Set<Document>> cliques,
			int scoreType, HashMap<String, ArrayList<BurstInterval>> bursts) {
		//create and initialize the indices array. Will be used for scoring and ranking the cliques. 
		int[] indices = new int[cliques.size()];

		for (int i=0;i<indices.length;i++)
			indices[i]=i;

		//create the array for the scores of the cliques
		double[] cliqueScores = new double[cliques.size()];

		//compute the score for each clique			
		int i = 0;
		for (Set<Document> tempClique : cliques)
			cliqueScores[i++]= BurstySimDater.computeJaccardCliqueScore(tempClique);

		//sort the scores of the cliques
		Sorter.quicksort(cliqueScores, indices);

		return indices;
	}

	private static int[] scoreAndRankCliques(List<Set<BurstInterval>> cliques,
			int scoreType, HashMap<String, ArrayList<BurstInterval>> bursts) {
		//create and initialize the indices array. Will be used for scoring and ranking the cliques. 
		int[] indices = new int[cliques.size()];

		for (int i=0;i<indices.length;i++)
			indices[i]=i;

		//create the array for the scores of the cliques
		double[] cliqueScores = new double[cliques.size()];

		//compute the score for each clique			
		int i = 0;
		for (Set<BurstInterval> tempClique : cliques)
			cliqueScores[i++]= BurstySimDater.computeCliqueScore(tempClique, scoreType, bursts);

		//sort the scores of the cliques
		Sorter.quicksort(cliqueScores, indices);

		return indices;
	}










	public void checkAlgorithm(int extendLength, String outFilename, Index lucIndex, 
			int K, boolean randomInterval, int scoreType, boolean weightScores, int p, int kNN, String[] topics, int termSelector) throws Exception{

		File resultsFile = new File(outFilename);
		FileWriter fw = new FileWriter(resultsFile);
		BufferedWriter out = new BufferedWriter(fw);

		String [] tDocTerms = null;

		//Do as many iterations as the number of the testdocs
		int j = 0;
		int iterations = this.testDocs.size();

		int sumOfCommonTerms = 0;
		int noBursts = 0;
		int manybursts = 0;
		int overlapsSum = 0;  
		int overlapTermsSum = 0;

		double avgPercentageSum = 0;


		int[] cliqueSizes = new int[K];

		int[] kRankedCliqueHits = new int[500];

		int kCliques = 3;
		int[] cliqueHits = new int[kCliques];
		int realHits = 0;
		int extendedHits = 0;
		double cliqueLengthSum = 0;
		int textKnnHits=0;
		int knnAndCliquesHits = 0;
		//For each testing document
		while (j<iterations){


			System.out.println(j);
			TDocument tDoc = this.testDocs.get(j);
			j++;


			if (tDoc.getBodyStr().length()<5)
				continue;

			//Get a reasonable interval arround the actual creation timestamp (+- 10-15 days)
			int actualFrom = tDoc.getTimestamp()-extendLength;
			int actualTo = tDoc.getTimestamp()+extendLength;


			//In case a random test is needed
			if (randomInterval){
				int randomCenterPoint = (int) (Math.random() * ( 730 - 0 ));
				actualFrom = randomCenterPoint-extendLength;
				actualTo = randomCenterPoint+extendLength;
			}

			//Adjust the actual interval
			if (actualFrom<0)
				actualFrom = 0;
			if (actualTo>729)
				actualTo = 729;

			// holds the number of overlaps between a burst in the rep. terms and the actual interval
			int overlapsCnt = 0;

			// how many terms have an overlapping burst with the actual interval 
			int overlapTermsCnt = 0;


			tDocTerms = tDoc.getTopKTerms(K, lucIndex, this.dataset);

			// get the rep terms of the doc according to some selection criterion
			// tDocTerms are the intersection of the LDA topic terms and the terms in the document BODY. 

			//			String[] docTopicTerms = topics[j-1].split(" ");
			//			String[] docTerms = tDoc.getBodyTerms();
			//			ArrayList<String> commonTerms = this.getIntersectionTopicDocTerms(docTopicTerms, docTerms);
			//			
			//			sumOfCommonTerms+= commonTerms.size();
			//			
			//			tDocTerms = new String[commonTerms.size()];
			//			commonTerms.toArray(tDocTerms);


			// get the bursts for the rep terms
			ArrayList<BurstInterval> burstList = new ArrayList<BurstInterval>();

			//int totalBurstCnt = 0;
			double burstPercentageSum = 0;

			// for each rep term
			for (String term : tDocTerms){
				if (this.terms.get(term)==null)
					continue;

				// get its bursts
				ArrayList<BurstInterval> bursts = this.terms.get(term);

				int burstyDays = 0;
				for (BurstInterval bint : bursts){
					int length = bint.getEnd()-bint.getStart()+1;
					burstyDays+= length;
				}

				burstPercentageSum += (double)burstyDays/730;
				//totalBurstCnt+= this.terms.get(term).size();

				//int burstCnt = this.terms.get(term).size();

				//double score =Math.pow((double)totalBurstCnt/burstCnt,3);
				//				for (BurstInterval bint : bursts)
				//					bint.setScore(score);
				//				
				burstList.addAll(bursts);
			}

			double tempValue = burstPercentageSum/tDocTerms.length;

			if (!Double.isNaN(tempValue))
				avgPercentageSum+= tempValue;

			if (burstList.isEmpty()){
				noBursts++;
				continue;
			}

			if (burstList.size()>=10000){
				manybursts++;
				continue;
			}

			// create bursty interval graph and find all maximal cliques
			SimpleGraph<BurstInterval, DefaultEdge> sg = BurstySimDater.createGraph(burstList);
			BronKerboschCliqueFinder<BurstInterval, DefaultEdge> cf = new BronKerboschCliqueFinder<>(sg);
			List<Set<BurstInterval>> cliques = (List<Set<BurstInterval>>) cf.getAllMaximalCliques();



			//get knn documents


			Document[] knnDocs = lucIndex.getKNNBasedOnText(tDoc, tDocTerms, kNN);


			//See if text-only-based knn works well

			HashMap<Integer, Integer> tsCounts = new HashMap<Integer, Integer>();


			int[] knnTimestamps = new int[kNN];
			int l = 0;
			for (Document d : knnDocs){

				if (d==null)
					break;

				int tempTs = Integer.parseInt(d.get("timestamp"));
				knnTimestamps[l++] = tempTs;
			}

			for (int ts : knnTimestamps){
				if (tsCounts.get(ts)==null)
					tsCounts.put(ts, 1);
				else{
					int newCnt = tsCounts.get(ts) + 1;
					tsCounts.put(ts, newCnt);
				}
			}

			int maxCnt = 0;
			int maxTs = 0;
			for  (int ts : knnTimestamps){
				if (tsCounts.get(ts)>maxCnt){
					maxTs = ts;
					maxCnt = tsCounts.get(ts);
				}

			}

			int actualTimestamp = tDoc.getTimestamp();

			if (maxTs+extendLength>= actualTimestamp && maxTs-extendLength<= actualTimestamp)
				textKnnHits++;
			int predFrom = maxTs-extendLength;
			int predTo = maxTs+extendLength;

			System.out.println("Predicted interval: ("+predFrom+","+predTo+"), maxTs:"+maxTs+" with: "+maxCnt +" voters! Actual ts: "+ actualTimestamp);








			//create and initialize the indices array. Will be used for scoring and ranking the cliques. 
			int[] indices = new int[cliques.size()];
			for (int i=0;i<indices.length;i++)
				indices[i]=i;




			int[] cliqueVotes = new int[cliques.size()];



			for (int cliqueCnt = 0; cliqueCnt<cliques.size(); cliqueCnt++){

				Set<BurstInterval> tempClique = cliques.get(cliqueCnt);
				// compute the clique interval
				int cliqueStart = -1;
				int cliqueEnd = Integer.MAX_VALUE;

				for (BurstInterval bint : tempClique){
					if (bint.getStart()>cliqueStart)
						cliqueStart = bint.getStart();

					if (bint.getEnd()<cliqueEnd)
						cliqueEnd = bint.getEnd();
				}


				for (int i=0; i<kNN;i++)
					if (knnTimestamps[i]>= cliqueStart && knnTimestamps[i]<= cliqueEnd)
						cliqueVotes[cliqueCnt]++;

			}

			int maxVoteCnt = cliqueVotes[0];
			int maxVotedClique = 0;

			for  (int i=1; i<cliqueVotes.length;i++){

				int tmpVoteCnt = cliqueVotes[i];

				if (tmpVoteCnt> maxVoteCnt){
					maxVoteCnt = tmpVoteCnt;
					maxVotedClique = i;
				}
			}


			Set<BurstInterval> selectedClique = cliques.get(maxVotedClique);
			// compute the clique interval
			int cliqueStart = -1;
			int cliqueEnd = Integer.MAX_VALUE;

			for (BurstInterval bint : selectedClique){
				if (bint.getStart()>cliqueStart)
					cliqueStart = bint.getStart();

				if (bint.getEnd()<cliqueEnd)
					cliqueEnd = bint.getEnd();
			}


			//real hit
			if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
				knnAndCliquesHits++;
			}








			//create the array for the scores of the cliques
			double[] cliqueScores = new double[cliques.size()];

			//compute the score for each clique			
			int i = 0;
			for (Set<BurstInterval> tempClique : cliques)
				cliqueScores[i++]=BurstySimDater.computeCliqueScore(tempClique, scoreType,  this.terms);

			//sort the scores of the cliques
			Sorter.quicksort(cliqueScores, indices);

			int end = indices.length;
			end = kCliques;

			int cliqueIntervalLengthSum = 0;

			//int [] cliqueHits = new int[end];
			//check the first kCliques 
			for (i=0;i<end;i++){

				Set<BurstInterval> tempClique = cliques.get(indices[i]);

				// compute the clique interval
				cliqueStart = -1;
				cliqueEnd = Integer.MAX_VALUE;

				for (BurstInterval bint : tempClique){
					if (bint.getStart()>cliqueStart)
						cliqueStart = bint.getStart();

					if (bint.getEnd()<cliqueEnd)
						cliqueEnd = bint.getEnd();
				}

				int size = tempClique.size();
				cliqueIntervalLengthSum+= (cliqueEnd - cliqueStart + 1);

				//real hit
				if (actualTimestamp <= cliqueEnd && actualTimestamp>=cliqueStart){
					realHits++;
					if (i<500)
						kRankedCliqueHits[i]++;
				}

				//extended hit
				if (overlaps(actualFrom, actualTo, cliqueStart, cliqueEnd, 0)){
					cliqueHits[i]++;
					extendedHits++;
				}

				if (size<=K)
					cliqueSizes[size-1]++;
			}

			cliqueLengthSum+= (double)cliqueIntervalLengthSum/end;

			ArrayList<String> overlappingTerms = new ArrayList<String>();
			ArrayList<BurstInterval> overlappingBursts = new ArrayList<BurstInterval>();
			for (BurstInterval bint : burstList){

				if (BurstySimDater.overlaps(bint.getStart(), bint.getEnd(), actualFrom, actualTo, 0)){
					overlapsCnt++;
					overlappingBursts.add(bint);


					if (!overlappingTerms.contains(bint.getTerm()))
						overlappingTerms.add(bint.getTerm());
				}
			}


			overlapTermsCnt = overlappingTerms.size();

			overlapTermsSum += overlapTermsCnt;
			overlapsSum+= overlapsCnt;



			out.write(tDoc.getDateStr()+ "\t" +
					tDoc.getTitleStr() + "\t" + 
					tDoc.getBodyStr() + "\t" + 
					"t:"+tDoc.getTimestamp() + "\t" +
					"overlaps:"+overlapsCnt + "\t" +
					"interval:"+actualFrom+'-' + actualTo + "\t");

			for (BurstInterval bint : overlappingBursts){
				out.write(bint.getTerm()+":"+bint.getStart()+"-"+bint.getEnd()+", ");
			}

			out.write("\n");


		}

		for (int k=0;k<K;k++){
			System.out.println("Avg " + (double)cliqueSizes[k]/iterations + " of size " + (k+1));
		}

		System.out.println("\n\n");
		for (int k=0;k<kCliques;k++)
			System.out.println(k + "-th clique hits: " + cliqueHits[k]);



		System.out.println("k\thits");
		for (int k=0;k<500;k++){
			System.out.println(kRankedCliqueHits[k]+ "\t" + k);
		}




		System.out.println("Many bursts: " + manybursts);
		System.out.println("No bursts: " + noBursts);
		System.out.println("Real hit percentage: " + (double)realHits/(iterations-noBursts));
		System.out.println("Text knn percentage: " + (double)textKnnHits/(iterations-noBursts));
		System.out.println("Text knn AND cliques percentage: " + (double)knnAndCliquesHits/(iterations-noBursts));

		System.out.println("Extended hit percentage: " + (double)extendedHits/(iterations-noBursts));
		System.out.println("Avg length of the first " + kCliques + " cliques: " + (double)cliqueLengthSum/(iterations-noBursts));
		System.out.println("Avg common terms (LDA + TF): " + (double)sumOfCommonTerms/(iterations-noBursts));
		System.out.println("Burst percentage of top-k terms: " + (double)avgPercentageSum/(iterations-noBursts));
		System.out.println("Overlaps per doc: " + (double)overlapsSum/(iterations-noBursts));
		System.out.println("Overlaping terms per doc: " + (double)overlapTermsSum/(iterations-noBursts));


		System.out.println();
	}

	private ArrayList<String> getIntersectionTopicDocTerms(String[] topicTerms, String[] docTerms){
		ArrayList<String> terms = new ArrayList<String>();

		for (String topicTerm : topicTerms){
			for (String docTerm : docTerms){
				if (docTerm.equals(topicTerm))
					terms.add(docTerm);
			}
		}

		return terms;
	}
	private static double computeCliqueScore(Set<BurstInterval> tempClique, int scoreType, HashMap<String, ArrayList<BurstInterval>> terms ){
		double maxscore = -1;
		double minscore = Double.MAX_VALUE;	
		double sumscore = 0;


		double score = 0;
		int totalBurstCount = 0;

		for (BurstInterval bint : tempClique){
			String term = bint.getTerm();
			int termBurstCnt = terms.get(term).size();
			totalBurstCount+=termBurstCnt;
		}

		for (BurstInterval bint : tempClique){

			String term = bint.getTerm();

			int termBurstCnt = terms.get(term).size();

			double bintScore;


			bintScore = bint.getScore();

			if (bintScore>maxscore)
				maxscore = bintScore;

			if (bintScore>minscore)
				minscore = bintScore;

			sumscore+= bintScore;
		}


		if (scoreType==BurstySimDater.AVGSCORE)
			score = -sumscore/tempClique.size();
		else if (scoreType==BurstySimDater.MAXSCORE)
			score = -maxscore;
		else if (scoreType==BurstySimDater.MEDIANSCORE)
			score = (maxscore-minscore)/2;
		else if (scoreType==BurstySimDater.NODECNTSCORE)
			score = -tempClique.size();
		else 
			score = -sumscore;

		return score;
	}




	private static double computeJaccardCliqueScore(Set<Document> tempClique){

		return -tempClique.size();


	}



//	private static SimpleGraph<Interval, DefaultEdge> createIntervalGraph(List<Interval> intervalList){
//
//
//		SimpleGraph<Interval, DefaultEdge> sg = new SimpleGraph<Interval, DefaultEdge>(DefaultEdge.class); 
//
//		for (Interval bint : intervalList)
//			sg.addVertex(bint);
//
//
//		List<Interval> itBurstList = intervalList;
//
//		Iterator<Interval> it1 = itBurstList.iterator();
//		while (it1.hasNext()){
//
//			Interval bint1 = (Interval)it1.next();
//			Iterator<Interval> it2 = itBurstList.iterator();
//
//			while (it2.hasNext()){
//				Interval bint2 = (Interval)it2.next();
//
//				//if there is NOT an edge between the two vertices 
//
//				if (bint1 == bint2)
//					continue;
//
//				if (BurstySimDater.overlaps(bint1, bint2, 0))
//
//					if (sg.getEdge(bint1, bint2)==null && sg.getEdge(bint2, bint1)==null )
//						sg.addEdge(bint1, bint2);
//
//			}	
//
//		}
//
//
//
//		return sg;
//
//	}


	private static SimpleGraph<Document, DefaultEdge> createJaccardGraph(List<Document> simDocs, int x){


		SimpleGraph<Document, DefaultEdge> sg = new SimpleGraph<Document, DefaultEdge>(DefaultEdge.class); 

		for (Document doc : simDocs)
			sg.addVertex(doc);


		List<Document> itBurstList = simDocs;

		Iterator<Document> it1 = itBurstList.iterator();
		while (it1.hasNext()){

			Document doc1 = (Document)it1.next();
			Iterator<Document> it2 = itBurstList.iterator();

			int doc1timestamp = Integer.valueOf(doc1.get("timestamp"));
			while (it2.hasNext()){
				Document doc2 = (Document)it2.next();

				//if there is NOT an edge between the two vertices 

				if (doc1 == doc2)
					continue;

				int doc2timestamp = Integer.valueOf(doc2.get("timestamp"));

				if (Math.abs(doc1timestamp-doc2timestamp)<=x)

					if (sg.getEdge(doc1, doc2)==null && sg.getEdge(doc2, doc1)==null )
						sg.addEdge(doc1, doc2);

			}	

		}



		return sg;

	}



	private static SimpleGraph<BurstInterval, DefaultEdge> createGraph(ArrayList<BurstInterval> burstList){


		SimpleGraph<BurstInterval, DefaultEdge> sg = new SimpleGraph<BurstInterval, DefaultEdge>(DefaultEdge.class); 

		for (BurstInterval bint : burstList)
			sg.addVertex(bint);


		List<BurstInterval> itBurstList = burstList;

		Iterator<BurstInterval> it1 = itBurstList.iterator();
		while (it1.hasNext()){

			BurstInterval bint1 = (BurstInterval)it1.next();
			Iterator<BurstInterval> it2 = itBurstList.iterator();

			while (it2.hasNext()){
				BurstInterval bint2 = (BurstInterval)it2.next();

				//if there is NOT an edge between the two vertices 

				if (bint1 == bint2)
					continue;

				if (BurstySimDater.overlaps(bint1, bint2, 0))

					if (sg.getEdge(bint1, bint2)==null && sg.getEdge(bint2, bint1)==null )
						sg.addEdge(bint1, bint2);

			}	

		}



		return sg;

	}


	private static boolean overlaps(BurstInterval a, BurstInterval b, double X){

		double weight = 0;

		int aStart = a.getStart();
		int bStart = b.getStart();
		int aEnd = a.getEnd();
		int bEnd = b.getEnd();

		double union=0;
		double intersection=-1;

		// a   |    |
		// b |    |
		if ( aStart>=bStart && aStart<=bEnd && aEnd>=bEnd ){
			weight = bEnd - aStart + 1;
			intersection = weight;
			union = aEnd - bStart + 1;
		}

		// a     |   |
		// b  |         |
		if ( aStart>=bStart && aEnd<=bEnd ){
			weight = aEnd - aStart + 1;
			intersection = weight;
			union = bEnd - bStart + 1;
		}

		// a   |    |
		// b     |     |
		if ( aStart<=bStart && aEnd>=bStart && aEnd<=bEnd ){
			weight = aEnd - bStart + 1;
			intersection = weight;
			union = bEnd - aStart + 1;
		}

		// a    |            |
		// b       |      |
		if ( aStart<=bStart && aEnd>=bEnd){
			weight = bEnd - bStart +1;
			intersection = weight;
			union = aEnd - aStart + 1;
		}

		double score = intersection/union;

		if (score >= X)
			return true;
		else 
			return false;
	}



//	private static boolean overlaps(Interval a, Interval b, double X){
//
//		double weight = 0;
//
//		int aStart = a.getStart();
//		int bStart = b.getStart();
//		int aEnd = a.getIntEnd();
//		int bEnd = b.getIntEnd();
//
//		double union=0;
//		double intersection=-1;
//
//		// a   |    |
//		// b |    |
//		if ( aStart>=bStart && aStart<=bEnd && aEnd>=bEnd ){
//			weight = bEnd - aStart + 1;
//			intersection = weight;
//			union = aEnd - bStart + 1;
//		}
//
//		// a     |   |
//		// b  |         |
//		if ( aStart>=bStart && aEnd<=bEnd ){
//			weight = aEnd - aStart + 1;
//			intersection = weight;
//			union = bEnd - bStart + 1;
//		}
//
//		// a   |    |
//		// b     |     |
//		if ( aStart<=bStart && aEnd>=bStart && aEnd<=bEnd ){
//			weight = aEnd - bStart + 1;
//			intersection = weight;
//			union = bEnd - aStart + 1;
//		}
//
//		// a    |            |
//		// b       |      |
//		if ( aStart<=bStart && aEnd>=bEnd){
//			weight = bEnd - bStart +1;
//			intersection = weight;
//			union = aEnd - aStart + 1;
//		}
//
//		double score = intersection/union;
//
//		if (score >= X)
//			return true;
//		else 
//			return false;
//	}

	private static boolean overlaps(int aStart, int aEnd, int bStart, int bEnd, int X){

		double weight = 0;

		double union=0;
		double intersection=-1;

		// a   |    |
		// b |    |
		if ( aStart>=bStart && aStart<=bEnd && aEnd>=bEnd ){
			weight = bEnd - aStart + 1;
			intersection = weight;
			union = aEnd - bStart + 1;
		}

		// a     |   |
		// b  |         |
		if ( aStart>=bStart && aEnd<=bEnd ){
			weight = aEnd - aStart + 1;
			intersection = weight;
			union = bEnd - bStart + 1;
		}

		// a   |    |
		// b     |     |
		if ( aStart<=bStart && aEnd>=bStart && aEnd<=bEnd ){
			weight = aEnd - bStart + 1;
			intersection = weight;
			union = bEnd - aStart + 1;
		}

		// a    |            |
		// b       |      |
		if ( aStart<=bStart && aEnd>=bEnd){
			weight = bEnd - bStart +1;
			intersection = weight;
			union = aEnd - aStart + 1;
		}

		double score = intersection/union;

		if (score >= X)
			return true;
		else 
			return false;
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

















	//Return true if number num is appeared only once in the array � num is unique.
	private static boolean isUnique(int [] arry, int num)
	{
		for (int i = 0; i < arry.length;i++)
			if (arry [i] == num)
				return false;
		return true;
	}

	//Convert the given array to an array with unique values � without duplicates and Return it
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




	public HashMap<String, HashMap<Integer, Double>> getDataset() {
		return dataset;
	}










	public void setBag(int j) {
		this.currentBag = j;

	}










	public Interval predictInterval(TDocument tDoc, int timespan, int kJaccard, int ktfidf, Index index, int cliques, int x,  HashMap<String, HashMap<Integer, Double>>  trainingSet) throws IOException, ParseException {
		
	

		TokenizerFactory tokFactory = new IndoEuropeanTokenizerFactory();
		JaccardDistance jaccardD = new JaccardDistance(tokFactory);


		String[] tDocTerms = tDoc.getTopKTerms(ktfidf, index, trainingSet);
		

		// Get the top-kJaccard most similar docs to the current document
		Document[] knndocs = index.getKNNBasedOnText(tDoc,tDocTerms, kJaccard+1);


		if (knndocs==null)
			return null;


		List<Document> bodySimDocs = new ArrayList<Document>();

		//4. Iterate over them
		double maxDist = 0;
		double minDist = Double.MAX_VALUE;
		double avgDist = 0;
		int kDocs = Math.min(kJaccard, knndocs.length);

		for(int i = 0;i<kDocs;i++){
			Document tempDoc = knndocs[i];

			bodySimDocs.add(tempDoc);
	
			double dist = jaccardD.proximity(tDoc.getBodyStr(),  knndocs[i].get("body"));

			if (dist>maxDist)
				maxDist = dist;
			if (dist<minDist)
				minDist = dist;

			avgDist+= dist;
		}
		avgDist = avgDist / kDocs;

		double scoreArray[] = new double[bodySimDocs.size()];
		int docIt = 0;

		int minStamp = timespan;
		int maxStamp = 0;

		//For each doc in the top-k
		for (Document tempDoc : bodySimDocs){

			double docScore = 1;

			int docTimestamp = Integer.parseInt(tempDoc.get("timestamp"));


			if (docTimestamp>maxStamp)
				maxStamp = docTimestamp;
			if (docTimestamp<minStamp)
				minStamp = docTimestamp;
			//Find overlap
			String[] commonTerms = this.findCommonTerms(tDoc, tempDoc);


			//maybe tDocTerms here
			for (String term : commonTerms){

				ArrayList<BurstInterval> termBursts = this.terms.get(term);


				if (termBursts==null)
					continue;

				
				//Is there a burst at tempDoc timestamp?
				for (BurstInterval termBurst : termBursts){
					if (termBurst.getStart()<= docTimestamp && termBurst.getEnd()>=docTimestamp){
						docScore= docScore+1;
						break;
					}
				}


			}
			//maybe insert percentage here
			scoreArray[docIt] = docScore;

			docIt++;

		}
	
		int simDocsSpan = maxStamp - minStamp+1;
		int maxStart = -1, maxEnd = -1;
		
		int cliqueStart = maxStart;
		int cliqueEnd = maxEnd;
		
		
		if(simDocsSpan < x){
			cliqueStart = minStamp;
			cliqueEnd = simDocsSpan;
		}
		else{
			
			double [] timeScoreArray = new double[simDocsSpan];

			int position = 0;
			for(Document doc : bodySimDocs){
				int docTimestamp = Integer.parseInt(doc.get("timestamp"));
				timeScoreArray[docTimestamp - minStamp] = scoreArray[position];
				
				position++;
			}
			
			
//			
//			for(int i = 0; i < timeScoreArray.length; i++){
//				System.out.print(timeScoreArray[i]+" ");
//			}
//			System.out.println();
		//find the max interval of maxSize = x
			int start = 0;
			int end = 0;
			
			double maxScore = -1;
			double tempSum = 0;
			
			int topZeroCount = 0;
			int zeroCount = 0;
			
			
			for(int i = 0; i < x;i++){
				tempSum+=timeScoreArray[i];
				end++;
				
				if(timeScoreArray[i] == 0){
					zeroCount++;
				}
			}
			
			//end == x
			//start = 0
			
			topZeroCount = zeroCount;
			maxScore = tempSum;
			maxEnd = end-1;
			maxStart = start;
			
			while(end < (simDocsSpan )){
				
				tempSum-=timeScoreArray[start];
				tempSum+=timeScoreArray[end];
				
				
				
				if(timeScoreArray[start]==0)
					zeroCount--;
				if(timeScoreArray[end] == 0)
					zeroCount++;
				
				
				
				
				start++;
				end++;
				
				if(tempSum >= maxScore){
					if(tempSum == maxScore){	//first tie breaker
						if(zeroCount < topZeroCount){
							//switch
							maxStart = start;
							maxEnd = end-1;
							maxScore = tempSum;
							topZeroCount = zeroCount;
						}
						else if(zeroCount== topZeroCount){	//second time breaker
							int length = end -1 - start;
							int maxL = maxEnd - maxStart;
							if(maxL > length){
								//switch
								maxStart = start;
								maxEnd = end-1;
								maxScore = tempSum;
								topZeroCount = zeroCount;
							}
						}
						//else dont swithc
					}
					else{
						//switch
						maxStart = start;
						maxEnd = end-1;
						maxScore = tempSum;
						topZeroCount = zeroCount;
					}
				}
			}
						
			///////////////////////////////////////////END END END END END ///////////////
		}
		
		
		cliqueStart = maxStart+minStamp;
		cliqueEnd = maxEnd+minStamp;
		
		int cliqueLength = cliqueEnd-cliqueStart+1;
		
		int rest = x - cliqueLength;
		int pad = rest/2;
		
		if (cliqueStart-pad>=0)
			cliqueStart = cliqueStart-pad;
		
		if (cliqueEnd+pad<=timespan-1)
			cliqueEnd = cliqueEnd+pad;
			
		return new Interval(cliqueStart, cliqueEnd);
		
//		for (int i=0;i<cliques;i++){
//
//			if (bodySimDocs.size()==0)
//				break;
//
//			Wclique.createNewGraphFile(bodySimDocs, scoreArray,  graphsFilename, x);
//			Wclique.testWclique(graphsFilename, cliquesFilename);
//
//			File cliqueFile = new File(cliquesFilename);
//			Reader rin = new InputStreamReader(new FileInputStream(cliqueFile), "UTF-8");
//			BufferedReader in = new BufferedReader( rin );
//
//			String line;
//			while ((line = in.readLine()) != null){
//				//19 19 19 19 19 19 59 87 142 116
//				String[] cliqueString = line.split(" ");
//				int[] cliqueNodes = new int[cliqueString.length];
//
//				boolean validFile = true;
//				for (int c=0;c<cliqueString.length;c++){
//
//					if (isInteger(cliqueString[c]))
//						cliqueNodes[c]= Integer.parseInt(cliqueString[c]);
//					else{
//						validFile = false;
//						break;
//					}
//				}
//
//				if (!validFile)
//					break;
//
//				cliqueNodes = toUniqueArray(cliqueNodes);
//
//				int cliqueEnd = -1;
//				int cliqueStart = Integer.MAX_VALUE;
//
//				for (int c=0;c<cliqueNodes.length;c++){
//					Document doc = bodySimDocs.get(cliqueNodes[c]);
//					int docStamp = Integer.valueOf(doc.get("timestamp"));
//
//					if (docStamp<cliqueStart)
//						cliqueStart = docStamp;
//
//					if (docStamp>cliqueEnd)
//						cliqueEnd = docStamp;
//				}	
//
//	
//				if (cliqueEnd-cliqueStart+1<x){
//					int diff = x-(cliqueEnd-cliqueStart+1);
//					int extend = diff/2;
//					cliqueStart-= extend;
//					cliqueEnd+= extend;
//				}
//
//				if (cliqueEnd<cliqueStart){
//					System.out.println("LOL FAIL");
//				}
//
//				if (cliqueStart<0)
//					cliqueStart = 0;
//				if (cliqueEnd>timespan-1)
//					cliqueEnd = timespan-1;
//
//				for (int d=cliqueStart; d<=cliqueEnd; d++){
//					if (days[d]==0)
//						days[d]=i+1;
//				}
//
//				//First clique
//				if (i==0){
//					in.close();
//					rin.close();
//					return new Interval(cliqueStart, cliqueEnd);
//				}
//
//			}
//			
//			in.close();
//			rin.close();
//
//			
//			
//		}
//		
//		return null;

		
	}
	


	public void setFilenames(String graphsFilename, String cliquesFilename) {
		this.graphsFilename = graphsFilename;
		this.cliquesFilename = cliquesFilename;
		
	}
		
		

}
