package in.ac.iitb.semeval;

import in.ac.iitb.disamb.NormTextVectors;
import in.ac.iitb.disamb.TextVector;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;
import org.apache.jena.larq.IndexWriterFactory;
import org.apache.jena.larq.LARQ;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Stopwords;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.clusterers.SimpleKMeans;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

public class WekaBaseLine {
	
	String queryTerm;
	List<Entity> entityList;
	ArrayList<String> snippets;
	HashMap<String, Entity> assignEnt;
	HashMap<String, Double> assignAccuracy;
	HashMap<String, ArrayList<String>> clusters;
	static final Entity defaultEnt = new Entity("default","Dummy");
	int classified, std;
	double precision, recall, F1, threshold;
	HashMap<String, ArrayList<String>> semEvalOut;
	ArrayList<ArrayList<String>> finalOut;
	int queryNum;
	
	@SuppressWarnings("deprecation")
	public WekaBaseLine (int qNum, String term, String[] snippets) {
		
		queryNum = qNum;
		
		finalOut = new ArrayList<ArrayList<String>>();
		queryTerm = term;
		
		this.snippets = new ArrayList<String>();
		for (int i = 0; i < snippets.length; i++) {
			this.snippets.add(snippets[i]);
			
		}
		
				
		
	}
	void classifySnippets () throws Exception {
		
		// Converting entity descriptions into vectors
		Attribute at = new Attribute("query", (FastVector) null);
		FastVector fv = new FastVector(1);
		fv.addElement(at);
		Instances insts = new Instances("Res",fv,snippets.size());
		
		for (String snippet: snippets) {
			Instance ins = new Instance(1);
			ins.setValue((Attribute)fv.elementAt(0), snippet);
			insts.add(ins);
		}
		StringToWordVector filter = new StringToWordVector();
		filter.setUseStoplist(true);
		filter.setOutputWordCounts(true);
		filter.setInputFormat(insts);
		Instances filtInsts = Filter.useFilter(insts, filter);
		
		SimpleKMeans kmeans = new SimpleKMeans();
		kmeans.setSeed(10);
		kmeans.setPreserveInstancesOrder(true);
		kmeans.setNumClusters(6);
		
		kmeans.buildClusterer(filtInsts);
		
		for (int i = 0; i < filtInsts.numInstances(); i++) {
			
			//System.out.println(filtInsts.instance(i));
			System.out.println(snippets.get(i));
			System.out.println(snippets.get(i).substring(0, 10) + " -> " + kmeans.clusterInstance(filtInsts.instance(i)));
		}

		
	}
	
	void printClusters () {
		for (Entity ent: entityList) {
			String subtopic = ent.obj.substring(28);
			if (clusters.get(subtopic) == null)
				continue;
			System.out.println("results under " + subtopic + ":");
			for (String result: clusters.get(subtopic)) {
				System.out.println("\t" + assignAccuracy.get(result) + "\t" + result);
			}
			System.out.println();
		}
	}
	void printOutput (PrintWriter pw) {
		for (int i = 0; i < finalOut.size(); i++)
		{
			String key = Integer.toString(queryNum+1) + "." + Integer.toString(i+1);
			for (String val: finalOut.get(i))
			{
				pw.println(key + "\t" + val);
			}
			
		}
	}
	
	
	
	public static void main (String[] args) throws Exception {
		
		
		String queryFileM = "/home/rakesh/Downloads/MORESQUE/topics.txt";
		String resultFileM = "/home/rakesh/Downloads/MORESQUE/results.txt";
		String queryFileT = "/home/rakesh/Downloads/testdata/topics.txt";
		String resultFileT = "/home/rakesh/Downloads/testdata/results.txt";
		String outputFile = "/home/rakesh/Downloads/testdata/STRel.txt";
		PrintWriter pw = new PrintWriter(outputFile);
		pw.println("subTopicID\tresultID");
		
		
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
		DataSet ambDS = new DataSet(queryFile,resultFile, true);
				
		
	/*	System.out.println("Input the query term: ");
		BufferedInputStream bis = new BufferedInputStream(System.in);
		Scanner sc = new Scanner(bis);
		String term = sc.next(); */
		for (int i = 0; i < 2; i++)
		{	
			WekaBaseLine wekaClass = new WekaBaseLine(i,ambDS.data[i].query, ambDS.data[i].results);
		    
		    
		    

		    
			wekaClass.classifySnippets();
		//	wekaClass.printClusters();
		//	wekaClass.printOutput(pw);
			
		}
		pw.close();
	//	entDis.calcAccuracy(ambDS);
		
		

	}

}
