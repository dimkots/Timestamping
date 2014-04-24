package utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import structures.BurstInterval;
import structures.BDocument;
import structures.OldNewsDocument;
import structures.TDocument;

public class Stats {

	public static void computeDocStats(ArrayList<TDocument> docs){
		Iterator<TDocument> it = docs.iterator();
		
		int count = 0;
		int sumTokens = 0;
		ArrayList<String> distinctTerms = new ArrayList<String>();
		while (it.hasNext()){
			
			count++;
			OldNewsDocument tDoc = (OldNewsDocument)it.next();
			String[] docTerms = tDoc.getRepresentativeTerms();
			sumTokens+= docTerms.length;
						
			for (String term : docTerms )
				if (!distinctTerms.contains(term))
					distinctTerms.add(term);
	
		}
			
		
		double avgLength = (double)sumTokens / count;
		
		System.out.println("Total documents: "+ count);
		System.out.println("Avg doc length: "+ avgLength);
		System.out.println("Total tokens: "+ sumTokens);
		System.out.println("Distinct terms: "+ distinctTerms.size());
		
	}



	public static void computeBurstStats(HashMap<String, ArrayList<BurstInterval>> terms) {
		
		int termsCount = 0;
		int burstsCount = 0;
		int sumLength = 0;
		
		
		for (String term : terms.keySet()) {
			termsCount++;
		
			ArrayList<BurstInterval> burstList = terms.get(term);
			
			Iterator<BurstInterval> burstIt = burstList.iterator();
			
			//Iterate over the bursts
			while (burstIt.hasNext()){
				BurstInterval burst = (BurstInterval)burstIt.next();
				burstsCount++;
				
				int length = burst.getEnd() - burst.getStart() + 1;				
				sumLength+= length;
			}

			System.out.println(termsCount);
		}
			
		
		double avgLength = (double)sumLength / burstsCount;
		double burstsPerTerm = (double) burstsCount / termsCount;
		
		System.out.println("Total bursts: "+ burstsCount);
		System.out.println("Total bursty terms: "+ termsCount);
		System.out.println("Avg burst length: "+ avgLength);
		System.out.println("Bursts per term: "+ burstsPerTerm);
		
	}
	
	
	public static void burstHistogram(HashMap<String, ArrayList<BurstInterval>> terms){
		
		int[] bursts = new int[11];
		
		for (int i=0;i<bursts.length;i++)
			bursts[i]=0;
		
		for (String term : terms.keySet()) {
			
			ArrayList<BurstInterval> burstList = terms.get(term);
			
			Iterator<BurstInterval> burstIt = burstList.iterator();
			
			//Iterate over the bursts
			while (burstIt.hasNext()){
				BurstInterval burst = (BurstInterval)burstIt.next();
				
				for (int i=burst.getStart(); i<=burst.getEnd(); i++)
					bursts[i/365]++;				
			}

		}
		for (int i=0;i<bursts.length;i++)
			System.out.println(bursts[i]);
	}
}
