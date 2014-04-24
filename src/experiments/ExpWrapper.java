package experiments;

public class ExpWrapper {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int datasetId = Integer.parseInt(args[0]);
		
		int kJaccard = 10;
		int ktfidf = 20;
		int cliques = 1;
		int x = 30;
		boolean newTrainingSet = true;
		double trainingSetPerc = 0.90;
		long timeStart = System.currentTimeMillis();
		
		try {			
			CombineExp cExp = new CombineExp(1);
			cExp.updateDatasetInfo(datasetId);
			
			if (newTrainingSet){
//				cExp.createTestTrainingFiles(trainingSetPerc);

				cExp.buildIndex();
			}
			else
				cExp.readTrainingSet();
			
			cExp.readTestingSet();
			cExp.experiment(kJaccard, ktfidf, cliques, x);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long timeEnd = System.currentTimeMillis();
		
		double runningTime = (double)(timeEnd - timeStart)/(1000*60);
		
		System.out.println("Total running time: " + runningTime);

	}

}
