package utilities;

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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import structures.TDocument;

public class Preprocessor {

	private static final String[] stopwords = {"about","above","across","after","again",
		"against","all","almost","alone","along","already","also","although","always",
		"among","an","and","another","any","anybody","anyone","anything","anywhere","are",
		"area","areas","around","as","ask","asked","asking","asks","at","away","b","back",
		"backed","backing","backs","be","became","because","become","becomes","been","before",
		"began","behind","being","beings","best","better","between","big","both","but","by",
		"c","came","can","cannot","case","cases","certain","certainly","clear","clearly","come",
		"could","d","did","differ","different","differently","do","does","done","down","down",
		"downed","downing","downs","during","e","each","early","either","end","ended","ending",
		"ends","enough","even","evenly","ever","every","everybody","everyone","everything",
		"everywhere","f","face","faces","fact","facts","far","felt","few","find","finds","first",
		"for","four","from","full","fully","further","furthered","furthering","furthers","g","gave",
		"general","generally","get","gets","give","given","gives","go","going","good","goods","got",
		"great","greater","greatest","group","grouped","grouping","groups","h","had","has","have",
		"having","he","her","here","herself","high","high","high","higher","highest","him","himself",
		"his","how","however","i","if","important","in","interest","interested","interesting","interests",
		"into","is","it","its","itself","j","just","k","keep","keeps","kind","knew","know","known","knows",
		"l","large","largely","last","later","latest","least","less","let","lets","like","likely","long",
		"longer","longest","m","made","make","making","man","many","may","me","member","members","men",
		"might","more","most","mostly","mr","mrs","much","must","my","myself","n","necessary","need",
		"needed","needing","needs","never","new","new","newer","newest","next","no","nobody","non",
		"noone","not","nothing","now","nowhere","number","numbers","o","of","off","often","old","older","oldest","on",
		"once","one","only","open","opened","opening","opens","or","order","ordered","ordering","orders","other","others",
		"our","out","over","p","part","parted","parting","parts","per","perhaps","place","places","point","pointed","pointing",
		"points","possible","present","presented","presenting","presents","problem","problems","put","puts","q","quite","r",
		"rather","really","right","right","room","rooms","s","said","same","saw","say","says","second","seconds","see","seem",
		"seemed","seeming","seems","sees","several","shall","she","should","show","showed","showing","shows","side","sides",
		"since","small","smaller","smallest","so","some","somebody","someone","something","somewhere","state","states","still",
		"still","such","sure","t","take","taken","than","that","the","their","them","then","there","therefore","these","they",
		"thing","things","think","thinks","this","those","though","thought","thoughts","three","through","thus","to","today",
		"together","too","took","toward","turn","turned","turning","turns","two","u","under","until","up","upon","us","use",
		"used","uses","v","very","w","want","wanted","wanting","wants","was","way","ways","we","well","wells","went","were",
		"what","when","where","whether","which","while","who","whole","whose","why","will","with","within","without","work",
		"worked","working","works","would","x","y","year","years","yet","you","young","younger","youngest","your","yours","z", "article",
		"writer", "writes", "anonymous"};


	public static void main(String[] args) {
		//String file = "/media/Data1/Datasets/NYT/1995-2005/NYT10.txt";
		String datasetFile = "/media/Data1/Datasets/NYT/19871996.txt";
		String newFile = "19871996_20.txt";
		int docCount = 665741;
		double percentage = 0.2;
		
		int finalDoccount = (int)(percentage*docCount);
	//	String datasetPath = "/media/Data1/Datasets/NYT/1987-1996";
		//String sampleFile = "NYT_SAMPLE_10y.txt";
		randomSample(datasetFile, newFile, finalDoccount, docCount);
		//remove2005(file, newFile);
		
		//mergeFiles("NYT19871996.txt", datasetPath);
		//computeIntersection("burstHits.txt", "kjetilHits.txt");
	}

