package experiments;

public class MaxEntDataPreparation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int datasetId = Integer.parseInt(args[0]);
		int ktfidf = -1;
		int granularity = 90;
		
		try {			
			MaxEntExp mExp = new MaxEntExp();
			mExp.updateDatasetInfo(datasetId);
			mExp.convertDataset(ktfidf, granularity);
//			mExp.computePrecision("precisionCall1.txt");
//			mExp.experiment(kJaccard, ktfidf, cliques, x);			
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}
	
	

}
