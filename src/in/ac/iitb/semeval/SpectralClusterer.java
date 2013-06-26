package in.ac.iitb.semeval;

import in.ac.iitb.semeval.TagmeJSON.Annotation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.http.auth.AuthScope;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.ApacheHttpClientState;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
class Node {
	boolean isSnippet;
	String repr;
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isSnippet ? 1231 : 1237);
		result = prime * result + ((repr == null) ? 0 : repr.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (isSnippet != other.isSnippet)
			return false;
		if (repr == null) {
			if (other.repr != null)
				return false;
		} else if (!repr.equals(other.repr))
			return false;
		return true;
	}
	public Node (boolean isSnippet, String repr) {
	
		this.isSnippet = isSnippet;
		this.repr = repr;
	}
	
}



public class SpectralClusterer {
	String queryTerm;
	List<Concept> conList;
	ArrayList<String> snippets;
	HashMap<String, Concept> assignCon;
	ArrayList<ArrayList<Annotation>> snipAnns;
	HashMap<String, ArrayList<String>> categs;
	ArrayList<ArrayList<String>> entityVecs;
	double[][] matrixData;
	
	
	double threshold;
	
	int queryNum;
	HashMap<String, ArrayList<String>> semEvalOut;
	static final Concept defaultCon = new Concept(); 
	HashMap<String, ArrayList<String>> clusters;
	String directory = "/home/rakesh/Downloads/yago_jena" ;
	Double precision, recall, F1;
	RealMatrix adjMatrix;
	
	ArrayList<Node> nodes;
	public final String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
		      + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX yago: <http://yago-knowledge.org/resource>";
	@SuppressWarnings("deprecation")
	public SpectralClusterer (int qNum, DataSet ambDS) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException, NoSuchAlgorithmException, KeyManagementException {
		
		
		
		queryTerm = ambDS.data[qNum].query;
		queryNum = qNum;
		queryTerm = ambDS.data[qNum].query;
		int n = ambDS.data[qNum].results.length;
		semEvalOut = new HashMap<String, ArrayList<String>>();
		nodes = new ArrayList<Node>();
		ArrayList<HashMap<Integer, Double>> adjList = new ArrayList<HashMap<Integer, Double>>();
		
		
		ArrayList<ArrayList<String>> entityLists;
		String path = "/home/rakesh/Downloads/final_annot/".concat(queryTerm);
		String writePath = "/home/rakesh/Downloads/mat_data/".concat(queryTerm);
		Path file = Paths.get(path);
		Path matfile = Paths.get(writePath);
		System.out.println(matfile);
		
		String[] snis = ambDS.data[qNum].results;
		for (int i = 0; i < snis.length; i++)
		{
			Node node = new Node(true, snis[i]);
			nodes.add(node);
		}

		if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) {
			
			final ApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
			Map<String, Object> configProp =  config.getProperties();
			final String proxyHost = System.getProperty("http.proxyHost");
			final String proxyPort = System.getProperty("http.proxyPort");
	
			if(proxyHost != null && proxyPort != null){
			        configProp.put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI, "http://" + proxyHost + ":" + proxyPort);
	
			        final String proxyUser = System.getProperty("http.proxyUser");
			        final String proxyPassword = System.getProperty("http.proxyPassword");
		//			System.out.println(proxyPassword);
	
			        if(proxyUser != null && proxyPassword != null){
			             ApacheHttpClientState state = config.getState();
			             state.setProxyCredentials(AuthScope.ANY_REALM, proxyHost, Integer.parseInt(proxyPort), proxyUser, proxyPassword);
	
			        }
			        
			        
					
					
			}
//			System.setProperty ("jsse.enableSNIExtension", "false");
//			
			ApacheHttpClient client = ApacheHttpClient.create(config);
//			client.setFollowRedirects(true);
			
			
			String url = "http://tagme.di.unipi.it/tag";
			WebResource wRes = client.resource(url);
//			System.out.println(client.getProperties().keySet());
			String par1 = "key";
			String val1 = "57vnafjl1";
			String par2 = "text";
			MultivaluedMap<String, String> params = new MultivaluedMapImpl();
			params.add(par1, val1);
			String val2;
			
