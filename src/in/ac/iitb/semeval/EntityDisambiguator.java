package in.ac.iitb.semeval;

import in.ac.iitb.disamb.NormTextVectors;
import in.ac.iitb.disamb.TextVector;
import weka.core.Stopwords;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.larq.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;


import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import com.hp.hpl.jena.tdb.TDBFactory;
class Entity {
	String obj;
	String abstrct;
	public Entity (String obj, String abstrct)
	{
		this.obj = obj;
		this.abstrct = abstrct;
	}
	public boolean equals (Entity ent) {
		if (this.obj.equals(ent.obj))
			return true;
		else
			return false;
		
	}
}
public class EntityDisambiguator {
	
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
	static IndexLARQ larqIndex;
	static Model titleModel;
	String capitalize (String word) {
		
		String newWord = Character.toUpperCase(word.charAt(0)) + word.substring(1);
		
		
		return newWord;
	}
	
	String capitalizeWords (String term, int var) {
		String[] words = term.split("_");
		
		if (var == 1 && words.length == 1)
			return term.toUpperCase();
		String newTerm = "";
		newTerm += capitalize(words[0]);
		if (words.length == 1) return newTerm;
		else newTerm += "_";
		for (int i = 1; i < words.length - 1; i++) {
			//	if ((i == 0) || (i > 0 && (words[i-1].equals("the") || words[i-1].equals("da"))))
			if (Stopwords.isStopword(words[i]) == false || words[i].equals("last"))
				newTerm += capitalize(words[i]) + "_";
			else
				newTerm += words[i] + "_";
		}
		
		if (var == 0) 
			newTerm += capitalize(words[words.length - 1]);
		else
			newTerm += words[words.length - 1];
		return newTerm;
	}
	ResultSet getResults(String query, Model mod) {
		
		Query q = QueryFactory.create(query);
		
		//	ResultSet iteresults = qexec.execSelect();
		//	System.out.println(ResultSetFormatter.asText(iteresults));
			
		QueryExecution qexec = QueryExecutionFactory.create(q,mod);
		ResultSet results = qexec.execSelect();
//		qexec.close();
		return results;
	
	}
	
