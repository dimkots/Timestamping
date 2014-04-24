package experiments;

import burstdetection.BurstEngine;

public class ComputeBursts {

	private static String timeseriesFilename = "dfGE20timeseries.txt";
	private static String burstsFilename = "MAX1burstsDfGE20.txt";
	
	public static void main(String[] args) {
		BurstEngine.computeMAX1Bursts(timeseriesFilename, burstsFilename, 0, 0, true,1);
		//BurstEngine.computeMAX2Bursts(timeseriesFilename, burstsFilename, 0, 0, true);
	}

}
