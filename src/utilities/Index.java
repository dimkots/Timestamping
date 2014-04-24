package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import experiments.CombineExp;

import structures.TDocument;

public class Index {

	private HashMap<String, Double> idfs;
	private Directory index;
	private IndexReader indexReader;
	private IndexSearcher indexSearcher;

	public Index(Directory index) throws IOException {
		idfs = new HashMap<String, Double>();
		this.index = index;

		this.indexReader = IndexReader.open(index);
		this.indexSearcher = new IndexSearcher(indexReader);

		TermEnum tEnum = indexReader.terms();
		int count = 0;
		while (tEnum.next())
			count++;

	}

	public Document[] getKNNBasedOnText(TDocument tDoc, String[] tDocTerms,
			int k) throws IOException, ParseException {
		
		HashMap<String, Integer> body = tDoc.getBody();
		String terms = "";

		for (String term : tDocTerms) {
			int freq = body.get(term);

//			for (int i = 0; i < freq; i++)
				terms += term + " ";
		}

		Document doc = new Document();
		doc.add(new Field("body", terms, Field.Store.YES, Field.Index.ANALYZED,
				Field.TermVector.YES));
		doc.add(new Field("docId", "-1", Field.Store.YES, Field.Index.ANALYZED,
				Field.TermVector.YES));

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

		String querystr = terms;
		Query q = new QueryParser(Version.LUCENE_35, "body", analyzer)
				.parse(querystr);

		int hitsPerPage = k;
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);

		indexSearcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		Document[] knn = new Document[Math.min(k, hits.length)];
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = indexSearcher.doc(docId);
			knn[i] = d;
			// System.out.println((i + 1) + ". " + d.get("title"));
		}

		return knn;
	}
	
	public DocVector getTermFreqVector(int docId,  Map<String, Integer> terms) throws IOException{
		
		DocVector docV =   new DocVector(terms);
		
		TermFreqVector[] tfvs = indexReader.getTermFreqVectors(docId);

		
		for (TermFreqVector tfv : tfvs) {
			String[] termTexts = tfv.getTerms();
			int[] termFreqs = tfv.getTermFrequencies();

			for (int j = 0; j < termTexts.length; j++) {
				docV.setEntry(termTexts[j], termFreqs[j]);
			}
		}
		docV.normalize();
		
		return docV;
		
	}
	
	
	public Map<String, Integer> getTermsMap() throws IOException{
		
		Map<String, Integer> terms = new HashMap<String, Integer>();
		
	    TermEnum termEnum = indexReader.terms(new Term("body"));
		int pos = 0;
		while (termEnum.next()) {
			Term term = termEnum.term();
			if (!"body".equals(term.field()))
				break;
			terms.put(term.text(), pos++);
		}
		
		return terms;
	}
	
	public ArrayList<Document> getDocsInInterval(int intStart, int intEnd, int k) throws IOException, ParseException {
		

		ArrayList<Document> docList = new ArrayList<Document>();
		

		Query q = NumericRangeQuery.newIntRange("timestamp", intStart, intEnd, true, true);
		
		
		int hitsPerPage = k;
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);

		indexSearcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = indexSearcher.doc(docId);
			
			docList.add(d);
			// System.out.println((i + 1) + ". " + d.get("title"));
		}

		return docList;
	}

	public Index(String idfsFile, Directory dir) throws IOException {

		this.index = dir;
		this.idfs = new HashMap<String, Double>();

		this.indexReader = IndexReader.open(index);
		this.indexSearcher = new IndexSearcher(indexReader);

		File file = new File(idfsFile);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF-8"));

		String line;
		while ((line = in.readLine()) != null) {
			String[] lineElements = line.split(":");
			this.idfs.put(lineElements[0], Double.parseDouble(lineElements[1]));
		}
		
		in.close();

	}

	public double getTfIdf(String term, int id, Directory index)
			throws CorruptIndexException, IOException, ParseException {

		

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		QueryParser queryParser = new QueryParser(Version.LUCENE_35, "docId",
				analyzer);
		Query query = queryParser.parse(String.valueOf(id));

		int hitsPerPage = 1;

		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);
		indexSearcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		int docId = hits[0].doc;
		Document d = indexSearcher.doc(docId);
		TermFreqVector tfv = indexReader.getTermFreqVector(docId, "title");

		if (tfv == null)
			return 0;

		String tterms[] = tfv.getTerms();

		int freqs[] = tfv.getTermFrequencies();
		int freqId = tfv.indexOf(term);

		if (freqId == -1)
			return 0;

		// System.out.println((i + 1) + ". " + d.get("title"));
		Term sterm = new Term(term);
		int docFreq = indexSearcher.docFreq(sterm);

		DefaultSimilarity ds = new DefaultSimilarity();
		double idf = ds.idf(docFreq, 65540);

		double tf = ds.tf(freqs[freqId]);

		// int numDocs = ir.numDocs();

		//
		// TermDocs tds = ir.termDocs();

		return 0;
	}

	public static Directory buildLuceneIndex(String datasetName, String indexPath,
			String datasetfile, Day startDay, int granularity, CombineExp combineExp)
			throws IOException {
		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

		String indexFolder = datasetName+ "Index";
		File path = new File(System.getProperty(indexPath), indexFolder);
	
		Directory index = new SimpleFSDirectory(path);

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35,
				analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		IndexWriter w = new IndexWriter(index, config);

		try {
			File dataset = new File(datasetfile);
			Reader rin = new InputStreamReader(new FileInputStream(dataset),
					"UTF-8");
			BufferedReader in = new BufferedReader(rin);

			String line;
			int count = 0;
			
			NumericField timestamp = new NumericField("timestamp", Field.Store.YES, true);
			TDocument tDoc;
			HashMap<String, Integer> map;
			String[] docTerms;
			String terms;
			Document doc;
			while ((line = in.readLine()) != null) {
				
				System.out.println(count);

				tDoc = new TDocument(line, startDay, granularity);
					
				int docTimestamp = tDoc.getTimestamp();

				docTerms = tDoc.getBodyTerms();
				map = tDoc.getBody();
				
				terms = "";

				//For each term
				for (String term : docTerms){
					int freq = map.get(term);
					
					if (combineExp!=null)
						combineExp.add(term, docTimestamp, freq);
					
					for (int i = 0; i < freq; i++)
						terms += term + " ";
				}

				doc = new Document();
//				doc.add(new Field("title", titleTerms, Field.Store.YES,
//						Field.Index.ANALYZED, Field.TermVector.YES));
				doc.add(new Field("body", terms, Field.Store.YES,
						Field.Index.ANALYZED, Field.TermVector.YES));
				doc.add(timestamp.setIntValue(tDoc.getTimestamp()));
				doc.add(new Field("docId", Integer.toString(count),
						Field.Store.YES, Field.Index.ANALYZED,
						Field.TermVector.YES));

				w.addDocument(doc);
				count++;

			}
			
			in.close();
			w.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return index;
	}

	public double entropy(String term, double totalTf) throws IOException {

		double score = 0;

		int ND = indexReader.numDocs();

		DefaultSimilarity ds = new DefaultSimilarity();
		// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
		// QueryParser queryParser = new QueryParser(Version.LUCENE_35,"body",
		// analyzer);
		int hitsNum = ND;
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsNum,
				true);

		Term qterm = new Term("body", term);
		TermQuery tquery = new TermQuery(qterm);

		indexSearcher.search(tquery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		double sumScore = 0;
		for (int i = 0; i < hits.length; i++) {

			int docId = hits[i].doc;

			TermFreqVector tfv = indexReader.getTermFreqVector(docId, "body");

			if (tfv == null) {
				System.out.println("Bug!!");
				System.exit(1);
			}

			String tterms[] = tfv.getTerms();

			int freqs[] = tfv.getTermFrequencies();
			int freqId = tfv.indexOf(term);

			// The document does not contain the term.
			if (freqId == -1) {
				System.out.println("Bug!!");
				System.exit(1);
			}

			double tf = freqs[freqId];
			// tf = ds.tf(freqs[freqId]);

			double Pdw = tf / totalTf;
			double iPdw = totalTf / tf;

			sumScore += Pdw * Math.log(Pdw);
		}

		// Add interpolation result for all the docs that do not contain the
		// term??
		int remDocs = ND - hits.length;

		score = 1 + ((double) 1 / Math.log(ND)) * sumScore;

		return score;
	}

	public void computeIdfs() throws CorruptIndexException, IOException {

		DefaultSimilarity ds = new DefaultSimilarity();

		// File termsFile = new File("CallDfs.txt");
		// BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new
		// FileOutputStream(termsFile),"UTF-8" ));

		TermEnum tEnum = indexReader.terms();
		int count = 0;
		while (tEnum.next()) {
			count++;
			Term term = tEnum.term();
			String sterm = term.text();
			int docFreq = indexSearcher.docFreq(term);
			double idf = ds.idf(docFreq, indexReader.numDocs());
			// double df = indexReader.docFreq(term);

			this.idfs.put(sterm, idf);
			// System.out.println(count);
		}
	}

	public double[] computeIdfs(String[] terms) throws IOException {

		DefaultSimilarity ds = new DefaultSimilarity();

		double[] idfs = new double[terms.length];
		// File termsFile = new File("idfs.txt");
		// BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new
		// FileOutputStream(termsFile),"UTF-8" ));

		int count = 0;
		for (String sterm : terms) {

			Term term = new Term("body", sterm);
			int docFreq = indexSearcher.docFreq(term);

			double idf = ds.idf(docFreq, indexReader.numDocs());
			// this.idfs.put(sterm, idf);
			// out.write(sterm + ":" + idf +"\n");
			// System.out.println(count);
			idfs[count++] = idf;
		}

		// out.close();
		// System.out.println(count + " terms");

		return idfs;

	}

	public double getIdf(String term) {

		Double value = this.idfs.get(term);
		
		if (value!=null)
			return value;
		else
			return 0;
	}

	public void createMinDFsFile(String outputFilename, int minDf)
			throws CorruptIndexException, IOException {

		File termsFile = new File(outputFilename);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(termsFile), "UTF-8"));

		TermEnum tEnum = indexReader.terms();
		int validCount = 0;
		int totalCount = 0;
		while (tEnum.next()) {
			totalCount++;
			Term term = tEnum.term();
			String sterm = term.text();
			int docFreq = indexSearcher.docFreq(term);

			if (docFreq >= minDf) {
				out.write(sterm + ":" + docFreq + "\n");
				validCount++;
			}
		}

		out.close();

		System.out.println("Total term count: " + totalCount);
		System.out.println("Valid term count: " + validCount);

	}

}
