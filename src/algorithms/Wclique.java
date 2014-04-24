package algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;

import structures.BurstInterval;

public class Wclique {

	public static int size = 0;
	
	/*
	 * Given a burstList (list of bursty intervals), this method creates the burst intervals graph
	 * where each node corresponds to a bursty interval, and each edge between
	 * intervals/nodes corresponds to an overlap.
	 *
	 * Rows of intervalGraph is the number of intervals/nodes. intervalGraph is N*N. 
	 */
	public static void createGraphFile(ArrayList<BurstInterval> burstList, String graphFilename, boolean weight, double X) throws IOException{

		int V = burstList.size();
		
		Wclique.size+=V;
		
		double[][] intervalGraph = new double[V][V];
		
		int i=0,j=0;
		
		
		List<BurstInterval> itBurstList = burstList;
		
		Iterator<BurstInterval> it1 = itBurstList.iterator();
		while (it1.hasNext()){
			
			BurstInterval bint1 = (BurstInterval)it1.next();
			Iterator<BurstInterval> it2 = itBurstList.iterator();
			j=0;
			while (it2.hasNext()){
				BurstInterval bint2 = (BurstInterval)it2.next();
				intervalGraph[i][j] = overlap(bint1, bint2, X);
				j++;
			}	
			
			i++;
		}
		

		File graphFile = new File(graphFilename);
	    FileWriter fw = new FileWriter(graphFile,false);
	    BufferedWriter out = new BufferedWriter(fw);

	    int E = 1;
	    
	    out.write(V + " " + E +"\n");
	    
	    for (i=0;i<V;i++){
	    	
	    	int count = 0;
	    	
	    	if (weight)
	    		out.write((int)Math.ceil(burstList.get(i).getScore())+" "); //WeightClique
	    	else
	    		out.write("1 ");   //MaxClique
	    	
	    	for (j=0;j<V;j++)
	    		if (intervalGraph[i][j]>0)
	    			count++;
	    		
	    	out.write(count+" ");
	    	
	    	for (j=0;j<V;j++)
	    		if (intervalGraph[i][j]>0)
	    			out.write(j+" ");	
	    	
	    	out.write("\n");
	    }
	    
	    out.close();
	    fw.close();
	}
	
	
	
	/*
	 * Given a burstList (list of bursty intervals), this method creates the burst intervals graph
	 * where each node corresponds to a bursty interval, and each edge between
	 * intervals/nodes corresponds to an overlap.
	 *
	 * Rows of intervalGraph is the number of intervals/nodes. intervalGraph is N*N. 
	 */
	public static void createNewGraphFile(List<Document> docList, double[] docScores, String graphFilename, int X) throws IOException{

		int V = docList.size();
		
		Wclique.size+=V;
		
		double[][] intervalGraph = new double[V][V];
		
		//int i=0,j=0;
		
		//List<Document> itBurstList = docList;

		int e = 0;
		for (int i=0;i<V;i++){
			Document doc1 = docList.get(i);
			int doc1timestamp = Integer.valueOf(doc1.get("timestamp"));
			
			for (int j=0;j<V;j++){
				if (j==i)
					continue;
				
				Document doc2 = docList.get(j);
				int doc2timestamp = Integer.valueOf(doc2.get("timestamp"));
				
				if (Math.abs(doc1timestamp-doc2timestamp)<=X){
					intervalGraph[i][j] = 1;
					e++;
				}
			}
			
		}

//		Iterator<Document> it1 = itBurstList.iterator();
//		while (it1.hasNext()){
//
//			Document doc1 = (Document)it1.next();
//			Iterator<Document> it2 = itBurstList.iterator();
//
//			int doc1timestamp = Integer.valueOf(doc1.get("timestamp"));
//			
//			
//			while (it2.hasNext()){
//				Document doc2 = (Document)it2.next();
//
//				//if there is NOT an edge between the two vertices 
//
//				if (doc1 == doc2)
//					break;
//
//				int doc2timestamp = Integer.valueOf(doc2.get("timestamp"));
//				
//				if (Math.abs(doc1timestamp-doc2timestamp)<=X){
//					intervalGraph[i][j] = 1;
//					
//				}
//				j++;
//					
//			}	
//			
//			i++;
//
//		}

		int i=0,j=0;
		File graphFile = new File(graphFilename);
	    FileWriter fw = new FileWriter(graphFile,false);
	    BufferedWriter out = new BufferedWriter(fw);

	    int E = 1;
	    
	    out.write(V + " " + E +"\n");
	    
	    for (i=0;i<V;i++){
	    	
	    	int count = 0;
	    	out.write((int)Math.ceil(docScores[i])+" "); //WeightClique
	    	
	    	for (j=0;j<V;j++)
	    		if (intervalGraph[i][j]>0)
	    			count++;
	    		
	    	out.write(count+" ");
	    	
	    	for (j=0;j<V;j++)
	    		if (intervalGraph[i][j]>0)
	    			out.write(j+" ");	
	    	
	    	out.write("\n");
	    }
	    
	    out.close();
	    fw.close();
	}
	
	
	
	private static double overlap(BurstInterval a, BurstInterval b, double X){
		
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
			return 1;
		else 
			return 0;
	}
	

	public static void testWclique(String graphFilename, String cliquesFilename){
		String cmd = "./wclique " + graphFilename + " "+ cliquesFilename;

		try {
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}

