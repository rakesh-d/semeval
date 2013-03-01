
package in.ac.iitb.disamb;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.query.*;
import java.util.*;


import java.io.*;
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
	
	public EntityDisambiguator (String term, String[] titles, String[] snippets) {
		
		
		queryTerm = term;
		System.out.println(term);
		this.snippets = new ArrayList<String>();
		for (int i = 0; i < snippets.length; i++) {
			if (snippets[i] != null) 
				this.snippets.add(titles[i] + " " + snippets[i]);
			else if (titles[i] != null)
				this.snippets.add(titles[i]);
		}
		
		String directory = "/home/rakesh/Downloads/Disamb_TDB" ;
		  @SuppressWarnings("deprecation")
		Model modelDisamb = TDBFactory.createModel(directory);
		
		String prefix = "http://dbpedia.org/resource/";
		String suffix = "_%28disambiguation%29";
		String query = "SELECT ?o WHERE {<"+prefix + term + suffix+"> ?p ?o}";
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.create(q, modelDisamb);
	//	ResultSet iteresults = qexec.execSelect();
	//	System.out.println(ResultSetFormatter.asText(iteresults));
		qexec.close();
		qexec = QueryExecutionFactory.create(q,modelDisamb);
		ResultSet results = qexec.execSelect();
		directory = "/home/rakesh/Downloads/Abstracts_TDB";
		entityList = new ArrayList<Entity>();
		@SuppressWarnings("deprecation")
		Model modelAbs = TDBFactory.createModel(directory);
		while (results.hasNext()) {
			QuerySolution result = results.next();
			Resource obj = result.getResource("?o");
			//System.out.println("test: " + obj.toString());
//			if (obj.toString().contains("disambiguation")) {
//				query = "SELECT DISTINCT ?o WHERE { <" + obj.toString() + "> ?p ?o }";
//				q = QueryFactory.create(query);
//				qexec = QueryExecutionFactory.create(q, modelDisamb);
//				ResultSet tempResults = qexec.execSelect();
//				while (tempResults.hasNext()) {
//					QuerySolution sol = tempResults.next();
//					Resource obj1 = sol.getResource("?o");
//					//System.out.println();
//					//System.out.println("===== " + obj1.toString()+ " =====");
//					query = "SELECT DISTINCT ?o WHERE {<" + obj1.toString() + "> ?p ?o}";
//					q = QueryFactory.create(query);
//					QueryExecution qabs0 = QueryExecutionFactory.create(q, modelAbs);
//					ResultSet finalResult0 = qabs0.execSelect();
//					if (finalResult0.hasNext()) 
//					{
//						String temp = finalResult0.next().getLiteral("?o").toString();
//					//	System.out.println(temp);
//						Entity ent = new Entity(obj1.toString(),temp);
//						if (entityList.contains(ent) == false)
//							entityList.add(ent);
//					}
//					else
//						//System.out.println("No Abstract");
//					qabs0.close();
//					
//				}
//				
//			}
//			else
//			{
				//System.out.println();
				//System.out.println("===== " + obj.toString()+" =====");
				query = "SELECT DISTINCT ?o WHERE {<" + obj.toString() + "> ?p ?o}";
				q = QueryFactory.create(query);
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
				else
					//System.out.println("No Abstract");
				qabs.close();
	//		}
		
				
		}
		qexec.close();
	}
	void classifySnippets () {
		
		// Converting entity descriptions into vectors
		classified = 0;
		threshold = 1000;
		clusters = new HashMap<String, ArrayList<String>>();
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
				if (clusters.containsKey(subtopic) == false)
				{
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(nv.text);
					clusters.put(subtopic, temp);
				}
				else
					clusters.get(subtopic).add(nv.text);
			
			}
			else
				assignEnt.put(nv.text, defaultEnt);
		}
		for (TextVector nv: normSnipVectors.normVectors) {
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
				if (max != -1)
				{
					//System.out.println("here2");
					
					assignEnt.put(nv.text, entityList.get(max));
					assignAccuracy.put(nv.text, maxSim);
					String subtopic = entityList.get(max).obj.substring(28);
					clusters.get(subtopic).add(nv.text);
					//System.out.println(nv.text);
				}
			}
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
	
	void printAssignments () {
		
		for (String snippet: snippets) {
			System.out.println("===================");
			System.out.println(snippet);
			System.out.println("**** Assigned Entinty: " + assignEnt.get(snippet).obj + " ****");
			System.out.println();
		}
		
	}
	void calcAccuracy (DataSet ds) {
		
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
		
		/*
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
		*/
		
		
	}
	
	public static void main (String[] args) throws IOException {
		
	
		String queryFile = "/home/rakesh/Downloads/ambient/mytopics.txt";
		String resultFile = "/home/rakesh/Downloads/ambient/myresults.txt";
		String subtopics = "/home/rakesh/Downloads/ambient/mysubTopics.txt";
		String assoc = "/home/rakesh/Downloads/ambient/mySTRel.txt";
		DataSet ambDS = new DataSet(queryFile,resultFile,subtopics,assoc,10);
		
	/*	System.out.println("Input the query term: ");
		BufferedInputStream bis = new BufferedInputStream(System.in);
		Scanner sc = new Scanner(bis);
		String term = sc.next(); */
		EntityDisambiguator entDis = new EntityDisambiguator(ambDS.queryString[2], ambDS.resultTitle[2],ambDS.resultSnippet[2]);
		entDis.printEntities();
		entDis.classifySnippets();
		entDis.printClusters();
		entDis.calcAccuracy(ambDS);
		
		

	}

}