package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Sorter {
	
	public static void quicksort(double[] main, int[] index) {
	    quicksort(main, index, 0, index.length - 1);
	}

	// quicksort a[left] to a[right]
	public static void quicksort(double[] a, int[] index, int left, int right) {
	    if (right <= left) return;
	    int i = partition(a, index, left, right);
	    quicksort(a, index, left, i-1);
	    quicksort(a, index, i+1, right);
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition(double[] a, int[] index, 
	int left, int right) {
	    int i = left - 1;
	    int j = right;
	    while (true) {
	        while (less(a[++i], a[right]))      // find item on left to swap
	            ;                               // a[right] acts as sentinel
	        while (less(a[right], a[--j]))      // find item on right to swap
	            if (j == left) break;           // don't go out-of-bounds
	        if (i >= j) break;                  // check if pointers cross
	        exch(a, index, i, j);               // swap two elements into place
	    }
	    exch(a, index, i, right);               // swap with partition element
	    return i;
	}

	// is x < y ?
	private static boolean less(double x, double y) {
	    return (x < y);
	}

	// exchange a[i] and a[j]
	private static void exch(double[] a, int[] index, int i, int j) {
	    double swap = a[i];
	    a[i] = a[j];
	    a[j] = swap;
	    int b = index[i];
	    index[i] = index[j];
	    index[j] = b;
	}
	
	
	public static int sumArray(int[] array) {
		int k;
		int sum = 0;
		for (k = 0; k < array.length; k++) {
			sum = sum + array[k];
		}
		return sum;
	}
	
	public static void findDiff() throws IOException{
		File max1File = new File("MAX1Terms.txt");
		BufferedReader maxIn = new BufferedReader(new InputStreamReader(new FileInputStream(max1File), "UTF-8") );
		
		
		File luceneFile = new File("LuceneTerms.txt");
		BufferedReader lucIn = new BufferedReader(new InputStreamReader(new FileInputStream(luceneFile), "UTF-8") );
		
		
		File termsFile = new File("DifferenceTerms.txt");
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(termsFile),"UTF-8" ));
		
		
		String line;
		ArrayList<String> maxTerms = new ArrayList<String>();
		//Read the whole file line by line
		while ((line = maxIn.readLine(  )) != null){
			maxTerms.add(line);
		}
		
		
		
		ArrayList<String> luceneTerms = new ArrayList<String>();
		//Read the whole file line by line
		while ((line = lucIn.readLine(  )) != null){
			luceneTerms.add(line);
		}
		
		
		for (String term : luceneTerms){
			if (!maxTerms.contains(term))
				out.write(term + "\n");
		}
	}
	
}
