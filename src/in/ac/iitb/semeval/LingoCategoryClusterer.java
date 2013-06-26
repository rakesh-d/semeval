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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.auth.AuthScope;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
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


public class LingoCategoryClusterer {
	String queryTerm;
	List<Concept> conList;
	ArrayList<String> snippets;
	HashMap<String, Concept> assignCon;
	HashMap<String, ArrayList<String>> categs;
	ArrayList<ArrayList<String>> entityVecs;
	String[][] data;
	HashMap<String, Double> assignAccuracy;
	double threshold;
	String[] catData;
	int queryNum;
	HashMap<String, ArrayList<String>> semEvalOut;
	static final Concept defaultCon = new Concept(); 
	HashMap<String, ArrayList<String>> clusters;
	String directory = "/home/rakesh/Downloads/yago_jena" ;
	Double precision, recall, F1;
	
	public final String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
		      + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX yago: <http://yago-knowledge.org/resource>";
	@SuppressWarnings("deprecation")
	public LingoCategoryClusterer (int qNum, DataSet ambDS) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException, NoSuchAlgorithmException, KeyManagementException {
		
		
		queryTerm = ambDS.data[qNum].query;
		queryNum = qNum;
		queryTerm = ambDS.data[qNum].query;
		int n = ambDS.data[qNum].results.length;
		semEvalOut = new HashMap<String, ArrayList<String>>();
		data = new String[n][3];
		
		
		ArrayList<ArrayList<String>> entityLists;
		String path = "/home/rakesh/Downloads/new_annot/".concat(queryTerm);
		Path file = Paths.get(path);

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
			String[] snips = ambDS.data[qNum].results;
			for (int i = 0; i < snips.length; i++)
			{
						
				val2 = snips[i].replace(' ','+');
				params.add(par2, val2);
				String out = wRes.queryParams(params).get(String.class);
				ObjectMapper mapper = new ObjectMapper();
				TagmeJSON data = mapper.readValue(out, TagmeJSON.class);
				Annotation[] ann = data.getAnnotations();
				params.remove(par2);
				ArrayList<String> titles = new ArrayList<String>();
				for (int j = 0; j < ann.length; j++)
				{
					if (Double.parseDouble(ann[j].getRho()) > 0.1)
					{
						titles.add(ann[j].getTitle());
					}
				}
				entityLists.add(titles);		
				
			}
			System.out.println(entityLists);	
			FileOutputStream fos = new FileOutputStream("/home/rakesh/Downloads/new_annot/".concat(queryTerm));
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(entityLists);
			os.close();
		}
		else
		{
			FileInputStream fis = new FileInputStream(path);
			ObjectInputStream ois = new ObjectInputStream(fis);
			entityLists = (ArrayList<ArrayList<String>>)ois.readObject();
			ois.close();
		}
		
		String directory = "/home/rakesh/Downloads/Titles_TDB" ;
		  @SuppressWarnings("deprecation")
		Model modelTitles = TDBFactory.createModel(directory);
		  
		entityVecs = new ArrayList<ArrayList<String>>();
		HashSet<String> entSet = new HashSet<String>();
		
		String prefix = "http://dbpedia.org/resource/";
		String suffix = "@en";
		Query q;
		QueryExecution qexec = null;
		for (ArrayList<String> ents: entityLists)
		{
			ArrayList<String> vec = new ArrayList<String>();
			for (String label: ents) {
			//	System.out.println(label);
				String query = "SELECT ?s WHERE {?s ?p \""+ label + "\"" + suffix + "}" ;
				q = QueryFactory.create(query);
				qexec = QueryExecutionFactory.create(q, modelTitles);
				ResultSet iteresults = qexec.execSelect();
				while (iteresults.hasNext())
				{
					String entName = iteresults.next().get("?s").toString();
					vec.add(entName);
					entSet.add(entName);
					
				}
		
			}
			int i = 0;
			for (String supcat: vec)
				System.out.print(i++ +". " + supcat.substring(28) + "  *  ");
			System.out.println();
			entityVecs.add(vec);
		}
		qexec.close();
		System.out.println("entSet size: " + entSet.size());
		directory = "/home/rakesh/Downloads/ArtCategs_TDB";
		Model modelCateg = TDBFactory.createModel(directory);
		HashSet<String> categSet = new HashSet<String>();
		categs = new HashMap<String, ArrayList<String>>();
		for (String ent: entSet)
		{
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
		
		/*for (String cat : categSet) 
		{
			System.out.print(cat.substring(37) + ": " );
			for (String supcat: closures.get(cat))
			{
				System.out.print(supcat + " * ");
			}
			System.out.println();
		}
		*/
		
		/* Prepare catData from entityVecs */
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


	}
	String getWords (String cat) {
		String[] words = cat.split("_");
		String data = "";
		for (int i = 0; i < words.length; i++) {
			data = data.concat(words[i] + " ");
		}
		return data;
	}
	