			entityLists = new ArrayList<ArrayList<String>>();
			snipAnns = new ArrayList<ArrayList<Annotation>>();
			String[] snips = ambDS.data[qNum].results;
			for (int i = 0; i < snips.length; i++)
			{
				Node node = new Node(true, snips[i]);
				nodes.add(node);
				val2 = snips[i].replace(' ','+');
				params.add(par2, val2);
				String out = wRes.queryParams(params).get(String.class);
				ObjectMapper mapper = new ObjectMapper();
				TagmeJSON data = mapper.readValue(out, TagmeJSON.class);
				Annotation[] ann = data.getAnnotations();
				ArrayList<Annotation> annList = new ArrayList<Annotation>();
				Collections.addAll(annList, ann);
				params.remove(par2);
				snipAnns.add(annList);
			/*	ArrayList<String> titles = new ArrayList<String>();
				for (int j = 0; j < ann.length; j++)
				{
					if (Double.parseDouble(ann[j].getRho()) > 0.1)
					{
						titles.add(ann[j].getTitle());
					}
				}
				entityLists.add(titles);		
				*/
			}
	//		System.out.println(entityLists);
			System.out.println(snipAnns);
			FileOutputStream fos = new FileOutputStream("/home/rakesh/Downloads/final_annot/".concat(queryTerm));
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(snipAnns);
			os.close();
		}
		else 
		{
		
			FileInputStream fis = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fis);
			snipAnns = (ArrayList<ArrayList<Annotation>>)ois.readObject();
			ois.close();
		}
		String directory = "/home/rakesh/Downloads/Titles_TDB" ;
		Model modelTitles = TDBFactory.createModel(directory);

		entityVecs = new ArrayList<ArrayList<String>>();
		HashSet<String> entSet = new HashSet<String>();

		String prefix = "http://dbpedia.org/resource/";
		String suffix = "@en";
		Query q;
		QueryExecution qexec = null;
		for (int i = 0; i < snipAnns.size(); i++)
		{
			ArrayList<Annotation> anns = snipAnns.get(i);
		
			for (Annotation ann: anns) {

				String label = ann.getTitle();
				if (ann.getTitle() == null)
					continue;
		
				String query = "SELECT ?s WHERE {?s ?p \""+ label + "\"" + suffix + "}" ;
				q = QueryFactory.create(query);
				qexec = QueryExecutionFactory.create(q, modelTitles);
				ResultSet iteresults = qexec.execSelect();
				while (iteresults.hasNext())
				{
					String entName = iteresults.next().get("?s").toString();
				
					Node no = new Node(false, entName);
					
					if (entSet.contains(entName) == false)
					{
						nodes.add(no);
						entSet.add(entName);
					}

				}

			}
		}
		matrixData = new double[nodes.size()][nodes.size()];
		if ( Files.notExists(matfile, LinkOption.NOFOLLOW_LINKS)) {
			
			System.out.println("here");
			for (int i1 = 0; i1 < snipAnns.size(); i1++)
			{
				ArrayList<Annotation> anns1 = snipAnns.get(i1);
				ArrayList<String> vec = new ArrayList<String>();
				HashMap<Integer, Double> weights = new HashMap<Integer, Double>();
				for (Annotation ann: anns1) {

					String label = ann.getTitle();
					double rho = Double.parseDouble(ann.getRho());
					//		System.out.println(label);
					String query = "SELECT ?s WHERE {?s ?p \""+ label + "\"" + suffix + "}" ;
					q = QueryFactory.create(query);
					qexec = QueryExecutionFactory.create(q, modelTitles);
					ResultSet iteresults = qexec.execSelect();
					while (iteresults.hasNext())
					{
						String entName = iteresults.next().get("?s").toString();
						vec.add(entName);
						Node no = new Node(false, entName);
						int ind;
						if (entSet.contains(entName))
						{

							ind = nodes.indexOf(no);
							weights.put(ind, rho);
						}
						else
						{
							ind = nodes.size();
							

							

						}

					}

				}
				adjList.add(weights);
				int j = 0;
				for (String supcat: vec)
					System.out.print(j++ +". " + supcat.substring(28) + "  *  ");
				System.out.println();
				entityVecs.add(vec);
			}
			qexec.close();
			System.out.println("entSet size: " + entSet.size());
			System.out.println("nodes size: " + nodes.size());
			
			for (int r = 0; r < adjList.size(); r++)
			{
				HashMap<Integer, Double> weights = adjList.get(r);
				for (Map.Entry<Integer, Double> entry: weights.entrySet())
				{
					int c = entry.getKey();
					double w = entry.getValue();
					System.out.println("r: " + r + " c: " + c + " rho: " + w);
					matrixData[r][c] = w;
					matrixData[c][r] = w;

				}

			}
			directory = "/home/rakesh/Downloads/pagelinks_TDB";
			Model modelLinks = TDBFactory.createModel(directory);

			ArrayList<HashSet<String>> inLinks = new ArrayList<HashSet<String>>(); 
			for (int r = 64; r < nodes.size(); r++) {
				String res = nodes.get(r).repr;
				System.out.println(res);
				String query = "SELECT ?s WHERE {?s ?p <" + res + ">}";
				q = QueryFactory.create(query);
				qexec = QueryExecutionFactory.create(q, modelLinks);
				ResultSet iteresults = qexec.execSelect();
				HashSet<String> set = new HashSet<String>();

				while (iteresults.hasNext()) {
					String result = iteresults.next().get("?s").toString();
					set.add(result);
				}
				inLinks.add(set);
			}

			for (int r = 64; r < nodes.size(); r++)
			{
				for (int c = 64; c < r; c++) {

					String resA = nodes.get(r).repr;

					String resB = nodes.get(c).repr;
					/*			    String query = "SELECT (COUNT(*) AS ?count) WHERE \n {?s ?p <" + resB + "> . \n" +
	//		    		"?s ?p <" + resA + "> . \n}";
			    q = QueryFactory.create(query);
			    qexec = QueryExecutionFactory.create(q, modelLinks);
			    ResultSet iteresults = qexec.execSelect();
				int tab = 0;
			    while (iteresults.hasNext()) {
					QuerySolution soln = iteresults.nextSolution();
					tab = soln.getLiteral("count").getInt();

				}*/

					int ta = inLinks.get(r-64).size();
					int tb = inLinks.get(c-64).size();

					HashSet<String> intxn = new HashSet<String>(inLinks.get(r-64));
					intxn.retainAll(inLinks.get(c-64));
					int tab = intxn.size();

					int nArts = 9442540;
					double lta = Math.log(ta);
					double ltb = Math.log(tb);
					double ltab = Math.log(tab);
					double lnArts = Math.log(nArts);

					double edw;
					if (lta > ltb) {
						edw = (lnArts - ltb) / (lta - ltab);
					}
					else
					{
						edw = (lnArts -lta) / (ltb - ltab);
					}
					if (ta == 0 || tb == 0) edw = 0;
					System.out.println(resA.substring(28) + " - " + resB.substring(28));
					System.out.println(lta + " " + ltb + " " + lnArts + " " +ltab + " "+ edw);
					matrixData[r][c] = edw;
					matrixData[c][r] = edw;




				}

			}
			FileOutputStream fos = new FileOutputStream("/home/rakesh/Downloads/mat_data/".concat(queryTerm));
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(matrixData);
			os.close();
			qexec.close();
		}
		else 
		{
		
			FileInputStream fis = new FileInputStream(writePath);
			ObjectInputStream ois = new ObjectInputStream(fis);
			matrixData = (double[][])ois.readObject();
			ois.close();
		}
		
	//	System.out.println(matrixData);
		for (int r = 0; r < matrixData.length; r++) {
			if (r < 64) {
				for (Annotation ann: snipAnns.get(r)) {
					System.out.print(ann.getTitle() + " " + Double.parseDouble(ann.getRho()) + "; ");
				}
				System.out.println();
			}
			for (int c = 0; c < matrixData[r].length; c++) {
				
				System.out.print(matrixData[r][c] + " ");
			}
			System.out.println();
		}
		adjMatrix = MatrixUtils.createRealMatrix(matrixData);
		ArrayRealVector ones = new ArrayRealVector(nodes.size(), 1.0);
		RealVector degs = adjMatrix.operate(ones);
	//	System.out.println(degs);
		double[] degree = degs.toArray();
		int zeroCount = 0;
		ArrayList<Integer> zeroEnts = new ArrayList<Integer>();
		for (int p = 0; p < degree.length; p++)
		{
			if (degree[p] == 0.0) {
				zeroCount++;
				zeroEnts.add(p);
			}
			
		}
		System.out.println("zeroCount: " + zeroCount);
		if (true || zeroCount != 0) {
			
			int newSize = nodes.size() - zeroCount, rIndex = 0, cIndex = 0;
			double[][] modified = new double[newSize][newSize];

			for (int r = 0; r < nodes.size(); r++) {

				cIndex = 0;
				if (zeroEnts.contains(r) == false) {

					for (int c = 0; c < nodes.size(); c++) {
						if (zeroEnts.contains(c) == false) {
							modified[rIndex][cIndex] = matrixData[r][c];
							cIndex++;
						}
					}
					rIndex++;

				}
			}
			for (int in: zeroEnts) {
				nodes.remove(in);
				
			}
			adjMatrix = MatrixUtils.createRealMatrix(modified);
			
		}
		ones = new ArrayRealVector(nodes.size(), 1.0);
		int numClust;
		ArrayList<RealMatrix> matList = new ArrayList<RealMatrix>();
		ArrayList<ArrayList<Integer>> indBag = new ArrayList<ArrayList<Integer>>();
		HashMap<ArrayList<Integer>, RealMatrix> indMap = new HashMap<ArrayList<Integer>, RealMatrix>();
		HashMap<RealMatrix, ArrayList<Integer>> vertMap = new HashMap<RealMatrix, ArrayList<Integer>>();

		matList.add(adjMatrix);
		RealMatrix current = adjMatrix;
		
				
		degs = current.operate(ones);
		degree = degs.toArray();
		double[] inv = new double[degree.length];
		for (int p = 0; p < degree.length; p++) {
			inv[p] = 1.0/degree[p];
		}
		System.out.println(degs);
		RealMatrix degMat = MatrixUtils.createRealDiagonalMatrix(degree);
		
		RealMatrix invDeg = new DiagonalMatrix(inv);
		RealMatrix unNorm = degMat.subtract(current);
		RealMatrix laplacian = invDeg.multiply(unNorm);
	//	RealMatrix laplacian = unNorm;
		
		EigenDecomposition eigs = new EigenDecomposition(laplacian);
		double[] eigValues = eigs.getRealEigenvalues();
		double[] copy = eigValues.clone();
		int[] order = new int[eigValues.length];
		for (int j = 0; j < order.length; j++)
			order[j] = j;
		
		for (int j = 0; j < eigValues.length; j++)
			for (int k = j+1; k < eigValues.length; k++)
			{
				if (copy[j] < copy[k]) {
					double temp = copy[j];
					copy[j] = copy[k];
					copy[k] = temp;
					int ord = order[j];
					order[j] = order[k];
					order[k] = ord;
				}
			}
		double feidlerVal = copy[copy.length-2];
		System.out.println(new ArrayRealVector(eigValues));
		System.out.println(new ArrayRealVector(copy));
		System.out.println("Feidler Vector:");
		RealVector feidler = eigs.getEigenvector(order[copy.length-2]);
		System.out.println(feidler);

		ArrayList<String> clp = new ArrayList<String>();
		ArrayList<String> cln = new ArrayList<String>();
		
		double median = StatUtils.percentile(feidler.toArray(), 50.0);
