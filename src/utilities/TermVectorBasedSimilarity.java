package utilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVectorFormat;
import org.apache.commons.math3.linear.SparseRealVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.FSDirectory;

public class TermVectorBasedSimilarity {

	public void testSimilarity() throws Exception {
		IndexReader reader = IndexReader.open(FSDirectory.open(new File(
				"/path/to/my/index")));
		// first find all terms in the index
		Map<String, Integer> terms = new HashMap<String, Integer>();
		TermEnum termEnum = reader.terms(new Term("content"));
		int pos = 0;
		while (termEnum.next()) {
			Term term = termEnum.term();
			if (!"content".equals(term.field()))
				break;
			terms.put(term.text(), pos++);
		}
		int[] docIds = new int[] { 31825, 31835, 31706, 30 };
		DocVector[] docs = new DocVector[docIds.length];
		int i = 0;
		for (int docId : docIds) {
			TermFreqVector[] tfvs = reader.getTermFreqVectors(docId);

			docs[i] = new DocVector(terms);
			for (TermFreqVector tfv : tfvs) {
				String[] termTexts = tfv.getTerms();
				int[] termFreqs = tfv.getTermFrequencies();

				for (int j = 0; j < termTexts.length; j++) {
					docs[i].setEntry(termTexts[j], termFreqs[j]);
				}
			}
			docs[i].normalize();
			i++;
		}
		// now get similarity between doc[0] and doc[1]
		double cosim01 = getCosineSimilarity(docs[0], docs[1]);
		System.out.println("cosim(0,1)=" + cosim01);
		// between doc[0] and doc[2]
		double cosim02 = getCosineSimilarity(docs[0], docs[2]);
		System.out.println("cosim(0,2)=" + cosim02);
		// between doc[0] and doc[3]
		double cosim03 = getCosineSimilarity(docs[0], docs[3]);
		System.out.println("cosim(0,3)=" + cosim03);
		reader.close();
	}

	public static double getCosineSimilarity(DocVector d1, DocVector d2) {
		return (d1.vector.dotProduct(d2.vector))
				/ (d1.vector.getNorm() * d2.vector.getNorm());
	}


}