package wekaClustering;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import clustering.DataPoint;

import structures.Interval;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.CSVLoader;

public class HAClusterer {

	Instances data;
	DistanceFunction d;
	HierarchicalClusterer h;
	Attribute x;
	Attribute y; 
	
	public HAClusterer() throws FileNotFoundException, IOException {
		d = new EuclideanDistance();
		h = new HierarchicalClusterer();
		h.setDistanceFunction(d);
		SelectedTag s = new SelectedTag(1, HierarchicalClusterer.TAGS_LINK_TYPE);
		h.setLinkType(s);
		
		x = new Attribute("x"); 
		y = new Attribute("y"); 
		
		
		String nameOfDataset = "CliquesClusteringProblem";
		
		 // Create vector of attributes.
	    FastVector attributes = new FastVector(2);
	    
	    // Add attribute for holding messages.
//	    attributes.addElement(new Attribute("x", (FastVector)null));
//	    attributes.addElement(new Attribute("y", (FastVector)null));
	    
	    attributes.addElement(x);
	    attributes.addElement(y);
	    
	    // Add class attribute.
//	    FastVector classValues = new FastVector(2);
//	    classValues.addElement("miss");
//	    classValues.addElement("hit");
//	    attributes.addElement(new Attribute("Class", classValues));

	    // Create dataset with initial capacity of 100, and set index of class.
	    data = new Instances(nameOfDataset, attributes, 100);
	    data.setClassIndex(data.numAttributes() - 1);
	    
//		data = new Instances(
//		         new BufferedReader(
//		           new FileReader("header.arff")));
//		
//		data.setClassIndex(data.numAttributes() - 1);
//		
		
		
		  

		   

		   
		
	}
	
	
	public void loadData(List<Interval> cliqueIntervals) {
		
//		 Instance xyz = new Instance(dataset.numAttributes());  // [1]
//	     xyz.setDataset(dataset);                   // [2]
//	     xyz.setValue(dataset.attribute(0), 1.0);   // [3]
//	     System.out.println(xyz);
		
		for (int i=0;i<cliqueIntervals.size();i++){
			
			Interval int1 = cliqueIntervals.get(i);
			Instance inst = new Instance(data.numAttributes());
			inst.setDataset(data);
			inst.setValue(data.attribute(0), (double)int1.getIntStart());
			inst.setValue(data.attribute(1), (double)int1.getIntEnd());
			data.add(inst);
			//dataPoints.add(new DataPoint(int1.getIntStart(),int1.getIntEnd(),"clique"));
	 }
		
	}
	

	public void analyzeClusters() {

		//Instances data = null;
		
		try {
			
			h.buildClusterer(data);

			 double[] arr;
			 for(int i=0; i<data.numInstances(); i++) {
			
			 arr = h.distributionForInstance(data.instance(i));
			 for(int j=0; j< arr.length; j++)
			 System.out.print(arr[j]+",");
			 System.out.println();
			
			 }

			System.out.println(h.numberOfClusters());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
