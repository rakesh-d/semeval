
package in.ac.iitb.disamb;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.query.*;
import java.util.*;


import java.io.*;
class OldEntity {
	String obj;
	String abstrct;
	public OldEntity (String obj, String abstrct)
	{
		this.obj = obj;
		this.abstrct = abstrct;
	}
}

public class OldEd {
	
	String queryTerm;
	List<Entity> entityList;
	ArrayList<String> snippets;
	HashMap<String, Entity> assignEnt;
	static final Entity defaultEnt = new Entity("default","Dummy");
	
	public OldEd(String term, String[] titles, String[] snippets) {
		
		
		queryTerm = term;
		this.snippets = new ArrayList<String>();
		for (int i = 0; i < snippets.length; i++) {
			if (snippets[i] != null) 
				this.snippets.add(snippets[i] + titles[i]);
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
			if (obj.toString().contains("disambiguation")) {
				query = "SELECT ?o WHERE { <" + obj.toString() + "> ?p ?o }";
				q = QueryFactory.create(query);
				qexec = QueryExecutionFactory.create(q, modelDisamb);
				ResultSet tempResults = qexec.execSelect();
				while (tempResults.hasNext()) {
					QuerySolution sol = tempResults.next();
					Resource obj1 = sol.getResource("?o");
					//System.out.println();
					//System.out.println("===== " + obj1.toString()+ " =====");
					query = "SELECT ?o WHERE {<" + obj1.toString() + "> ?p ?o}";
					q = QueryFactory.create(query);
					QueryExecution qabs0 = QueryExecutionFactory.create(q, modelAbs);
					ResultSet finalResult0 = qabs0.execSelect();
					if (finalResult0.hasNext()) 
					{
						String temp = finalResult0.next().getLiteral("?o").toString();
					//	System.out.println(temp);
						Entity ent = new Entity(obj1.toString(),temp);
						entityList.add(ent);
					}
					else
						//System.out.println("No Abstract");
					qabs0.close();
					
				}
				
			}
			else
			{
				//System.out.println();
				//System.out.println("===== " + obj.toString()+" =====");
				query = "SELECT ?o WHERE {<" + obj.toString() + "> ?p ?o}";
				q = QueryFactory.create(query);
				QueryExecution qabs = QueryExecutionFactory.create(q, modelAbs);
				ResultSet finalResult = qabs.execSelect();
				if (finalResult.hasNext())
				{
					String temp = finalResult.next().getLiteral("?o").toString();
					//System.out.println(temp);
					Entity ent = new Entity(obj.toString(),temp);
					entityList.add(ent);
					
				}
				else
					//System.out.println("No Abstract");
				qabs.close();
			}
		
				
		}
		qexec.close();
	}
	void classifySnippets () {
		
		// Converting entity descriptions into vectors
		
		assignEnt = new HashMap<String, Entity>();
		TextVector[] entVects = new TextVector[entityList.size()];
		for (int i = 0; i < entVects.length; i++) {
			entVects[i] = new TextVector(entityList.get(i).abstrct);
		}
		
		// Converting snippets into vectors
		for (String snippet: snippets) {
			TextVector snippetVector = new TextVector(snippet);
			double sim, maxSim = 0;
			int maxEnt = -1; // default cluster
			for (int i = 0; i < entVects.length; i++) {
				sim = TextVector.similarity(snippetVector, entVects[i]);
				if (sim > maxSim) {
					maxEnt = i;
					maxSim = sim;
				}
			}
			if (maxEnt != -1) 
				assignEnt.put(snippet, entityList.get(maxEnt));
			else
				assignEnt.put(snippet, defaultEnt);
		}
		

		
	}
	void printEntities () throws IOException {
		
		for (Entity ent: entityList)
		{
			System.out.println(ent.obj.substring(28));
			
			
		//	System.out.println(ent.abstrct);
			
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
		EntityDisambiguator entDis = new EntityDisambiguator(ambDS.queryString[5], ambDS.resultTitle[5],ambDS.resultSnippet[5]);
		entDis.printEntities();
		entDis.classifySnippets();
		entDis.printAssignments();
		
		

	}

}