	private static void remove2005(String file, String newFile) {
	
		try {
			File newsFile = new File(newFile);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newsFile, false),"UTF-8" ));
			
			Reader rin = new InputStreamReader(new FileInputStream(file), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line = null;
			int counter2005 = 0;
			int counterElse = 0;
			while ((line = in.readLine()) != null){
				String[] lineElements = line.split("\t");
				String date = lineElements[1];
				
				String[] dateElements = date.split(" ");
				
				String year = dateElements[2];
				
				if (!year.equals("2005")){
					out.write(line+"\n");
					counterElse++;
				}
				else
					counter2005++;
				
				//out.write(line+"\n");
				
			}
			in.close();
			rin.close();
			
			out.close();
			System.out.println("2005: "+ counter2005);
			System.out.println("Else: " + counterElse);
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	public static void mergeFiles(String newsFilename, String datasetPath){
		File dir = new File(datasetPath);
		String[] children = dir.list();

		int counter = 0;
		try {
			File newsFile = new File(newsFilename);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newsFile, false),"UTF-8" ));

			if (children == null) {
				System.exit(-1);
			} else {


				for (int i=0; i<children.length; i++) {
					String filename = children[i];
					String year = filename.substring(filename.indexOf("."));

					
					Reader rin = new InputStreamReader(new FileInputStream(datasetPath+ "/" + filename), "UTF-8");
					BufferedReader in = new BufferedReader( rin );
					String line;

					while ((line = in.readLine()) != null){
						out.write(line+"\n");
						counter++;
					}
					in.close();
					rin.close();
				}

			}
			System.out.println(counter + " documents");
			out.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}


	public static void removeTerms(String termsDfsFilename, String datasetFilename, String newFilename, Day startDay){

		try {

			Reader rin = new InputStreamReader(new FileInputStream(datasetFilename), "UTF-8");
			BufferedReader in = new BufferedReader( rin );

			Reader termsReader = new InputStreamReader(new FileInputStream(termsDfsFilename), "UTF-8");
			BufferedReader termsIn = new BufferedReader( termsReader );

			File newFile = new File(newFilename);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile, false),"UTF-8" ));

			ArrayList<String> validTerms = new ArrayList<String>();
			String line;

			while ((line = termsIn.readLine()) != null){

				String[] lineElements = line.split(":");

				String term = lineElements[0];

				
				validTerms.add(term);

			}

			int count = 0;

			while ((line = in.readLine()) != null){

				TDocument doc = new TDocument(line, startDay, true, true);

				String body = "";
				String[] terms = doc.getBodysTerms();
				for (String term : terms){
					if (validTerms.contains(term))
						body+= term+" ";
				}

				System.out.println(count++);
				out.write(doc.getDateStr() + "\t" + doc.getTitleStr() + "\t" + body + "\t" + doc.getOtherFields() + "\n");
			}




			out.close();
			in.close();
			termsIn.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	public static void computeIntersection(String burstyHitsFilename, String kjetilHitsFilename){

		try {

			Reader bin = new InputStreamReader(new FileInputStream(burstyHitsFilename), "UTF-8");
			BufferedReader binReader = new BufferedReader( bin );

			Reader kin = new InputStreamReader(new FileInputStream(kjetilHitsFilename), "UTF-8");
			BufferedReader kinReader = new BufferedReader( kin );

			String binLine = binReader.readLine();
			String kinLine = kinReader.readLine();
			
			kinReader.close();
			binReader.close();
			
			kin.close();
			bin.close();
			
			String[] binElements = binLine.split(",");
			String[] kinElements = kinLine.split(",");
			
			
			System.out.println("Bin hits:" + binElements.length);
			System.out.println("Kin hits:" + kinElements.length);
			
			HashSet<Integer> binHits = new HashSet<Integer>();
			HashSet<Integer> kinHits = new HashSet<Integer>();
			
			for (String binHit : binElements)
				binHits.add(Integer.parseInt(binHit));

			for (String kinHit : kinElements)
				kinHits.add(Integer.parseInt(kinHit));
			
			int common = 0;
			for (Integer binHit : binHits)
				if (kinHits.contains(binHit))
					common++;
			
			System.out.println("Common hits:" + common);

		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}


	
	

	public static void countTerms(String datasetFilename, Day startDay){
		try {

			Reader rin = new InputStreamReader(new FileInputStream(datasetFilename), "UTF-8");
			BufferedReader in = new BufferedReader( rin );

			String line;			
			int totalTerms = 0;
			while ((line = in.readLine()) != null){
				
				TDocument tDoc = new TDocument(line, startDay, true, false);
				totalTerms+= tDoc.getBodyTerms().length;
			}

			in.close();

			System.out.println("Total: " + totalTerms + " distinct terms");
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}



	public static void removeStopWords(String datasetFilename, String newDatasetFilename, Day startDay){
		try {

			Reader rin = new InputStreamReader(new FileInputStream(datasetFilename), "UTF-8");
			BufferedReader in = new BufferedReader( rin );

			File newFile = new File(newDatasetFilename);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile, false),"UTF-8" ));

			String line;

			ArrayList<String> stopList = new ArrayList<String>(Arrays.asList(Preprocessor.stopwords));
			int count = 0;
			while ((line = in.readLine()) != null){

				//String[] lineElements = line.split("\t");

				TDocument tDoc = new TDocument(line, startDay, true, true);

				String[] terms = tDoc.getBodyTerms();
				String body = "";

				for (String term : terms){
					if (!stopList.contains(term))
						body+= term+" ";
				}

				System.out.println(count++);
				out.write(tDoc.getDateStr() + "\t" + tDoc.getTitleStr() + "\t" + body + "\t" + tDoc.getOtherFields() +"\n");

			}

			in.close();
			out.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	public static String createCrossvalFiles(String datasetFilename, String folderName, String datasetName,  int N){

		String fileStem = datasetName + "f";
		
		try {
			
			
			File folder = new File(folderName);
			folder.mkdir();
			
			Reader rin = new InputStreamReader(new FileInputStream(datasetFilename), "UTF-8");
			BufferedReader in = new BufferedReader( rin );

			File[] files = new File[N];
			BufferedWriter[] outs = new BufferedWriter[N];
			for (int i=0;i<N;i++){
				files[i] = new File(folderName+"\\"+fileStem+i+".txt");
				outs[i] = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(files[i], false),"UTF-8" ));
			}

			String line = null;

			int j=0;
			while ((line = in.readLine()) != null )	{
				outs[j%N].write(line+"\n");
				j++;
			}
				
			for (int i=0;i<N;i++){
				outs[i].close();
			}
	
			in.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return folderName;
	}

	public static void createTestTrainingFiles(String newsFilename, String testdocsFilename, String trainingDocsFilename, double trainingSetPerc, int total){

		try {

			Reader rin = new InputStreamReader(new FileInputStream(newsFilename), "UTF-8");
			BufferedReader in = new BufferedReader( rin );

			File testDocsFile = new File(testdocsFilename);
			BufferedWriter testOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testDocsFile, false),"UTF-8" ));


			File trainingDocsFile = new File(trainingDocsFilename);
			BufferedWriter trainOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trainingDocsFile, false),"UTF-8" ));

			String line = null;

			Random randomGenerator = new Random(); 
			int i=0, j=0;

			int testDocs = (int)((1-trainingSetPerc) * total);
			int[] randomIndices = new int[testDocs];

			for (int idx = 0; idx < testDocs; ++idx){
				int randomInt = randomGenerator.nextInt(total);
				if (contains(randomIndices, randomInt)){
					idx--;
					continue;
				}
				randomIndices[idx] = randomInt;			
			}

			Arrays.sort(randomIndices);

			j=0; i=0;
			while ((line = in.readLine()) != null ){

				if (j<testDocs && randomIndices[j]==i++){//select this doc
						testOut.write(line+"\n");
						j++;
				}
				else
					trainOut.write(line+"\n");
			}

			testOut.close();
			trainOut.close();
			in.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static boolean contains(int[] array, int value){
		for (int i=0;i<array.length;i++)
			if (array[i]==value)
				return true;

		return false;
	}


	

	public static void computeFreqSequences(HashMap<String, HashMap<Integer, Double>> dataset, String timeseriesFilename, boolean output) throws IOException  {

		File tsFile = new File(timeseriesFilename);
		FileWriter fw = new FileWriter(tsFile,false);
		BufferedWriter out = new BufferedWriter(fw);

		Object[] keysArray = dataset.keySet().toArray();
		Arrays.sort(keysArray);
		System.out.println(keysArray.length + " Distinct Terms");
		for (int i=0; i<keysArray.length; i++){

			String term = (String)keysArray[i];
			HashMap<Integer,Double> termMap = dataset.get(term);

			out.write(term+":");

			ArrayList<Integer> dayList = new ArrayList<Integer>();

			// Iterate through all days when a term has appeared
			for (Integer day : termMap.keySet())
				dayList.add(day);

			// Sort them in ascending order
			Collections.sort(dayList);

			int stopped = 0;
			int count;
			for (Integer day : dayList){

				for (count=stopped;count<day;count++)
					out.write("0 ");
				if (output) System.out.print(termMap.get(day)+" ");
				out.write(termMap.get(day)+" ");
				stopped = day+1;
			}
			if (output) System.out.println();
			out.write("\n");
		}
		out.close();
		fw.close();

		if (output) System.out.println("Timeseries file created!");
	}

	
	
	public static void createLexicon(String[] datasetFiles, String lexiconFile, Day[] startDays){
		try {

			// 0. Specify the analyzer for tokenizing text.
		    //    The same analyzer should be used for indexing and searching
		    StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

		    //Index.buildLuceneIndex("luceneIndex", datasetFile, startDay, false);
		    //buildLuceneIndex(String indexPath, String datasetfile, Day startDay, boolean finalIndex)
		    String indexFolder = "index";
		    File path = new File(System.getProperty("luceneIndex"), "lexiconIindex");
		   
		    //Create the index
		    Directory index = new SimpleFSDirectory(path);
		    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
//		    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
//		    
//		    IndexWriter w = new IndexWriter(index, config);
//
//			//for each dataset file
//			for (int datasetCnt = 0; datasetCnt < datasetFiles.length; datasetCnt++){
//				
//				String datasetFilename = datasetFiles[datasetCnt];
//				Day startDay = startDays[datasetCnt];
//				//Read all documents and store them in lucene index
//				Reader rin = new InputStreamReader(new FileInputStream(datasetFilename), "UTF-8");
//				BufferedReader in = new BufferedReader( rin );
//
//				String line;
//			    int count = 0;
//		        while ((line = in.readLine()) != null){
//		        	TDocument tDoc = new TDocument(line, startDay, true, false);
//
//		        	HashMap<String, Integer> body = tDoc.getBody();
//		        	String[] termsArray = tDoc.getBodyTerms();
//		        	String terms = "";
//		        	
//		        	for (String term : termsArray){
//		        		int freq = body.get(term);
//		        		
//		        		for (int i=0;i<freq;i++)
//		        			terms+= term +" ";
//		        	}
//		        	
//		        	Document doc = new Document();
//		        	doc.add(new Field("body", terms , Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
//		        	doc.add(new Field("timestamp", Integer.toString(tDoc.getTimestamp()), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
//		        	doc.add(new Field("docId", Integer.toString(count) , Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
//		   
//		        	w.addDocument(doc);
//		        	count++;
//		        	System.out.println(count);
//		        }
//			}
//			
//			
//	        w.close();
	        
	      //  HashMap<String, Double> dfs = new HashMap<String, Double>();
	        
	        IndexReader indexReader = IndexReader.open(index);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		    DefaultSimilarity ds = new DefaultSimilarity();		
			
		    File lFile = new File(lexiconFile);
		    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lFile, false),"UTF-8" ));
		    
			TermEnum tEnum = indexReader.terms();
			int count = 0;
			
			
			while (tEnum.next())
				count++;
				
			System.out.println(count + " distinct terms");
			
			
			double[] dfs = new double[count];
			String[] terms = new String[count];
			count = 0;
			
			tEnum = indexReader.terms();
			while (tEnum.next()){
				Term term = tEnum.term();
				String sterm = term.text();
				int docFreq = indexSearcher.docFreq(term);
				dfs[count] = docFreq;
				terms[count] = sterm;
				count++;
			}
			
			System.out.println("Sorting....");
			
			int[] indices = new int[dfs.length];
			for (int i=0;i<indices.length;i++)
				indices[i]=i;
			
			Sorter.quicksort(dfs, indices);
			
			System.out.println("Sorted!");
			
			
			//Take only top-100000 words
			int[] docFreqs = new int[105000];
			
			int k = 102109;
			for (int i=indices.length-1; i>=0;i--){
				String sterm = terms[indices[i]];
				double df = dfs[i];
				
				docFreqs[(int)df]++;
				if (isInteger(sterm))
					continue;
				out.write(sterm + " " + df +"\n");
				if (--k==0)
					break;
				
				
			}
			System.out.println("Wrote lexicon file!");
			
			
