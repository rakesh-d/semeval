
/** Exact Implemenatation of Yago Clustering Paper */

package in.ac.iitb.disamb;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

class Concept {
	Resource obj;
	boolean isAbstrct;
	String factString;
	
	// Dummy default concept constructor
	public Concept () {
		// create an empty Model
		Model model = ModelFactory.createDefaultModel();

		// create the resource
		obj = model.createResource("http://yago-knowledge.org/resource/Default");
		

	}
	
	public Concept (Resource obj, boolean isAbs )
	{
		this.obj = obj;
		this.isAbstrct = isAbs;
	}
}

public class YagoDisambiguator {
	
	String queryTerm;
	List<Concept> conList;
	ArrayList<String> snippets;
	HashMap<String, Concept> assignCon;
	HashMap<String, Double> assignAccuracy;
	double threshold;
	static final Concept defaultCon = new Concept(); 
	HashMap<String, ArrayList<String>> clusters;
	String directory = "/home/rakesh/Downloads/yago_jena" ;
	Double precision, recall, F1;
	
	public final String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
		      + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX yago: <http://yago-knowledge.org/resource>";
	
	void addPrimaryConcept(Model modelDisamb) {
		String term = queryTerm;
		
		String wikiUrl = "http://en.wikipedia.org/wiki/" + term;
		System.out.println(wikiUrl);
		String query = "SELECT ?s WHERE { ?s yago:hasWikipediaUrl \"" + wikiUrl + "\" }";
		query = prefixes + "\n" + query;
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, modelDisamb);
		ResultSet results = qexec.execSelect();
		while (results.hasNext()) {
			QuerySolution result = results.next();
			Resource res = result.getResource("?s");
			System.out.println(res.toString());
			Concept con = new Concept(res, true);
			conList.add(con);
		
			
		}
	}

	public YagoDisambiguator (String term, String[] titles, String[] snippets) {
		
		
		queryTerm = term;
		String yagoPrefix = "http://yago-knowledge.org/resource/";
		this.snippets = new ArrayList<String>();
		for (int i = 0; i < snippets.length; i++) {
			if (snippets[i] != null) 
				this.snippets.add(titles[i] + " " + snippets[i]);
			else if (titles[i] != null)
				this.snippets.add(titles[i]);
		}
		
		
		  @SuppressWarnings("deprecation")
		Model modelDisamb = TDBFactory.createModel(directory);
		
		//addPrimaryConcept(modelDisamb);
	
		
		// Getting Direct Concepts
		String query = "SELECT DISTINCT ?s WHERE { ?s ?p \""  + term + "\"}";
		
		
		
		query = prefixes + "\n" + query;
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, modelDisamb);
		ResultSet results = qexec.execSelect();
		conList = new ArrayList<Concept>();
		
	//	System.out.println(ResultSetFormatter.asText(results));
		
		while (results.hasNext()) {
			
			QuerySolution result = results.next();
			Resource obj = result.getResource("?s");
			//System.out.println(testAbstract(term,obj,modelDisamb));
			
			
			if (testAbstract(term, obj, modelDisamb))
			{
				// Getting type parents
				query = "SELECT ?o where { <" + obj.toString() + "> rdf:type " + "?o }";
				query = prefixes + "\n" + query;
				q = QueryFactory.create(query);
				qexec = QueryExecutionFactory.create(q, modelDisamb);
				ResultSet parents = qexec.execSelect();
				while (parents.hasNext()) {
					QuerySolution parent = parents.next();
					Resource res = parent.getResource("?o");
					Concept con = new Concept(res, true);
					conList.add(con);
					
				}
				
				// Getting children class concepts
				getChildConcepts(obj,modelDisamb);
				
				
				// Getting peer concepts
				
				query = "SELECT ?o WHERE { <" + obj.toString() + "> rdfs:subClassOf ?o }";
				query = prefixes + "\n" + query;
				q = QueryFactory.create(query);
				qexec = QueryExecutionFactory.create(q, modelDisamb);
				ResultSet superClasses = qexec.execSelect();
				while (superClasses.hasNext()) {
					QuerySolution parent = parents.next();
					Resource res = parent.getResource("?o");
					getChildConcepts(res, modelDisamb);
					
				}
				
			}
			else {
				Concept con = new Concept(obj, false);
				conList.add(con);
			}
			
		}
		
		getFacts(modelDisamb);
		
		
		qexec.close();
		
	}
	
	void getChildConcepts (Resource obj, Model modelDisamb) {
		
		String query = "SELECT ?s WHERE { ?s rdfs:subClassOf <" + obj.toString() + "> }";
		query = prefixes + "\n" + query;
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, modelDisamb);
		ResultSet children = qexec.execSelect();
		while (children.hasNext()) {
			QuerySolution child = children.next();
			Resource res = child.getResource("?o");
			Concept con = new Concept(res, true);
			conList.add(con);
			
		}
		
	}
	boolean testAbstract(String term, Resource res, Model model) {
		
		//System.out.println(res.getLocalName() + " " + term);
		if (res.getLocalName().toLowerCase().equals(term.toLowerCase()))
			return true;
		else {
			String query = "ASK { ?sub rdfs:subClassOf <" + res.toString() + "> }";
			query = prefixes + "\n" + query;
			Query q = QueryFactory.create(query);
			QueryExecution qexec = QueryExecutionFactory.create(q, model);
			return qexec.execAsk();
			
		}
	}
	void getFacts (Model model) {
		for (Concept con: conList) {
			
			String propQuery = "SELECT DISTINCT ?o WHERE {<" + con.obj.toString() + "> ?p ?o }";
			QueryExecution qexec = QueryExecutionFactory.create(propQuery, model);
			ResultSet rs = qexec.execSelect();
		//	System.out.println(ResultSetFormatter.asText(rs));
			
			String factString = "";
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				RDFNode res = sol.get("?o");
				if (res.isLiteral()) {
					String[] words = res.toString().split("[ -]");
					for (int i = 0; i < words.length; i++) {
						if (words[i].startsWith("http://en.wiki"))
							continue;
						if (words[i].startsWith("(")) {
							if (words[i].length() > 1)
								words[i] = words[i].substring(1, words[i].length()-1);
							else
								continue;
						}
						factString = factString.concat(words[i] + " ");
					}
				}
				
				else if (res.isResource()) {
					String local = res.asResource().getLocalName();
					if (local.startsWith("wikicategory")) {
						String[] words = local.split("[_-]");
						for (int i = 1; i < words.length; i++) {
							if (words[i].startsWith("(")) {
								words[i] = words[i].substring(1, words[i].length()-1);
							}
							factString = factString.concat(words[i] + " ");
						}
					}
				}
				
			}
			con.factString = factString;
		}
	}
	void printConcepts () {
		
		for (Concept con: conList) {
			System.out.println(con.obj.toString());
			//System.out.println(con.factString);
		}
		
	}
	
	void classifySnippets (double th) {
		
		threshold = th;
		
		clusters = new HashMap<String, ArrayList<String>>();
		assignCon = new HashMap<String, Concept>();
		assignAccuracy = new HashMap<String, Double>();
		List<TextVector> vecList = new ArrayList<TextVector>();
		for (String snippet: snippets) {
			TextVector tv = new TextVector(snippet);
			vecList.add(tv);
		}
		NormTextVectors nVectors = new NormTextVectors(vecList);
	//	nVectors.printVectors();
		List<TextVector> conVectors = new ArrayList<TextVector>();
		for (Concept con: conList) {
			TextVector conVect = new TextVector(con.factString);
			conVectors.add(conVect);
		}
	//	HashMap<Concept, Boolean> assigned = new HashMap<Concept, Boolean>();
		
		for (TextVector nv: nVectors.normVectors) {
			double maxSim = 0.0;
			int max = -1;
			for (int i = 0; i < conVectors.size(); i++) 
			{
				double sim = TextVector.similarity(nv, conVectors.get(i));
				if (sim > maxSim)
				{
					maxSim = sim;
					max = i;
				}
			}
			if (max != -1 && maxSim > threshold)
			{
				//System.out.println("here2");
				
				assignCon.put(nv.text, conList.get(max));
			//	assigned.put(conList.get(max), true);
				assignAccuracy.put(nv.text, maxSim);
				String subtopic = conList.get(max).obj.getLocalName();
				if (clusters.containsKey(subtopic) == false)
				{
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(nv.text);
					clusters.put(subtopic, temp);
				}
				else
					clusters.get(subtopic).add(nv.text);
				//System.out.println(nv.text);
			}
			else
			{
				//System.out.println("here");
				assignCon.put(nv.text, defaultCon);
			//	System.out.println("*** " + maxSim + " ***" + nv.text);
			}    
			
		}
		for (TextVector nv: nVectors.normVectors) {
			if (assignCon.get(nv.text).obj.toString().equals("http://yago-knowledge.org/resource/Default"))
			{
		//		System.out.println("*******************************");
				double maxSim = 0.0;
				int max = -1;
				for (int i = 0; i < conVectors.size(); i++) 
				{
					if (assignCon.containsValue(conList.get(i)) == false)
						continue;
					double sim = TextVector.similarity(nv, conVectors.get(i));
					if (sim > maxSim)
					{
						maxSim = sim;
						max = i;
					}
				}
				if (max != -1)
				{
					//System.out.println("here2");
					
					assignCon.put(nv.text, conList.get(max));
					assignAccuracy.put(nv.text, maxSim);
					String subtopic = conList.get(max).obj.getLocalName();
					clusters.get(subtopic).add(nv.text);
					//System.out.println(nv.text);
				}
			}
		}
		
	}
	void printAssignments () {
		
		for (String snippet: snippets) {
			System.out.println("===================");
			System.out.println(snippet);
			System.out.println("**** Assigned Entinty: " + assignCon.get(snippet).obj + " ****");
			System.out.println();
		}
		
	}
	void printClusters () {
		for (Concept con: conList) {
			String subtopic = con.obj.toString().substring(35);
			if (clusters.get(subtopic) == null)
				continue;
			System.out.println("results under " + subtopic + ":");
			for (String result: clusters.get(subtopic)) {
				System.out.println("\t" + assignAccuracy.get(result) + "\t" + result);
			}
			System.out.println();
		}
	}
	
	void calcAccuracy (DataSet ds) {
		
		precision = 0.0;
		recall = 0.0;
		HashMap<String, ArrayList<String>> standard = ds.clusters.get(queryTerm).clusterMap;
		for (Map.Entry<String, ArrayList<String>> entry: clusters.entrySet())
		{
			String subtopic = entry.getKey();
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
			pre = pre * 100 / (entry.getValue().size() - noClass);
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
		
	}
	
	public static void main (String[] args) throws IOException {
		
	
		String queryFile = "/home/rakesh/Downloads/ambient/yagotopics.txt";
		String resultFile = "/home/rakesh/Downloads/ambient/myresults.txt";
		String subtopics = "/home/rakesh/Downloads/ambient/mysubTopics.txt";
		String assoc = "/home/rakesh/Downloads/ambient/mySTRel.txt";
		DataSet ambDS = new DataSet(queryFile,resultFile,subtopics, assoc, 10);
				
		
	/*	System.out.println("Input the query term: ");
		BufferedInputStream bis = new BufferedInputStream(System.in);
		Scanner sc = new Scanner(bis);
		String term = sc.next(); */
		YagoDisambiguator yagoDis = new YagoDisambiguator(ambDS.queryString[3], ambDS.resultTitle[3],ambDS.resultSnippet[3]);
		yagoDis.printConcepts();
	//	System.out.println(YagoDisambiguator.defaultCon.obj);
		
		
		yagoDis.classifySnippets(60);
		yagoDis.printClusters();
		yagoDis.calcAccuracy(ambDS);
		
			
		
	
		
//		for (int i = 0; i < 15; i++)
//			System.out.print(prec[i] + " ");
//		System.out.println();
//		for (int i = 0; i < 15; i++)
//			System.out.print(rec[i] + " ");
//		System.out.println();
//		for (int i = 0; i < 15; i++)
//			System.out.print(f1[i] + " ");
//		System.out.println();
//		
		
		
		

	}

}
