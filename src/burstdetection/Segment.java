package burstdetection;


/**
 * @author Ted Lappas <br>
 * <br>
 *         The Segment class represent a Maximal Segment
 * 
 */
public class Segment implements Comparable<Segment> {


	public int from, to;
	public float L, R;
	public float val;

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public float getL() {
		return L;
	}

	public void setL(float l) {
		L = l;
	}

	public float getR() {
		return R;
	}

	public void setR(float r) {
		R = r;
	}

	public Segment(int f, int t, float v, float total) {
		from = f;
		to = t;
		L = total - v;
		R = total;
	}


	public float getScore() {

		return R - L;
	}

	public int getLen() {
		return to - from + 1;
	}

	public float getVal() {
		return val;
	}

	public void setVal(float val) {
		this.val = val;
	}

	

	public int compareTo(Segment s) {
		if (s.val == val) {
			return 0;
		} else if (val < s.val) {
			return -1;
		} else {
			return +1;
		}
	}
	


}