//			for (int i=0; i<docFreqs.length; i++)
//				System.out.println(i + " " + docFreqs[i]);

			out.close();
	    }
		catch (IOException e1) {
			e1.printStackTrace();
		}

		
		
			
////			File newFile = new File(lexiconFile);
////			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile, false),"UTF-8" ));
//
//			String line;
//
//			ArrayList<String> stopList = new ArrayList<String>(Arrays.asList(Preprocessor.stopwords));
//			int count = 0;
//			while ((line = in.readLine()) != null){
//
//				//String[] lineElements = line.split("\t");
//
//				TDocument tDoc = new TDocument(line, startDay, true, true);
//
//				String[] terms = tDoc.getBodyTerms();
//				String body = "";
//
//				for (String term : terms){
//					if (!stopList.contains(term))
//						body+= term+" ";
//				}
//
//				System.out.println(count++);
////				out.write(tDoc.getDateStr() + "\t" + tDoc.getTitleStr() + "\t" + body + "\t" + tDoc.getOtherFields() +"\n");
//
//			}
//
//			in.close();
//			out.close();
//		}
//		catch (IOException e1) {
//			e1.printStackTrace();
//		}

	}

	
	public static boolean isInteger( String input )  
	{  
	   try  
	   {  
	      Integer.parseInt( input );  
	      return true;  
	   }  
	   catch( Exception e )  
	   {  
	      return false;  
	   }  
	} 

	public static void mergePartitions(String directory, String outputFilename){

		File dir = new File(directory);
		String[] children = dir.list();

		int counter = 0;
		try {
			File newsFile = new File(outputFilename);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newsFile, true),"UTF-8" ));

			if (children == null) {
				System.exit(-1);
			} else {


				for (int i=0; i<children.length; i++) {
					String filename = children[i];


					Reader rin = new InputStreamReader(new FileInputStream(directory+ "\\" + filename), "UTF-8");
					BufferedReader in = new BufferedReader( rin );
					String line;

					while ((line = in.readLine()) != null){
						int index = line.indexOf("\t");
						String newLine = line.substring(index+1)+"\n";
						out.write(newLine);
						counter++;
					}
					in.close();
					rin.close();
				}

			}
			System.out.println(counter + " documents");
			out.close();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}

	}
