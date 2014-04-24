package burstdetection;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

/**
 * @author Ted Lappas <br>
 * <br>
 *         Implementation of the Ruzzo-Tompa Alg
 * 
 */
public class Ruzzo {

	private static final String TSFILENAME = "testtimeseries.txt";
	
	// returns Vector of all maximal segments
	// input is the seque
	public Vector<Segment> getMaximalIntervals(Vector<Float> seq) throws IOException {

		// get baseline
		// float avg = // 2*
		// this.getAvg(seq);

		// float euclid = 1f / seq.size();
		// System.out.println(euclid);
		Vector<Segment> v = new Vector<Segment>();
		Segment newsub;
		float R = 0;
		for (int i = 0; i < seq.size(); i++) {
				
			float next = seq.get(i);// -avg;
			// float next=(seq.get(i) / avg)-euclid;
			// if(seq.get(i)<0||next>1)

			R += next; //after the removal of the baseline
			
		//	System.out.println("Id: " + i + " Value:" + next);
			if (next > 0) {// ignore negative values
				newsub = new Segment(i, i, next, R);// init segment
				insertSegment(newsub, v, R);// insert segment
			}
		}

		if (v.size() == 0) {// no maximal segments found
			return null;
		}

		return v;
	}

	// processes new segment for maximality
	private void insertSegment(Segment newsub, Vector<Segment> v, float R) {
		int max = -10000; // small number

		// going from Right to Left, find the first segment I_j that has an L
		// smaller than the newbie
		for (int j = v.size() - 1; j >= 0; j--) {
			if (v.get(j).getL() < newsub.getL()) {
				max = j;
				break;
			}
		}
		
		//2. and 3. point of the paper
		if (max == -10000// no segment was found, or segment was found but it
				// also has an R >= than newbie's
				|| (max != -10000 && v.get(max).getR() >= newsub.getR())) {
			newsub.val = newsub.getScore();
			v.add(newsub);
		} else if (max != -1 && v.get(max).getR() < newsub.getR()) {

			// extend newbie to cover all up to Ij
			newsub.setFrom(v.get(max).getFrom());
			newsub.setL(v.get(max).getL());// update L
			newsub.setR(R);// update R

			// Remove non-maximal segments
			while (v.size() > max) {
				v.removeElementAt(v.size() - 1);
			}
			
			// Here, v contains only maximal segments.
			

			// re-consider extended newbie
			insertSegment(newsub, v, R);
		} else {

			// Do nothing.

		}
	}

	public float getAvg(Vector<Float> seq) {
		float avg = 0;

		for (int i = 0; i < seq.size(); i++) {
			avg += seq.get(i);
		}
		return avg / seq.size();

	}
	
	public Vector<Float> removeAvg(Vector<Float> seq){
		float avg = this.getAvg(seq);
		float value; 
		int u = 10;
		for (int i = 0; i < seq.size(); i++) {
			value = seq.get(i);
			seq.set(i, value-avg);
			
		}
		return seq;
	}
	
	
	
	public Vector<Float> removeKAvg(Vector<Float> seq, int k){
		float avg = this.getAvg(seq);
		float value; 
		
		for (int i = 0; i < seq.size(); i++) {
			value = seq.get(i);
			seq.set(i, value-k*avg);
			
		}
		return seq;
	}


	public static void main(String args[]){

		Ruzzo R = new Ruzzo();
		Vector<Float> seq = new Vector<Float>();
		String line = null;
		
		// Read the test time-series from TSFILENAME file
		try {
			FileReader fr = new FileReader ( TSFILENAME );
			BufferedReader in = new BufferedReader( fr );
			String tmp;

			//Read the last line of the file
			while ((tmp = in.readLine(  )) != null) line = tmp;
			in.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
			 
		// Create a  Vector<Float> from a line of the form "1 2 -3 4 -5 ..."
		String[] lineArray = line.split(" ");
		for (String valueStr : lineArray){
			seq.add(Float.parseFloat(valueStr));
		}
		
		
		R.removeAvg(seq);
		Vector<Segment> segs;
		
		try {
			segs = R.getMaximalIntervals(seq);
			for (Segment s:segs)
				System.out.println(s.from+"\t"+s.to+"\t"+s.val);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			
	
		
	}

}