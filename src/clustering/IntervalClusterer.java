package clustering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import structures.BurstInterval;
import structures.Interval;

import de.parmol.graph.MaximumClique;
import de.parmol.graph.UndirectedListGraph;

public class IntervalClusterer {

	double maxDist;
	int[][] intervals;
	int intervalsNum;

	public IntervalClusterer(List<Interval> cliqueIntervals, double maxDist) {
		this.maxDist = maxDist;

		this.intervalsNum = cliqueIntervals.size();

		intervals = new int[intervalsNum][2];

		int ptr = 0;
		for (Interval tempInt : cliqueIntervals) {
			intervals[ptr][0] = tempInt.getIntStart();
			intervals[ptr][1] = tempInt.getIntEnd();
			ptr++;
		}
	}

	public ArrayList<Interval> clusterIntervals() {

		ArrayList<Interval> clusterCenters = new ArrayList<Interval>();
		// Graph Representation
		UndirectedListGraph G = new UndirectedListGraph();

		// populate graph with one node per interval
		for (int i = 0; i < intervalsNum; ++i) {
			G.addNode(i);
			G.setNodeLabel(i, i);
		}

		// add edges
		int edgeLabel = 0;
		for (int i = 0; i < intervalsNum; ++i) {
			for (int j = i + 1; j < intervalsNum; ++j) {
				float JD = TemporalJaccardDist(intervals[i], intervals[j]);
				if (JD <= maxDist) {
					G.addEdge(i, j, edgeLabel);
					edgeLabel++;
				}

			}
		}

		MaximumClique MC = new MaximumClique();

		
		
		
		
		System.out.println();
		// while there are still nodes in the graph
		// keep looking for cliques
		while (G.getNodeCount() > 0) {
			
			
			// compute the cluster interval
			int clusterStart = -1;
			int clusterEnd = Integer.MAX_VALUE;


			// get max clique
			int clique[] = MC.getMaximumClique(G);

			// print the nodes in the clique
			
			for (int id : clique) {
				int nodeLabel = G.getNodeLabel(id);
				
				int start = intervals[nodeLabel][0];
				int end = intervals[nodeLabel][1];
				
				
				if (start>clusterStart)
					clusterStart = start;
				
				if (end<clusterEnd)
					clusterEnd = end;
				
				
				System.out.println(start+","+end+ " ");
				//System.out.println(G.getNodeLabel(id));
				G.removeNode(id);// remove this node
			}
			
			Interval cluster = new Interval(clusterStart, clusterEnd);
			clusterCenters.add(cluster);
			System.out.println("----------------");
		}
		
		return clusterCenters;
	}

	public static void main(String args[]) throws Exception {

		float maxDist = 0.5f;
		int intervalsNum = 20;
		int[][] intervals = new int[intervalsNum][2];

		// READ INTERVALS
		BufferedReader br = new BufferedReader(new FileReader("points"));
		int ptr = 0;
		while (br.ready()) {
			String[] toks = br.readLine().split(",");
			intervals[ptr][0] = Integer.parseInt(toks[0]);
			intervals[ptr][1] = Integer.parseInt(toks[1]);
			ptr++;
		}
		br.close();

	}

	private static float TemporalJaccardDist(int p1[], int p2[]) {

		int overlapStart = Math.max(p1[0], p2[0]);
		int overlapEnd = Math.min(p1[1], p2[1]);

		int unionStart = Math.min(p1[0], p2[0]);
		int unionEnd = Math.max(p1[1], p2[1]);

		if (overlapStart > overlapEnd)
			return 1f;

		return 1 - (overlapEnd - overlapStart + 1) * 1f
				/ (unionEnd - unionStart + 1);

	}
}