//	public static void computeTfsInDocs(
//			String trainingdocsFilename, Day startDay ) throws IOException {
//		
//		Reader rin = new InputStreamReader(new FileInputStream(trainingdocsFilename), "UTF-8");
//		BufferedReader in = new BufferedReader( rin );
//
//		String line;
//		int docId = 0;
//		while ((line = in.readLine()) != null){
//
//			TDocument tDoc = new TDocument(line, startDay, true, false);
//
//			String[] docTerms = tDoc.getBodyTerms();
//			HashMap<String, Integer> docBody = tDoc.getBody();
//
//			for (String term : docTerms){
//
//				if (term.equals("")) continue;
//
//				int tf = docBody.get(term);
//
//
//				HashMap<Integer, Integer> termMap = tokens.get(term);
//				if (termMap == null){
//					termMap = new HashMap<Integer,Integer>();
//
//					termMap.put(docId, tf);
//					tokens.put(term, termMap);
//				}
//				else{   				
//					Integer mtf = termMap.get(docId);
//					if (mtf == null) termMap.put(docId, tf);
//					else termMap.put(docId, mtf + tf);
//				}
//
//
//				Integer Ctf = corpusTokens.get(term);
//				if (Ctf == null) corpusTokens.put(term, tf);
//				else corpusTokens.put(term, Ctf + tf);
//
//			}
//		}
//	}


	public static String mergefolds(String folder, String datasetName, int testNumber, int folds) throws IOException {
		
		String trainingFilename = folder+"\\"+datasetName+"training"+testNumber+".txt";

		File trainingFile = new File(trainingFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trainingFile, false),"UTF-8" ));		
		
		for (int i=0;i<folds;i++){
			
			if (i==testNumber)
				continue;
			String foldFilename = folder+"\\"+datasetName+"f"+i+".txt";
			
			Reader rin = new InputStreamReader(new FileInputStream(foldFilename), "UTF-8");
			BufferedReader in = new BufferedReader( rin );
			String line;

			while ((line = in.readLine()) != null)
				out.write(line+"\n");
		}
		
		out.close();
		return trainingFilename;
	}


	public static void deleteDir(String folderName) {
		File directory = new File(folderName);
		
		if (directory.exists()){
			delete(directory);
		}
		
	}
	
	
	public static void deleteFile(String fileName) {
		File file = new File(fileName);
		delete(file);
	}
	
	public static void delete(File file){
		if (file.isDirectory()){
			
			//directory is empty, then delete it
			if (file.list().length==0)
				file.delete();
			else{
				//list all directory contents
				String files[] = file.list();
				
				for (String temp : files){
					//construct the file structure 
					File fileDelete = new File(file, temp);
					delete(fileDelete);
					
				}
				
				if (file.list().length==0)
					file.delete();
				
				
			}
		}
		else{
			
			file.delete();
			
		}
			
			
	}
	
	
	public static void randomSample(String inputFilename, String outFileName, int sampleSize, int total) {
		String fileName = inputFilename;
		
		boolean selectTerms = false;
		
	
		try {
			// The file with the docs
			File fileIn = new File(fileName);

			
			Reader rin = new InputStreamReader(new FileInputStream(fileIn), "UTF-8");
			BufferedReader in = new BufferedReader(rin);
			
			// The end file with all the data
			File outputFile = new File(outFileName);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"));

			Random randomGenerator = new Random(); 
			int i=0, j=0;
			
			int[] randomIndices = new int[sampleSize];
			 
		    for (int idx = 0; idx < sampleSize; ++idx){
		    	int randomInt = randomGenerator.nextInt(total);
		    	if (contains(randomIndices, randomInt)){
		    		idx--;
		    		continue;
		    	}
		    	randomIndices[idx] = randomInt;			
		    }
			
		    Arrays.sort(randomIndices);
			
			j=0; i=0;
			String line = null;
			while ((line = in.readLine()) != null && j<sampleSize){
				
				if (randomIndices[j]==i++){//select this doc
					
					j++;
					
					if (selectTerms){
						
						Day startDay = new Day(1903,0,1);
						TDocument tDoc = new TDocument(line, startDay, true, true);
						String newBodystr = "";
						String[] bodyTerms = tDoc.getBodyTerms();
						
						for (String term : bodyTerms){
						
							String[] pos = term.split("/");
							//if it is adjective, noun or verb
							String actualTerm = pos[0];
							if (pos[1].contains("NN") || pos[1].contains("VB") || pos[1].contains("JJ"))
								newBodystr = newBodystr + " " + term;
						}
						
						String dateStr = tDoc.getDateStr();
						String titleStr = tDoc.getTitleStr();
						out.write( dateStr + "\t" + titleStr  + "\t" + newBodystr + "\n");			        	
						
						
						
					}
					else
						out.write(line+"\n");
					
					
					System.out.println(j);
					
					
					
				}
				
			}

			out.close();
			in.close();

			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	

	










}