//		double median = 0;
		System.out.println("median: " + median);
		for (int j = 0; j < 64; j++) {
			if (feidler.getEntry(j) > median) 
				clp.add(nodes.get(j).repr);
			
			else
				cln.add(nodes.get(j).repr);
			
		}
		System.out.println("cluster +ve");
		for (String s: clp) {
			System.out.println(s);
		}
		System.out.println("cluster -ve");
		for (String s: cln) {
			System.out.println(s);
		}
		/*double[][] pmatData = new double[clp.size()][clp.size()];
		for (int j = 0; j < pmatData.length; j++)
			for (int k = 0; k < pmatData[j].length; k++)
			{
				pmatData[j][k] = adjMatrix.getEntry(j, k);
			}
		
		RealMatrix childp = MatrixUtils.createRealMatrix(pmatData);
		vertMap.put(childp, clp);
		double[][] nmatData = new double[cln.size()][cln.size()];
		for (int j = 0; j < nmatData.length; j++)
			for (int k = 0; k < nmatData[j].length; k++)
			{
				nmatData[j][k] = adjMatrix.getEntry(j, k);
			}
		RealMatrix childn = MatrixUtils.createRealMatrix(nmatData);
		vertMap.put(childn, cln);
		System.out.println("cluster +ve");
		for (String s: clp) {
			System.out.println(s);
		}
		System.out.println("cluster -ve");
		for (String s: cln) {
			System.out.println(s);
		}
		*/
		
		
	/*	int numClust = 0;
		double diff = 0;
		for (int j = copy.length - 10; j >= copy.length - 20; j--) {
			if (copy[j-1] - copy[j] > diff)
			{
				diff = copy[j-1] - copy[j];
				numClust = copy.length - j; 
			}
		}
		System.out.println(numClust);
		
		RealMatrix trunc = MatrixUtils.createRealMatrix(nodes.size(), numClust);
		for (int j = 0; j < numClust; j++) {
			trunc.setColumnVector(j, eigs.getEigenvector(order[0]));
		}
		System.out.println(trunc);
		System.out.println(trunc.getRowDimension() + " " + nodes.size());
		
		ArrayList<DoublePoint> points = new ArrayList<DoublePoint>();
		for (int j = 0; j < trunc.getRowDimension(); j++) {
			
			DoublePoint point = new DoublePoint(trunc.getRow(j));
			points.add(point);
		}
		KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<DoublePoint>(numClust);
		List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(points);
		
		for (CentroidCluster<DoublePoint> clu : clusters) {
			
			System.out.println("cluster: ");
			for (DoublePoint dp: clu.getPoints())
			{
				if (points.indexOf(dp) < 64)
					System.out.println(nodes.get(points.indexOf(dp)).repr);
			}
		
		}*/
		
					
			/*		{
		}
			ArrayList<String> vec = new ArrayList<String>();
			//System.out.println(label);
			String query = "SELECT ?o WHERE {<" + ent + "> ?p ?o}" ;
			q = QueryFactory.create(query);
			qexec = QueryExecutionFactory.create(q, modelCateg);
			ResultSet iteresults = qexec.execSelect();
			while (iteresults.hasNext()) {
				String categ = iteresults.next().get("?o").toString(); 
				vec.add(categ);
				categSet.add(categ);
			}
			categs.put(ent, vec);
		}
		qexec.close();
		directory = "/home/rakesh/Downloads/Categories_TDB";
//		String yagoPrefix = "http://yago-knowledge.org/resource/";
		System.out.println("CategSet size: " + categSet.size());

		Model modelClosure = TDBFactory.createModel(directory);
		HashMap<String, ArrayList<String>> closures = new HashMap<String, ArrayList<String>>();
		for (String cat: categSet)
		{
			ArrayList<String> vec = new ArrayList<String>();
		//	System.out.println("Before: " + entVec.size());
			String pre = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>";
			String[] slices = cat.split("/");
		//	System.out.println(slices[slices.length-1]);
			String res = prefix.concat(slices[slices.length-1]);
		//	System.out.println(res);
			String query = pre + "\n" + "SELECT distinct ?o WHERE {<" + res + "> skos:broader{,2}  ?o}" ;
			q = QueryFactory.create(query, Syntax.syntaxARQ);
			qexec = QueryExecutionFactory.create(q, modelClosure);
			ResultSet iteresults = qexec.execSelect();
			while (iteresults.hasNext())
				vec.add(iteresults.next().get("?o").asResource().getLocalName());
	//		System.out.println("After: " + vec.size());
			closures.put(cat, vec);
		}
		
		for (String cat : categSet) 
		{
			System.out.print(cat.substring(37) + ": " );
			for (String supcat: closures.get(cat))
			{
				System.out.print(supcat + " * ");
			}
			System.out.println();
		}
		
		
		 Prepare catData from entityVecs 
		catData = new String[n];
		for (int i = 0; i < entityVecs.size(); i++) {
			ArrayList<String> vec = entityVecs.get(i);
			for (String ent: vec) {
				for (String categ: categs.get(ent)) {
					catData[i] = getWords(categ.substring(37));
					for (String cat: closures.get(categ))
						catData[i] = catData[i] + getWords(cat);
				}
			}
		}
		
		
		
		for (int i = 0; i < n; i++) {
			data[i][0] = ambDS.data[qNum].urls[i];
			data[i][1] = ambDS.data[qNum].titles[i];
			data[i][2] = ambDS.data[qNum].results[i] + " " + catData[i];
			System.out.println(data[i][2]);
		}
*/

	}
	String getWords (String cat) {
		String[] words = cat.split("_");
		String data = "";
		for (int i = 0; i < words.length; i++) {
			data = data.concat(words[i] + " ");
		}
		return data;
	}
	
	
	void printOutput (PrintWriter pw) {
		for (Map.Entry<String, ArrayList<String>> entry: semEvalOut.entrySet())
		{
			for (String val: entry.getValue())
			{
				pw.println(entry.getKey() + "\t" + val);
			}
			
		}
	}
	public static void main (String[] args) throws IOException, ClassNotFoundException, KeyManagementException, NoSuchAlgorithmException {
		
		
		String queryFileM = "/home/rakesh/Downloads/MORESQUE/topics.txt";
		String resultFileM = "/home/rakesh/Downloads/MORESQUE/results.txt";
		String queryFileT = "/home/rakesh/Downloads/testdata/topics.txt";
		String resultFileT = "/home/rakesh/Downloads/testdata/results.txt";
		final boolean TEST = true;
		String queryFile, resultFile;
		if (TEST) {
			queryFile = queryFileT;
			resultFile = resultFileT;
		}
		else {
			queryFile = queryFileM;
			resultFile = resultFileM;
		}
		String outputFile = "/home/rakesh/Downloads/testdata/categSTRel.txt";
		PrintWriter pw = new PrintWriter(outputFile);
		pw.println("subTopicID\tresultID");
		DataSet ambDS = new DataSet(queryFile,resultFile, true);
				
		
	/*	System.out.println("Input the query term: ");
		BufferedInputStream bis = new BufferedInputStream(System.in);
		Scanner sc = new Scanner(bis);
		String term = sc.next(); */
		for (int i = 3; i < 4; i++) {
			
		
			SpectralClusterer specClust = new SpectralClusterer(i, ambDS);
	//		catClass.getClusters();
	//		catClass.printOutput(pw);
			
		}	
	}

}