	void getClusters () {
		
		/* Prepare Carrot2 documents */
		final ArrayList<Document> documents = new ArrayList<Document>();
		for (String [] row : data)
		{
			documents.add(new Document(row[1], row[2], row[0]));
		}

		/* A controller to manage the processing pipeline. */
		final Controller controller = ControllerFactory.createSimple();


		/*
		 * Perform clustering by topic using the Lingo algorithm. Lingo can 
		 * take advantage of the original query, so we provide it along with the documents.
		
		 */
		 Map<String, Object> attributes = new HashMap<String, Object>();

		 
         attributes.put("documents", documents);
     //    attributes.put("LingoClusteringAlgorithm.labelAssigner", SimpleLabelAssigner.class);

   //      attributes.put("LingoClusteringAlgorithm.factorizationFactory", NonnegativeMatrixFactorizationEDFactory.class);
    
         attributes.put("LingoClusteringAlgorithm.desiredClusterCountBase", 9);
 //        attributes.put("LingoClusteringAlgorithm.clusterMergingThreshold", 0.55);



         
        ProcessingResult byTopicClusters = controller.process(attributes, LingoClusteringAlgorithm.class);
//		final ProcessingResult byTopicClusters = controller.process(documents, queryTerm,
//		LingoClusteringAlgorithm.class);
		final List<Cluster> clustersByTopic = byTopicClusters.getClusters();
		
		HashMap<Document, Cluster> assignClust = new HashMap<Document, Cluster>();
		HashMap<Cluster, ArrayList<Document>> finalClust = new HashMap<Cluster, ArrayList<Document>>();
		for (Cluster cluster: clustersByTopic) {
				
			for (Document doc: cluster.getAllDocuments()) {
				
				if (assignClust.containsKey(doc)) {
					if (assignClust.get(doc).size() < cluster.size())
					{
						assignClust.put(doc, cluster);
					}
				
				}
				else
					assignClust.put(doc, cluster);
				
			}

		}
		for (Map.Entry<Document, Cluster> entry: assignClust.entrySet())
		{
			if (finalClust.containsKey(entry.getValue())) {
				finalClust.get(entry.getValue()).add(entry.getKey());
			}
			else
			{
				ArrayList<Document> temp = new ArrayList<Document>();
				temp.add(entry.getKey());
				finalClust.put(entry.getValue(), temp);
			}
		}
		
		
		
		for (Map.Entry<Cluster, ArrayList<Document>> entry: finalClust.entrySet()) {
			Cluster cluster = entry.getKey();
			String key = Integer.toString(queryNum+1) + "." + Integer.toString(cluster.getId()+1);
			ArrayList<String> temp = new ArrayList<String>();
			ArrayList<Document> docs = entry.getValue();
			for (Document doc: docs) {
				
				String val = Integer.toString(queryNum +1) + "." + Integer.toString(doc.getId()+1);
		//		System.out.println("key: " + key + " val: " + val);
				temp.add(val);
			}
			semEvalOut.put(key, temp);
		}
		
		
				
		ConsoleFormatter.displayClusters(clustersByTopic);
	
		
		
		
		
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
		String outputFile = "/home/rakesh/Downloads/testdata/lingo_categSTRel.txt";
		PrintWriter pw = new PrintWriter(outputFile);
		pw.println("subTopicID\tresultID");
		DataSet ambDS = new DataSet(queryFile,resultFile, true);
				
		
	/*	System.out.println("Input the query term: ");
		BufferedInputStream bis = new BufferedInputStream(System.in);
		Scanner sc = new Scanner(bis);
		String term = sc.next(); */
		for (int i = 0; i < 100; i++) {
			
		
			LingoCategoryClusterer catClass = new LingoCategoryClusterer(i, ambDS);
			catClass.getClusters();
			catClass.printOutput(pw);
			
		}	
		pw.close();
	}

}
