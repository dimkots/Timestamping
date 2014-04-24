package burstdetection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.InputStreamReader;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import structures.BurstInterval;
import utilities.Day;


public class BurstEngine {

	/*
	 * Computes bursts. 
	 * Input: 
	 * 1. time series filename
	 * 2. bursts filename
	 * 5. minBurstLength: The minimum length for a bursts to consider it valid
	 * 6. minValue: The minimum frequency of the first appearance of a term to start considering it. 
	 * 
	 * By default discards the whole timeseries before the first 'significant' appearance of a term
	 * 
	 * Output:
	 * Creates the burstsFileName file with all the discovered bursts
	 */
	public static void computeMAX2Bursts(String tsFileName, String burstsFileName, int minBurstLength, int minValue, boolean output){

		int totalBursts = 0, pointBursts = 0, totalLength = 0, counter = 0;


		if (output) System.out.println("Computing bursts!");
		
		try {
			File tsFile = new File(tsFileName);
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tsFile), "UTF-8") );
			
			File burstsFile = new File(burstsFileName);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(burstsFile),"UTF-8" ));
			
			/* This method reads a file of the form
			   beacon:0 0 0 0 0 0 0 0 0 0 0 0 0 0 */
			String[] lineElements = null;

			String term;
			Vector<Float> seq = new Vector<Float>();
			Ruzzo ruzzo = new Ruzzo();
			
			long startTime = 0, stopTime = 0, elapsedTime = 0;
			
			startTime = System.currentTimeMillis();
			int termsCnt = 0, maxSeqLength = 0;
			
			String line;
			
			//Read the whole file line by line
			while ((line = in.readLine(  )) != null){
				//Parse the line just read
				lineElements = line.split(":");
				seq.clear();
				term = lineElements[0];
				termsCnt++;

				boolean appeared = false;
				int firstTermDay = -1;
				
				counter = 0;
				for (String valueStr :  lineElements[1].split(" ")){
					
					float val = Float.parseFloat(valueStr);	

					if (appeared || val>=minValue){
						seq.add(val);
						if (!appeared){
							firstTermDay = counter;
							appeared=true;
						}
						
					}	
					
					counter++;	
				}
			
				/*
				 *************************************************
				 *************************************************
				 * 				OFFLINE ALGORITHM
				 *************************************************
				 *************************************************
				 */
				Vector<Float> originalSeq = new Vector<Float>(seq);
				ruzzo.removeAvg(seq);						
				Vector<Segment> segs;
				boolean termWritten = false;
				
				segs = ruzzo.getMaximalIntervals(seq);
				if (segs!=null && segs.size()>0){

						// for each segment re-apply the max-1 algorithm
						for (Segment s:segs){
							
							Vector<Float> newVec = new Vector<Float>();
							
							int from1 = firstTermDay + s.getFrom();
							int to1 = firstTermDay + s.getTo();
							
							for (int i=from1;i<=to1;i++)
								newVec.add(originalSeq.get(i));
							
							ruzzo.removeAvg(newVec);	
							
							Vector<Segment> secondSegs = ruzzo.getMaximalIntervals(newVec) ;
							
							if (secondSegs!=null && secondSegs.size()>0){
									
									for (Segment s1 : secondSegs){
										
										if (!termWritten){
											out.write(term +":");
											termWritten=true;
										}
										
										int from2 = firstTermDay + s1.getFrom() + from1;
										int to2 = firstTermDay + s1.getTo() + from1;
										
										int length = to2-from2+1;
										
										if (length==1)
											pointBursts++;
										
										totalLength += (to2-from2+1);
										
										if (length<minBurstLength)
											continue;
										
										
										out.write("("+from2+","+to2+"," + s1.getScore()+ ") ");
										totalBursts++;	
									}//for each secondseg							
								}//if secondsegs != null 
							}//for each seg	
						}//if segs != null
						
						if (termWritten) out.write("\n");
					}//while in.readline
					
			out.close();
			in.close();

			stopTime = System.currentTimeMillis();
			elapsedTime = stopTime - startTime;
			
			if (output) {
				System.out.println ("Totalbursts:" + totalBursts);
				System.out.println("Time: "+elapsedTime);
				System.out.println("Terms: "+termsCnt);
				System.out.println("Max seq length: "+maxSeqLength);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (output){
			System.out.println("Bursts' computation finished!");
			System.out.println("Total Burst Count: "+totalBursts);
			System.out.println("Avg Burst Length: "+ (double)totalLength/totalBursts);
			System.out.println("Point Burst Count: "+pointBursts);
		}
	}//End computeBursts
	
	
	
	
	public static List<BurstInterval> computeMAXKBurstsSplit(Vector<Float> seq, int minBurstLength, int maxBurstLength, boolean output){
		
		int totalBursts = 0, pointBursts = 0, totalLength = 0, counter = 0;

		List<BurstInterval> bursts = new ArrayList<BurstInterval>();
		if (output) System.out.println("Computing bursts!");
		
		try {
	
			Ruzzo ruzzo = new Ruzzo();
			
			int termsCnt = 0, maxSeqLength = 0;
			
	
			
			double burstPercentSum = 0; 
			
			
				


			
				
				
				Vector<Float> originalSeq = new Vector<Float>(seq);
				ruzzo.removeAvg(seq);						
				Vector<Segment> segs;

				int u = 10;
				int daysInBursts = 0;
				segs = ruzzo.getMaximalIntervals(seq);
				if (segs!=null && segs.size()>0){

						boolean allSmall = false;
						
						
						while (!allSmall){
							
							allSmall = true;
							int segId = 0;
							
							for (segId = 0; segId < segs.size(); segId++){
								Segment tempSeg = segs.get(segId);
							
								if (tempSeg.getLen()>maxBurstLength){
									
									segs.remove(segId);
									allSmall = false;
									
									Vector<Float> newVec = new Vector<Float>();
							
									int from1 =  tempSeg.getFrom();
									int to1 =  tempSeg.getTo();
									
									for (int i=from1;i<=to1;i++)
										newVec.add(originalSeq.get(i));
									
									ruzzo.removeAvg(newVec);	
									
									Vector<Segment> newSegs = ruzzo.getMaximalIntervals(newVec);
									
									if (newSegs!=null && newSegs.size()>0){
										for (Segment newTempSeg : newSegs){
											int from2 = newTempSeg.getFrom() + from1;
											int to2 = newTempSeg.getTo() + from1;
											
											newTempSeg.setFrom(from2);
											newTempSeg.setTo(to2);
										}
										
										segs.addAll(segId, newSegs);
									}
								}
									
									
							}
							
						}

						for (Segment s:segs){
							
							
							
							
							int from = s.getFrom();
							int to = s.getTo();
							
							
							int length = to-from+1;
							
							if (length==1)
								pointBursts++;
							
							totalLength += (to-from+1);
							
							daysInBursts += (to-from+1);
							
							if (length<minBurstLength)
								continue;
							
							BurstInterval bint = new BurstInterval(from, to, "term", s.getScore());
							
							bursts.add(bint);
							totalBursts++;	

							
						}//for segment s
						

					}//segs!=null && segs.size()>0
				
					double burstPercent = (double)daysInBursts/730;
//					System.out.print("Burst percent:" + burstPercent);
//					System.out.println(" Bursty days:" + daysInBursts);
					burstPercentSum+= burstPercent;
					
	

			
			
			if (output) {
				System.out.println ("Totalbursts:" + totalBursts);
				System.out.println("Terms: "+termsCnt);
				System.out.println("Bursts per term: "+ (double)totalBursts/termsCnt);
				System.out.println("Bursty days percentage: "+ (double)burstPercentSum/termsCnt);
				System.out.println("Max seq length: "+maxSeqLength);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (output){
			System.out.println("Bursts' computation finished!");
			System.out.println("Total Burst Count: "+totalBursts);
			System.out.println("Avg Burst Length: "+ (double)totalLength/totalBursts);
			System.out.println("Point Burst Count: "+pointBursts);
		}
		
		return bursts;
		
	}
	
	/*
	 * Computes bursts, by applyint MAX-n algorithm, with the contraint of maximum
	 * burst length
	 * Input: 
	 * 1. time series filename
	 * 2. bursts filename
	 * 5. minBurstLength: The minimum length for a bursts to consider it valid
	 * 6. minValue: The minimum frequency of the first appearance of a term to start considering it. 
	 * 
	 * By default discards the whole timeseries before the first 'significant' appearance of a term
	 * 
	 * Output:
	 * Creates the burstsFileName file with all the discovered bursts
	 */
	public static void computeMAXKBurstsSplit(String tsFileName, String burstsFileName, int minBurstLength, int minValue, int maxBurstLength, boolean output){

		int totalBursts = 0, pointBursts = 0, totalLength = 0, counter = 0;


		if (output) System.out.println("Computing bursts!");
		
		try {
			File tsFile = new File(tsFileName);
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tsFile), "UTF-8") );
			
			File burstsFile = new File(burstsFileName);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(burstsFile),"UTF-8" ));
			
			/* This method reads a file of the form
			   beacon:0 0 0 0 0 0 0 0 0 0 0 0 0 0 */
			String[] lineElements = null;

			String term;
			Vector<Float> seq = new Vector<Float>();
			Ruzzo ruzzo = new Ruzzo();
			
		
			
			
			int termsCnt = 0, maxSeqLength = 0;
			
			String line;
			
			double burstPercentSum = 0; 
			
			//Read the whole file line by line
			while ((line = in.readLine(  )) != null){
				//Parse the line just read
				lineElements = line.split(":");
				seq.clear();
				term = lineElements[0];
				termsCnt++;

				boolean appeared = false;
				int firstTermDay = -1;
				
				counter = 0;
				for (String valueStr :  lineElements[1].split(" ")){
					
					float val = Float.parseFloat(valueStr);	

					if (appeared || val>=minValue){
						seq.add(val);
						if (!appeared){
							firstTermDay = counter;
							appeared=true;
						}
						
					}	
					
					counter++;	
				}
			
				
				Vector<Float> originalSeq = new Vector<Float>(seq);
				ruzzo.removeAvg(seq);						
				Vector<Segment> segs;
				boolean termWritten = false;
				
				int daysInBursts = 0;
				segs = ruzzo.getMaximalIntervals(seq);
				if (segs!=null && segs.size()>0){

						boolean allSmall = false;
						
						
						while (!allSmall){
							
							allSmall = true;
							int segId = 0;
							
							for (segId = 0; segId < segs.size(); segId++){
								Segment tempSeg = segs.get(segId);
							
								if (tempSeg.getLen()>maxBurstLength){
									
									segs.remove(segId);
									allSmall = false;
									
									Vector<Float> newVec = new Vector<Float>();
							
									int from1 = firstTermDay + tempSeg.getFrom();
									int to1 = firstTermDay + tempSeg.getTo();
									
									for (int i=from1;i<=to1;i++)
										newVec.add(originalSeq.get(i));
									
									ruzzo.removeAvg(newVec);	
									
									Vector<Segment> newSegs = ruzzo.getMaximalIntervals(newVec);
									
									if (newSegs!=null && newSegs.size()>0){
										for (Segment newTempSeg : newSegs){
											int from2 = firstTermDay + newTempSeg.getFrom() + from1;
											int to2 = firstTermDay + newTempSeg.getTo() + from1;
											
											newTempSeg.setFrom(from2);
											newTempSeg.setTo(to2);
										}
										
										segs.addAll(segId, newSegs);
									}
								}
									
									
							}
							
						}

						for (Segment s:segs){
							
							
							if (!termWritten){
								out.write(term +":");
								termWritten=true;
							}
							
							int from = s.getFrom();
							int to = s.getTo();
							
							
							int length = to-from+1;
							
							if (length==1)
								pointBursts++;
							
							totalLength += (to-from+1);
							
							daysInBursts += (to-from+1);
							
							if (length<minBurstLength)
								continue;
							
							out.write("("+from+","+to+"," + s.getScore()+ ") ");
							totalBursts++;	

							
						}//for segment s
						
						if (termWritten) out.write("\n");
					}//segs!=null && segs.size()>0
				
					double burstPercent = (double)daysInBursts/730;
//					System.out.print("Burst percent:" + burstPercent);
//					System.out.println(" Bursty days:" + daysInBursts);
					burstPercentSum+= burstPercent;
					
			}//while in.readline
					
			out.close();
			in.close();

			
			
			if (output) {
				System.out.println ("Totalbursts:" + totalBursts);
				System.out.println("Terms: "+termsCnt);
				System.out.println("Bursts per term: "+ (double)totalBursts/termsCnt);
				System.out.println("Bursty days percentage: "+ (double)burstPercentSum/termsCnt);
				System.out.println("Max seq length: "+maxSeqLength);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (output){
			System.out.println("Bursts' computation finished!");
			System.out.println("Total Burst Count: "+totalBursts);
			System.out.println("Avg Burst Length: "+ (double)totalLength/totalBursts);
			System.out.println("Point Burst Count: "+pointBursts);
		}
	}//End computeBursts
	
	
	
	
	
	
	
	/*
	 * Computes bursts. 
	 * Input: 
	 * 1. time series filename
	 * 2. bursts filename
	 * 5. minBurstLength: The minimum length for a bursts to consider it valid
	 * 6. minValue: The minimum frequency of the first appearance of a term to start considering it. 
	 * 
	 * By default discards the whole timeseries before the first 'significant' appearance of a term
	 * 
	 * Output:
	 * Creates the burstsFileName file with all the discovered bursts
	 */
	public static void computeMAX1Bursts(String tsFileName, String burstsFileName, int minBurstLength, int minValue, boolean output,int k){

		int totalBursts = 0, pointBursts = 0, totalLength = 0, counter = 0;

		double maxBurstScore = 0;
		
		if (output) System.out.println("Computing bursts!");
		
		try {
			File tsFile = new File(tsFileName);
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tsFile), "UTF-8") );
			
			File burstsFile = new File(burstsFileName);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(burstsFile),"UTF-8" ));
			
			
			File termsFile = new File("MAX1Terms.txt");
			BufferedWriter tout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(termsFile),"UTF-8" ));
			/* This method reads a file of the form
			   beacon:0 0 0 0 0 0 0 0 0 0 0 0 0 0 */
			String[] lineElements = null;

			Vector<Float> seq = new Vector<Float>();
			Ruzzo ruzzo = new Ruzzo();
			
			long startTime = 0, stopTime = 0, elapsedTime = 0;
			
			startTime = System.currentTimeMillis();
			int termsCnt = 0, maxSeqLength = 0;
			
			String line;
			
			//Read the whole file line by line
			while ((line = in.readLine(  )) != null){
				//Parse the line just read
				lineElements = line.split(":");
				seq.clear();
				String term = lineElements[0];
				termsCnt++;
				tout.write(term + "\n");
				boolean appeared = false;
				int firstTermDay = -1;
				
				counter = 0;
				for (String valueStr :  lineElements[1].split(" ")){
					
					
					float val = Float.parseFloat(valueStr);	

					if (appeared || val>=minValue){
						seq.add(val);
						if (!appeared){
							firstTermDay = counter;
							appeared=true;
						}
						
					}	
					
					counter++;	
				}
			
				/*
				 *************************************************
				 *************************************************
				 * 				OFFLINE ALGORITHM
				 *************************************************
				 *************************************************
				 */

				ruzzo.removeKAvg(seq, k);
				Vector<Segment> segs;
				boolean termWritten = false;
				
				segs = ruzzo.getMaximalIntervals(seq);
				if (segs!=null && segs.size()>0){

						for (Segment s:segs){
						
							
							int from1 = firstTermDay + s.getFrom();
							int to1 = firstTermDay + s.getTo();
																	
							int length = to1-from1+1;
							
							if (length==1)
								pointBursts++;
							
							totalLength += (to1-from1+1);
							
							if (length<minBurstLength)
								continue;
							
							if (!termWritten){
								out.write(term +":");
								termWritten=true;
							}
							
							
							if (s.getScore()>maxBurstScore)
								maxBurstScore=s.getScore();
							out.write("("+from1+","+to1+"," + s.getScore()+ ") ");
							totalBursts++;	
						 
							}//for each seg	
						}//if segs != null
						
						if (termWritten) out.write("\n");
					}//while in.readline
					
			out.close();
			in.close();

			stopTime = System.currentTimeMillis();
			elapsedTime = stopTime - startTime;
			
			if (output) {
				System.out.println ("MaxScore:" + maxBurstScore);
				System.out.println ("Totalbursts:" + totalBursts);
				System.out.println("Time: "+elapsedTime);
				System.out.println("Terms: "+termsCnt);
				System.out.println("Max seq length: "+maxSeqLength);
			}
			
			tout.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (output){
			System.out.println("Bursts' computation finished!");
			System.out.println("Total Burst Count: "+totalBursts);
			System.out.println("Avg Burst Length: "+totalLength/totalBursts);
			System.out.println("Point Burst Count: "+pointBursts);
		}
		
		
	}//End computeBursts

}
