package structures;

public class BurstInterval implements Comparable<BurstInterval>{

	private int start;
	private int end;
	private String term;
	private double score;

	public BurstInterval(int start, int end, String term, double score) {
		super();
		this.start = start;
		this.end = end;
		this.term = term;
		this.score = score;
	}
	
	/* burst is of the form: (start,end,score) */
	public BurstInterval(String burst, String term){
		String[] burstElements = burst.split(",");
		this.start = Integer.parseInt(burstElements[0].substring(1));
		this.end = Integer.parseInt(burstElements[1]);
		this.score = Double.parseDouble((burstElements[2].substring(0, burstElements[2].length()-1)));
		this.term = term;
	}
	
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	
	
	public boolean includes(int timestamp){
		return (timestamp<=end && timestamp>start);
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public double getScore() {
		return score;
	}
	
	public void setScore(double score){
		this.score = score;
	}

	
	/*
    compareTo method should return 0 if both objects are equal,
    1 if first grater than other and -1 if first less than the
    other object of the same class.
    */
	@Override
	public int compareTo(BurstInterval bint) {

		double score = bint.getScore();
		
	   if(this.getScore() >= score)    
            return 1;
       
	   return -1;
	}
	
	
}
