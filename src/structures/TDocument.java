package structures;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import utilities.Day;
import utilities.Index;
import utilities.Sorter;

public class TDocument {

	protected int timestamp;
	protected HashMap<String, Integer> title;
	protected HashMap<String, Integer> body;

	private String dateStr;
	private String bodyStr;
	private String titleStr;
	private String otherFields;
	
	/* line is of the form: 
	 * date\tafield\tafield\ttitleterm1 titleterm2 ...\tbodyterm1 bodyterm2 
	 */
	
	// First day is: 01/01/1900, 01/01/1903, 01/01/1908 
	// Always stores other fields
	public TDocument(String line, Day startDay, boolean createMaps, boolean storeStrings) {
		super();

		
		String[] lineElements = line.split("\t");
		// lineElements[0] = date
		// lineElements[1] = title
		// lineElements[2] = body
		// lineElements[3..n] = other fields
		
		if (lineElements.length<3){
			String tempDate = lineElements[0];
			lineElements = new String[3];
			lineElements[0] = tempDate;
			lineElements[1] = "title";
			lineElements[2] = "body";
			
		}
		
		if (storeStrings){
			this.dateStr = lineElements[0];
			this.titleStr = lineElements[1];
			this.bodyStr = lineElements[2];
			
		}
		
		if (lineElements.length>3){
			this.otherFields = lineElements[3];
			for (int i=4;i<lineElements.length;i++)
				this.otherFields = this.otherFields + "\t" + lineElements[i];
		}
			
		
		if (createMaps){
			this.body = new HashMap<String, Integer>();
			this.title = new HashMap<String, Integer>();
			//this.timestamp = Integer.parseInt(lineElements[1]);
			//this.date = lineElements[2];

			//lineElements[0] = YYYYMMDD
			int year = Integer.parseInt(lineElements[0].substring(0, 4));
			int month = Integer.parseInt(lineElements[0].substring(4, 6))-1; //months in class Day go 0-11
			int day = Integer.parseInt(lineElements[0].substring(6));
			Day date = new Day(year, month, day);


			this.timestamp = startDay.daysBetween(date);

			String titleString = lineElements[1];
			for (String term : titleString.split(" ")){

				if (term.equals("")) continue;

				Integer tf = title.get(term);
				if (tf == null) title.put(term, 1);
				else title.put(term, tf + 1);

			}

			String bodyString = lineElements[2];
			for (String term : bodyString.split(" ")){

				if (term.equals("")) continue;

				Integer tf = body.get(term);
				if (tf == null) body.put(term, 1);
				else body.put(term, tf + 1);

			}
		}
		
		
		
		
	}

	
	
	
	//always creates maps
	//dkotzias version
	public TDocument(String line, Day startDay, int granularity) {
		super();

		
		String[] lineElements = line.split("\t");
		// lineElements[0] = date
		// lineElements[1] = title
		// lineElements[2] = body
		// lineElements[3..n] = other fields
		
//		if (lineElements.length<3){
//			String tempDate = lineElements[0];
//			lineElements = new String[3];
//			lineElements[0] = tempDate;
//			lineElements[1] = "title";
//			lineElements[2] = "body";
//			
//		}
//		
//		if (storeStrings){
//			this.dateStr = lineElements[0];
//			this.titleStr = lineElements[1];
//			this.bodyStr = lineElements[2];
//			
//		}
//		
//		if (lineElements.length>3){
//			this.otherFields = lineElements[3];
//			for (int i=4;i<lineElements.length;i++)
//				this.otherFields = this.otherFields + "\t" + lineElements[i];
//		}
		
		
		
		if(lineElements.length!= 4){
			System.out.println("I could not load document with ID "+ lineElements[0]);
		}
			
		
		if ( lineElements.length == 4){
			this.body = new HashMap<String, Integer>();
			this.title = new HashMap<String, Integer>();
			//this.timestamp = Integer.parseInt(lineElements[1]);
			//this.date = lineElements[2];

			
			
			//dkotzias
			//lineElements[0] = country code - always 1 for NYT dataset
			//find the date
			
			String dateStr = lineElements[1];
			String [] dateElements = dateStr.split(" ");
			
			int year = Integer.parseInt(dateElements[2]);
			int month = Integer.parseInt(dateElements[1]) - 1;
			int day = Integer.parseInt(dateElements[0]);
			Day date = new Day(year,month,day);
			
			month++;
			String monthStr = month +"";
			if(monthStr.length()<2) monthStr="0"+monthStr;
			
			String temp = ""+year+monthStr+day;
			this.dateStr = temp;
			
			temp = "";
			

			this.timestamp = startDay.daysBetween(date);
			
			//fill in the title and body maps
			//title
			String [] titleTerms = lineElements[2].split(" ");
			for(String entry : titleTerms){
				if(entry != null && !entry.equals("")){
					String word = entry.split(":")[0];
					Integer freq = Integer.parseInt(entry.split(":")[1]);
					title.put(word, freq);
					for(int i =0; i< freq; i++){
						temp+= word+" ";
					}
				}
				
			}
			
			this.titleStr = temp;
			temp = "";
			
			//body
			String [] bodyTerms = lineElements[3].split(" ");
			for(String entry : bodyTerms){
				if(entry != null && !entry.equals("")){
					String word = entry.split(":")[0];
					Integer freq = Integer.parseInt(entry.split(":")[1]);
					body.put(word, freq);
					for(int i =0; i< freq; i++){
						temp+= word+" ";
					}
				}
				
			}
			
			this.bodyStr = temp;
			
			
			
			//lineElements[0] = YYYYMMDD
//			int year = Integer.parseInt(lineElements[0].substring(0, 4));
//			int month = Integer.parseInt(lineElements[0].substring(4, 6))-1; //months in class Day go 0-11
//			int day = Integer.parseInt(lineElements[0].substring(6));
//			Day date = new Day(year, month, day);
//
//
//			this.timestamp = startDay.daysBetween(date);
//
//			String titleString = lineElements[1];
//			for (String term : titleString.split(" ")){
//
//				if (term.equals("")) continue;
//
//				Integer tf = title.get(term);
//				if (tf == null) title.put(term, 1);
//				else title.put(term, tf + 1);
//
//			}
//
//			String bodyString = lineElements[2];
//			for (String term : bodyString.split(" ")){
//
//				if (term.equals("")) continue;
//
//				Integer tf = body.get(term);
//				if (tf == null) body.put(term, 1);
//				else body.put(term, tf + 1);
//
//			}
		}
		
		
		
		
	}


	
	
	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public int getDocLength(){
		int length = 0;
		Set<String> terms = this.body.keySet();
		for (String term : terms)
			length+= this.body.get(term);
		
		return length;
	}
	