	@SuppressWarnings("deprecation")
	public EntityDisambiguator (int qNum, int var, String term, String[] snippets) {
		
		queryNum = qNum;
		queryTerm = capitalizeWords(term, var);
		finalOut = new ArrayList<ArrayList<String>>();
		term = queryTerm;
		System.out.println(queryTerm + " var" + var);
		this.snippets = new ArrayList<String>();
		for (int i = 0; i < snippets.length; i++) {
			this.snippets.add(snippets[i]);
			
		}
		
		

		String directory = "/home/rakesh/Downloads/Disamb_TDB" ;

		Model modelDisamb = TDBFactory.createModel(directory);



		String prefix = "http://dbpedia.org/resource/";
//		String dbpre = "PREFIX dbp: <" + prefix + ">\n PREFIX onto: <http://dbpedia.org/ontology> \n";
		String suffix = "_%28disambiguation%29";
		String query = "";

		query = "SELECT ?o WHERE {<"+prefix + term + suffix+"> ?p ?o}";


		//		    query = "SELECT ?o \n WHERE \n { ?s ?p ?o .\n" +  "FILTER regex(str(?o), \"" +prefix+ term + "\", \"i\") }";

		//System.out.println(query);
		ResultSet results = getResults(query, modelDisamb);

		if (results.hasNext() == false)
		{
			query = "SELECT ?o WHERE {<"+prefix + term + "> ?p ?o}";
			results = getResults(query, modelDisamb);
		}
		ResultSetRewindable rsrw = ResultSetFactory.copyResults(results);
		int numberOfResults = rsrw.size();
		
		if (numberOfResults < 3 && var == 1)
		{
			// use the full text search
			System.out.println("using full-text search");
			String searchString = "+" + queryTerm.replace('_',' ');
	        System.out.println(searchString);
	        String queryString = StrUtils.strjoin("\n", 
	            "PREFIX pf:     <http://jena.hpl.hp.com/ARQ/property#>",
	            "PREFIX xsd:    <http://www.w3.org/2001/XMLSchema#>" ,
	            
	            "SELECT ?o {" ,
	            "    ?lit pf:textMatch  ( '"+searchString+"' 200 )  .",
	            "?o ?p ?lit .",
	            
	            "FILTER regex(str(?o), \"" +prefix+ term + "\", \"i\")",
	            "}");
	        System.out.println(queryString);
	        results = getResults(queryString,titleModel);
	        rsrw = ResultSetFactory.copyResults(results);
	  
		}
		
		directory = "/home/rakesh/Downloads/Abstracts_TDB";
		entityList = new ArrayList<Entity>();
		Model modelAbs = TDBFactory.createModel(directory);
		while (rsrw.hasNext()) {
			
			QuerySolution result = rsrw.next();
			Resource obj = result.getResource("?o");

			query = "SELECT ?o WHERE {<" + obj.toString() + "> ?p ?o}";
			Query q = QueryFactory.create(query);
			QueryExecution qabs = QueryExecutionFactory.create(q, modelAbs);
			ResultSet finalResult = qabs.execSelect();
			if (finalResult.hasNext())
			{
				String temp = finalResult.next().getLiteral("?o").toString();
				//System.out.println(temp);
				Entity ent = new Entity(obj.toString(),temp);
				if (entityList.contains(ent) == false)
					entityList.add(ent);

			}



		}




		
		
		
	}
	void classifySnippets () {
		
		// Converting entity descriptions into vectors
		classified = 0;
		threshold = 600;
		clusters = new HashMap<String, ArrayList<String>>();
		semEvalOut = new HashMap<String, ArrayList<String>>();
		assignEnt = new HashMap<String, Entity>();
		assignAccuracy = new HashMap<String,Double>();
		ArrayList<TextVector> entVects = new ArrayList<TextVector>();
		for (int i = 0; i < entityList.size(); i++) {
			entVects.add(new TextVector(entityList.get(i).abstrct));
		}
		NormTextVectors normEntVectors = new NormTextVectors(entVects);
		
		// Converting snippets into vectors
		ArrayList<TextVector> snippetVects = new ArrayList<TextVector>();
		for (String snippet: snippets) {
			snippetVects.add(new TextVector(snippet));
		}
		NormTextVectors normSnipVectors = new NormTextVectors(snippetVects);
		int index = 0;
		for (TextVector nv: normSnipVectors.normVectors) {
			double sim, maxSim = 0;
			int maxEnt = -1; // default cluster
			for (int i = 0; i < normEntVectors.normVectors.size(); i++) {
				sim = TextVector.similarity(nv, normEntVectors.normVectors.get(i));
				if (sim > maxSim) {
					maxEnt = i;
					maxSim = sim;
				}
			}
			if (maxEnt != -1 && maxSim > threshold) 
			{	
				classified++;
				assignEnt.put(nv.text, entityList.get(maxEnt));
				assignAccuracy.put(nv.text, maxSim);
				
			
				String subtopic = entityList.get(maxEnt).obj.substring(28);
				String key = Integer.toString(queryNum+1) + "." + Integer.toString(maxEnt+1);
				String val = Integer.toString(queryNum+1) + "." + Integer.toString(index+1);
				if (clusters.containsKey(subtopic) == false)
				{
					ArrayList<String> temp = new ArrayList<String>();
					ArrayList<String> temp2 = new ArrayList<String>();
					temp.add(nv.text);
					clusters.put(subtopic, temp);
					temp2.add(val);
					semEvalOut.put(key, temp2);
				}
				else
				{
					clusters.get(subtopic).add(nv.text);
					semEvalOut.get(key).add(val);
				}
			
			}
			else
				assignEnt.put(nv.text, defaultEnt);
			index++;
		}
		index = 0;
		for (TextVector nv: normSnipVectors.normVectors)
		{
			if (assignEnt.get(nv.text).equals(defaultEnt))
			{
		//		System.out.println("*******************************");
				
				double maxSim = 0.0;
				int max = -1;
				for (int i = 0; i < entVects.size(); i++) 
				{
					if (assignEnt.containsValue(entityList.get(i)) == false)
						continue;
					double sim = TextVector.similarity(nv, entVects.get(i));
					if (sim > maxSim)
					{
						maxSim = sim;
						max = i;
					}
				}
				if (max != -1 && maxSim > 300)
				{
					//System.out.println("here2");
					String key = Integer.toString(queryNum+1) + "." + Integer.toString(max+1);
					String val = Integer.toString(queryNum+1) + "." + Integer.toString(index+1);
					assignEnt.put(nv.text, entityList.get(max));
					assignAccuracy.put(nv.text, maxSim);
					String subtopic = entityList.get(max).obj.substring(28);
					clusters.get(subtopic).add(nv.text);
					semEvalOut.get(key).add(val);
					//System.out.println(nv.text);
				}
			
			
			}
			index++;
		}
		ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>(semEvalOut.values());
		while (temp.isEmpty() == false)
		{
			int max = Integer.MIN_VALUE;
			for (int i = 0; i < temp.size(); i++) {
				if (temp.get(i).size() > max)
				{
					max = i;
				}
			}
			finalOut.add(temp.get(max));
			temp.remove(max);
		}
		

		
	}
	void printEntities () throws IOException {
		
		for (Entity ent: entityList)
		{
			System.out.println(ent.obj.substring(28));
			
			
		//	System.out.println(ent.abstrct);
			
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
	
	void printAssignments () {
		
		for (String snippet: snippets) {
			System.out.println("===================");
			System.out.println(snippet);
			System.out.println("**** Assigned Entinty: " + assignEnt.get(snippet).obj + " ****");
			System.out.println();
		}
		
	}
	
	/*void calcAccuracy (DataSet ds) {
		
		precision = 0.0;
		std = 0;
		recall = 0.0;
		HashMap<String, ArrayList<String>> standard = ds.clusters.get(queryTerm).clusterMap;
	//	System.out.println(clusters.size());
		for (Map.Entry<String, ArrayList<String>> entry: clusters.entrySet())
		{
			String subtopic = entry.getKey();
			if (subtopic.startsWith("http"))
				subtopic = subtopic.substring(28);
			int noClass = 0;
			if (standard.containsKey(subtopic) == false) continue;
			Double pre = 0.0, rec = 0.0;
			for (String result: entry.getValue()) {
			//	System.out.println(result);
			//	System.out.println(ds.resultMap.get("28.34"));
				if (ds.standardAssign.get(result) == null) 
				{
					noClass++;
					continue;
				}
				if (ds.standardAssign.get(result).equals(subtopic))
				{
					//System.out.println(result);
					pre += 1;
				}
					
				
				
			}
			rec = pre;
			if (noClass != entry.getValue().size())
				pre = pre * 100 / (entry.getValue().size() - noClass);
			else continue;
			rec = rec * 100 / standard.get(subtopic).size();
			
			pre = pre * standard.get(subtopic).size() / ds.clusters.get(queryTerm).size;
			rec = rec * standard.get(subtopic).size() / ds.clusters.get(queryTerm).size;
			precision += pre;
			recall += rec;
		}
		
		F1 = 2 * precision * recall / (precision + recall);
		
		System.out.println("Precision: " + precision);
		System.out.println("Recall: " + recall);
		System.out.println("F1: " + F1);
		
		
		for (Map.Entry<String, ArrayList<String>> entry: standard.entrySet()) 
		{
			String subtopic = entry.getKey();
			//System.out.println(entry.getValue());
			//System.out.println(subtopic);
			if (clusters.containsKey(subtopic) == false)
			{
				System.out.println("Missing subtopic: " + subtopic);
				System.out.println(entry.getValue());
				continue;
			}
			Double pre = 0.0, rec = 0.0;
			for (String result: entry.getValue())
			{
				String mytopic = assignEnt.get(result).obj;
			//	System.out.println(mytopic);
				if (mytopic.startsWith("http"))
					mytopic = mytopic.substring(28);
				if (mytopic.equals(subtopic))
					pre += 1.0;
					
			}
			rec = pre;
			pre = pre * 100;
	//		System.out.println("pre value " + pre);
			rec = rec * 100 / entry.getValue().size();
			rec = rec *  clusters.get(subtopic).size();
			precision += pre;
			recall += rec;
			std += entry.getValue().size();
			
		}
		
		for (Map.Entry<String, ArrayList<String>> entry: clusters.entrySet())
		{
			String subtopic = entry.getKey();
			if (standard.containsKey(subtopic) == false) continue;
			Double pre = 0.0, rec = 0.0;
			for (String result: entry.getValue())
			{
			//	System.out.println(result);
			//	System.out.println(ds.resultMap.get("28.34"));
				if (ds.standardAssign.get(result) == null)
				{
					System.out.println("bad");
					continue;
				}
				if (ds.standardAssign.get(result).equals(subtopic))
				{
						pre += 1;
						System.out.println("good");
				}
								
			}
			
			rec = pre;
			pre = pre * 100 / entry.getValue().size();
			System.out.println("pre value " + pre);
			rec = rec * 100 / standard.get(subtopic).size();
			pre = pre * entry.getValue().size();
			rec = rec * entry.getValue().size();
			precision += pre;
			recall += rec;
			std += entry.getValue().size();
		}
		
		
		
	}*/
	
	@SuppressWarnings("deprecation")
	public static void main (String[] args) throws IOException {
		
	
		String queryFileM = "/home/rakesh/Downloads/MORESQUE/topics.txt";
		String resultFileM = "/home/rakesh/Downloads/MORESQUE/results.txt";
		String queryFileT = "/home/rakesh/Downloads/testdata/topics.txt";
		String resultFileT = "/home/rakesh/Downloads/testdata/results.txt";
		String outputFile = "/home/rakesh/Downloads/testdata/STRel.txt";
		PrintWriter pw = new PrintWriter(outputFile);
		pw.println("subTopicID\tresultID");
		boolean NOINDEX = false;
		String directory = "/home/rakesh/Downloads/Titles_TDB" ;
		
		titleModel = TDBFactory.createModel(directory);
		
		
		if (NOINDEX) {
			// create the index
			
			(new IndexCreator(titleModel)).getIndex();
		}
		
		IndexWriter indWriter = IndexWriterFactory.create(FSDirectory.open(new File("/home/rakesh/Downloads/LarqIndex")));
		
		IndexBuilderString larqBuilder = new IndexBuilderString(indWriter);
		larqIndex = larqBuilder.getIndex();
		LARQ.setDefaultIndex(larqIndex);
		
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
		for (int i = 0; i < ambDS.data.length; i++)
		{	
			EntityDisambiguator entDis = new EntityDisambiguator(i, 0, ambDS.data[i].query, ambDS.data[i].results);
		    if (entDis.entityList.size() == 0) 
		    	entDis = new EntityDisambiguator(i, 1, ambDS.data[i].query, ambDS.data[i].results);
		    
		    
		    

		    entDis.printEntities();
			entDis.classifySnippets();
			entDis.printClusters();
			entDis.printOutput(pw);
			
		}
		pw.close();
	//	entDis.calcAccuracy(ambDS);
		
		

	}

}
