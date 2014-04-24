package structures;

public abstract class BDocument {

	private String body;
	
	//Blogs
	private String docUrl;

	private int timestamp;
	
	
	//News
	public BDocument(int timestamp, String body) {
		super();
		this.timestamp = timestamp;
		this.body = body;
	}
	
	
	/* line is of the form: 
	 * 41\tSun Oct 12 2008\tlimit:1 expert:1 of:1 welcom:1 deci:1 insur:1 deposit:1 presid:1 on:1 foreign:1 increa:1 payout:1 	2009:6 
	 * timestemp day month date year tterm1:ttfreq1 .. ttermN:ttfreqN \tbterm1:btfreq1 .. btermM:btfreqM \t topicsString */
	public BDocument(String line, String country) {
		super();
		String[] lineElements = line.split("\t");
		
		this.timestamp = Integer.parseInt(lineElements[0]);
		this.body = lineElements[1];
		this.docUrl = lineElements[2];
	}
	
	
	/* line is of the form: docTimestamp;docBody;docUrl */
	public BDocument(String line) {
		super();
		String[] lineElements = line.split(";");
		
		this.timestamp = Integer.parseInt(lineElements[0]);
		this.body = lineElements[1];
		this.docUrl = lineElements[2];
	}



	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getDocUrl() {
		return docUrl;
	}

	public void setDocId(String docUrl) {
		this.docUrl = docUrl;
	}


	public String[] getRepresentativeTerms() {
		// TODO Auto-generated method stub
		return null;
	}

}