	public String[] getTitleTerms(){
		
		Set<String> terms = this.title.keySet();
		//Set<String> terms = this.body.keySet();
		String[] array = (String[])terms.toArray(new String[terms.size()]);
		return array;
	}
	
	
	public String[] getBodysTerms(){
		
		return this.bodyStr.split(" ");
//		Set<String> terms = this.body.keySet();
//		//Set<String> terms = this.body.keySet();
//		String[] array = (String[])terms.toArray(new String[terms.size()]);
//		return array;
	}
	

	public String[] getBodyTerms(){
		
		
		Set<String> terms = this.body.keySet();
		//Set<String> terms = this.body.keySet();
		String[] array = (String[])terms.toArray(new String[terms.size()]);
		return array;
	}
	
	
	public HashMap<String, Integer> getBody() {
		return body;
	}


	public HashMap<String, Integer> getTitle() {
		return title;
	}
	
	
	public String[] getRepresentativeTerms(){
		
		Set<String> terms = this.title.keySet();
		//Set<String> terms = this.body.keySet();
		String[] array = (String[])terms.toArray(new String[terms.size()]);
		return array;
	}

	
	public String[] getTopKTerms(int K, Index lucIndex, HashMap<String, HashMap<Integer, Double>> dataset) throws IOException{

		// Iterate through all the terms in the body
		Set<String> terms = this.body.keySet();
		String[] termsArray = (String[])terms.toArray(new String[terms.size()]);
		int[] indicesTfIdf = new int[termsArray.length];
		int[] indicesEntropy = new int[termsArray.length];
		int[] indicesRandom = new int[termsArray.length];
		
		if (termsArray.length<K)
			return termsArray;
		
		
		double[] scoreTfIdf = new double[termsArray.length];
		
		double[] scoreEntropy = new double[termsArray.length];
		
		double[] scoreRandom = new double[termsArray.length];
		
		
		int count = 0;
		for (String term : termsArray){
			
//			if (dataset.containsKey(term)){
//			
//				Double totalTf = 0.0;
//				
//				HashMap<Integer, Double> appearances = dataset.get(term);
//				
//				Collection<Double> values = appearances.values();
//				for (Double value : values)
//					totalTf += value;
//			
//				scoreEntropy[count]=lucIndex.entropy(term, totalTf);				
//			}
//			else {
//				scoreEntropy[count]=0;
//			}
			
			double tf = Math.sqrt(this.body.get((term)));
			double idf = lucIndex.getIdf(term);
			
			scoreTfIdf[count]=tf*idf;
			
//			scoreRandom[count] = 1;
			
			//System.out.println(term + " " + tf);
			count++;
		}
		
		for (int i=0;i<termsArray.length;i++){
			indicesEntropy[i]=i;
			indicesTfIdf[i]=i;
			indicesRandom[i]=i;
		}
		
//		Sorter.quicksort(scoreEntropy, indicesEntropy);
		Sorter.quicksort(scoreTfIdf, indicesTfIdf);
//		Sorter.quicksort(scoreRandom, indicesRandom);

		String[] topkterms = new String[K];

		for (int i=0;i<K;i++)
			topkterms[i] = termsArray[indicesTfIdf[indicesTfIdf.length-i-1]];
		
		

		//System.out.println(Arrays.toString(topkterms));
		return topkterms;
		
	}
	
	
	public String getDateStr() {
		return dateStr;
	}

	

	public String getBodyStr() {
		
		
		return bodyStr;
		
	}





	public String getTitleStr() {
		return titleStr;
	}





	public String getOtherFields() {
		return otherFields;
	}

	
	
}
