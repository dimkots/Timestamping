package lda;


import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class Lda{
	
	public Lda(){
		System.out.println("Created an LDA class");
	}

	
	
	
	/*Returns the filename required to train the set
	 * result must be id\t year\t title\t text\n
	 * must be a .csv file
	 * @param dataset filename
	 *Assumes input file with date \t title \t text\n 
	 */
	
	public static String MakeLdaCompatible (String filename){
		String result = null;
		result = filename.replace(".txt", ".csv");
		
		int id = 1;
		
		try{
			//Open the dataset file
			FileReader fr = new FileReader(filename);
			BufferedReader in = new BufferedReader(fr);
			
			//open the result file
			FileWriter fw = new FileWriter(result);
			
			
			//parse filename into result
			String line = null;
			
			
			while ( (line = in.readLine()) != null){
				String[] lineElements = line.split("\t"); 
				// check if lineElemets is valid?
				//remove commas for safety				
				fw.write(id +"," +lineElements[0]+"," + lineElements[1].replace(",","") + "," + lineElements[2].replace(",","") + "\n");
				id++;
				
				//System.out.println("id: " + id);
			}
			
			//close the files
			fw.close();
			fr.close();
			
			
		}
		catch(IOException e){
			System.err.println("Caught IOException: " +  e.getMessage());
		}	
		
		
		System.out.println("Created lda compatible file with " + (id -1) + " documents");
		return result;	
	}
	
	
	
	
	static void Training(String LdaFilename, int numberOfTopics){
		
		
		System.out.print("Training for " + LdaFilename + " ....");
		//create the script needed for training
		String scriptName = null;
		try{
			scriptName = createTrainingScript(LdaFilename,numberOfTopics);
		}
		catch (IOException e){
			 System.err.println("Caught IOException: " +  e.getMessage());
		}
		
		
		//command: java -jar tmt-0.4.0.jar scriptName
		String cmd = "java -jar tmt-0.4.0.jar "+ scriptName +" 2 > error.txt";
		
		Runtime run = Runtime.getRuntime() ; 
		
		Process pr;
		//run the process
		try {
		
			pr = run.exec(cmd);	
			//pr.waitFor();		/////This line may need to be commented TODO possible error
			//must create my wait
		
		} catch (IOException e ) {
			//  Auto-generated catch block
			e.printStackTrace();
		} 
//		catch (InterruptedException e) {
//			//  Auto-generated catch block
//			e.printStackTrace();
//		} 
		
		
		
		
		System.out.println("   Done!");
	}
	
	
	
	
	//No need to look at this very simple and debugged
	//simply prints the script into a file with a proper name
	public static String createTrainingScript(String filename, int numberOfTopics) throws IOException{
		
		String scriptName = null;
		scriptName = "script_" + filename.replace(".csv", ".scala");
		
		try{
			FileWriter fw = new FileWriter(scriptName);
			String str = null;
			str = "// Stanford TMT Example 2 - Learning an LDA model\n// http://nlp.stanford.edu/software/tmt/0.4/\n\n// tells Scala where to find the TMT classes\nimport scalanlp.io._;\nimport scalanlp.stage._;\nimport scalanlp.stage.text._;\nimport scalanlp.text.tokenize._;\nimport scalanlp.pipes.Pipes.global._;\n\nimport edu.stanford.nlp.tmt.stage._;\nimport edu.stanford.nlp.tmt.model.lda._;\nimport edu.stanford.nlp.tmt.model.llda._;\n\nval source = CSVFile(\""+ filename + "\") ~> IDColumn(1);\n\nval tokenizer = {\n  SimpleEnglishTokenizer() ~>            // tokenize on space and punctuation\n  CaseFolder() ~>                        // lowercase everything\n  WordsAndNumbersOnlyFilter() ~>         // ignore non-words and non-numbers\n  MinimumLengthFilter(3)                 // take terms with >=3 characters\n}\n\nval text = {\n  source ~>                              // read from the source file\n  Column(4) ~>                           // select column containing text\n  TokenizeWith(tokenizer) ~>             // tokenize with tokenizer above\n  TermCounter() ~>                       // collect counts (needed below)\n  TermMinimumDocumentCountFilter(4) ~>   // filter terms in <4 docs\n  TermDynamicStopListFilter(30) ~>       // filter out 30 most common terms\n  DocumentMinimumLengthFilter(5)         // take only docs with >=5 terms\n}\n\n// turn the text into a dataset ready to be used with LDA\nval dataset = LDADataset(text);\n\n// define the model parameters\nval params = LDAModelParams(numTopics = "+ numberOfTopics + ", dataset = dataset,\n  topicSmoothing = 0.01, termSmoothing = 0.01);\n\n// Name of the output model folder to generate\nval modelPath = file(\"lda-"+filename.replace(".csv", "")+ "-"+numberOfTopics+"-model\");\n\n// Trains the model: the model (and intermediate models) are written to the\n// output folder.  If a partially trained model with the same dataset and\n// parameters exists in that folder, training will be resumed.\nTrainCVB0LDA(params, dataset, output=modelPath, maxIterations=1000);\n\n// To use the Gibbs sampler for inference, instead use\n// TrainGibbsLDA(params, dataset, output=modelPath, maxIterations=1500);\n\n";
			fw.write(str);
			fw.close();
		}
		catch (IOException e){
			 System.err.println("Caught IOException: " +  e.getMessage());
		}
		System.out.println("Created the script for training");
		return scriptName;
	}
	

	
	
	
	
	
	
	
	public static String LdaTest(String trainedSet, String TestingSetFilename, int numberOfTopics, int numberOfTerms){
		String scriptName = null;
		
		//create Testing Script
		scriptName = createTestingScript(trainedSet,TestingSetFilename,numberOfTopics,numberOfTerms);
		
		//execute the command for the script
		String cmd = "java -jar tmt-0.4.0.jar "+ scriptName +" 2 > error.txt";
		
		Runtime run = Runtime.getRuntime() ; 
		Process pr;
		try {
			
			pr = run.exec(cmd);	
			pr.waitFor();
			
		} catch (IOException e ) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} 
		
		System.out.println("Finished Testing for " + TestingSetFilename);
		
		
		
		
		//Create the results file
		//Each line must have the keywords from the most probable topic
		String result = null;
		result = "LDA-Terms-" + TestingSetFilename.replace(".csv",".txt");
		Lda.createResultsFile(TestingSetFilename, trainedSet,result, numberOfTopics);
		
		
		//returns the filename
		return result;
		
	
	}
	
	
	public static String [] getTopics(String filename, int numberOfTopics){
		
		String topics [] = new String[numberOfTopics];
		//load the N topics in memory - array of strings 
	
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader in = new BufferedReader(fr);
			int i = 0;
			String line = null;
		
			//for each topic
			line = in.readLine();	//first line has headers
		
			while((line = in.readLine() ) != null){
				String [] lineWords = line.split(",");
				topics[i] = lineWords[1] ;
				
				//for each keyword in the topic
				for(int j = 2; j < lineWords.length; j++){
					topics[i] = topics[i]+ " " + lineWords[j];
				}
				i++;
			
			}
			
			in.close();
			fr.close();
		
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		return topics;
	}
	
	
	
	public static int [] getMaxTopics(String filename){
		int count = 0;
		int [] max = null;
		
		FileReader fr;
		try {
			fr = new FileReader(filename);
			BufferedReader in = new BufferedReader(fr);
			
			while((in.readLine() != null)) count++;
			
			fr.close();
			in.close();
			
			
			max = new int [count];
			
			fr = new FileReader(filename);
			in = new BufferedReader(fr);
			String line = null;
			count = 0;
			while((line = in.readLine()) != null){
				String [] lineElements = line.split(",");
				float temp_max = 0;
				
				for(int i = 1; i < lineElements.length; i++){
					if(temp_max < Float.parseFloat(lineElements[i])){
						temp_max = Float.parseFloat(lineElements[i]);
						max[count] = i-1;
					}
					
				}
				count++;
			}
			
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		
	
		
		return max;
	}
	
	
	//No need to look
	public static String createTestingScript(String trained, String testing, int numberOfTopics, int numberOfTerms){
		String testingScript = null;
		testingScript = "inferScript_" + testing.replace(".csv",".scala");
		
		
		try{
			FileWriter fw = new FileWriter(testingScript);
			String str = null;
			str = "// Stanford TMT Example 3 - LDA inference on a new dataset\n// http://nlp.stanford.edu/software/tmt/0.4/\n\n// tells Scala where to find the TMT classes\nimport scalanlp.io._;\nimport scalanlp.stage._;\nimport scalanlp.stage.text._;\nimport scalanlp.text.tokenize._;\nimport scalanlp.pipes.Pipes.global._;\n\nimport edu.stanford.nlp.tmt.stage._;\nimport edu.stanford.nlp.tmt.model.lda._;\nimport edu.stanford.nlp.tmt.model.llda._;\n\n// the path of the model to load\nval modelPath = file(\"lda-"+trained.replace(".csv","")+ "-"+numberOfTopics+"-model\");\n\nprintln(\"Loading \"+modelPath);\nval model = LoadCVB0LDA(modelPath);\n// Or, for a Gibbs model, use:\n// val model = LoadGibbsLDA(modelPath);\n\n// A new dataset for inference.  (Here we use the same dataset\n// that we trained against, but this file could be something new.)\nval source = CSVFile(\""+testing + "\") ~> IDColumn(1);\n\nval text = {\n  source ~>                              // read from the source file\n  Column(4) ~>                           // select column containing text\n  TokenizeWith(model.tokenizer.get)      // tokenize with existing model's tokenizer\n}\n\n// Base name of output files to generate\nval output = file(modelPath, source.meta[java.io.File].getName.replaceAll(\".csv\",\"\"));\n\n// turn the text into a dataset ready to be used with LDA\nval dataset = LDADataset(text, termIndex = model.termIndex);\n\nprintln(\"Writing document distributions to \"+output+\"-document-topic-distributions.csv\");\nval perDocTopicDistributions = InferCVB0DocumentTopicDistributions(model, dataset);\nCSVFile(output+\"-document-topic-distributuions.csv\").write(perDocTopicDistributions);\n\nprintln(\"Writing topic usage to \"+output+\"-usage.csv\");\nval usage = QueryTopicUsage(model, dataset, perDocTopicDistributions);\nCSVFile(output+\"-usage.csv\").write(usage);\n\nprintln(\"Estimating per-doc per-word topic distributions\");\nval perDocWordTopicDistributions = EstimatePerWordTopicDistributions(\n  model, dataset, perDocTopicDistributions);\n\nprintln(\"Writing top terms to \"+output+\"-top-terms.csv\");\nval topTerms = QueryTopTerms(model, dataset, perDocWordTopicDistributions, numTopTerms="+numberOfTerms+");\nCSVFile(output+\"-top-terms.csv\").write(topTerms);\n\n";
			fw.write(str);
			fw.close();
		}
		catch (IOException e){
			 System.err.println("Caught IOException: " +  e.getMessage());
		}
		
		System.out.println("Created the testing script for inference "+ testingScript);
		return testingScript;
	}
	

	
	public static void createResultsFile(String TestingSetFilename, String trainedSet, String result, int numberOfTopics){
		String directoryName = null;
		String topicsWords = null;
		String documentsTopics = null;
		
		
		directoryName = "lda-" + trainedSet.replace(".csv","") + "-" + numberOfTopics +"-model";
		documentsTopics = directoryName + "/" +TestingSetFilename.replace(".csv", "")+"-document-topic-distributuions.csv";
		
		
		topicsWords = directoryName + "/" +TestingSetFilename.replace(".csv", "")+"-top-terms.csv";
		
		String [] topics;
		
		topics = getTopics(topicsWords,numberOfTopics);
		
		
		
		
		try {
			
			FileWriter fw = new FileWriter (result);
			int [] max = getMaxTopics(documentsTopics);
			
			
			//For each doc in the testing set, read the line with topics distribution
			
			//find max
			
			//write topic[max] + \n
			for(int i = 0; i < max.length; i++){
				if(max[i] < 0 || max[i] >= numberOfTopics){
					System.out.println("i is " + i + "max[i] is " + max[i]);
				}
				fw.write(topics[max[i]] + "\n");
			}
			
			//close  files
			fw.close();
			
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	public static String [] getResults(String filename){
		int count = 0;
		
		FileReader fr;
		
		//count the documents
		try {
			fr = new FileReader(filename);
			BufferedReader in = new BufferedReader(fr);
			
			while((in.readLine() != null)) count++;
			
			fr.close();
			in.close();
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		
		String [] topics = new String[count];
		
		try{
			fr = new FileReader(filename);
			BufferedReader in = new BufferedReader(fr);
			String line = null;
			int i = 0;
			while((line = in.readLine()) != null){
				topics[i] = line;
				i++;
			}
			fr.close();
			in.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		return topics;
		
	}
	
	
	
	public static String [] LDATopics(String trainingFilename, String testingFilename, int numberOfTopics, int numberOfTerms){
		String [] topics = null;
		String resultsFilename = null;
		

		String ldaTraining = null;
		ldaTraining = MakeLdaCompatible(trainingFilename);

		//testing
		String ldaTesting = null;
		ldaTesting = MakeLdaCompatible(testingFilename);
		resultsFilename = LdaTest(ldaTraining,ldaTesting,numberOfTopics,numberOfTerms);

		//load the results in memory
		topics = getResults(resultsFilename);
		
		System.out.println("Your results are in the file " + resultsFilename);
		return topics;
	}
	
	
	public static String LDATrainTopics(String trainingFilename, int numberOfTopics){

		//training
		String ldaTraining = MakeLdaCompatible(trainingFilename);
		Training(ldaTraining,numberOfTopics);
		
		
		return ldaTraining;

	}
	
	
	
	
	
	public static void main(String[] args){
	
//		String ldaCompatibleFilename = null;
//		ldaCompatibleFilename = Lda.MakeLdaCompatible("call1.txt");
//		
//		
//		//Lda.Training( filename.csv , Number of Topics wanted)
//		//Lda.Training(ldaCompatibleFilename, 5);
//
//		
//		String ldaCompatibleTest = null;
//		ldaCompatibleTest = Lda.MakeLdaCompatible("call1_test.txt");
//		
//		String result = null;
//		//Lda.TestSet( training filename.csv , testing.csv, Number of Topics wanted, NUmber of terms for each topic)
//		result = Lda.LdaTest(ldaCompatibleFilename, ldaCompatibleTest, 5, 10);
//		
//		System.out.println("Your results are in the file " + result);

		String trainingFilename = "trainingFile.txt";
		int numberOfTopics = 10;
		Lda.LDATrainTopics(trainingFilename, numberOfTopics);
		//Lda.LDATopics("call1.txt", "call1_test.txt", 3, 10);
	}
}




