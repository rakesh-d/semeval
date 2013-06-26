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
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.ApacheHttpClientState;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class TagClassifier {
	
	String queryTerm;
	ArrayList<String> snippets;
	HashMap<Integer, String> assignEnt;
	HashMap<String, Double> assignAccuracy;
	double threshold;
	HashMap<String, ArrayList<String>> semEvalOut;
	static final Concept defaultCon = new Concept(); 
	HashMap<String, ArrayList<String>> clusters;
	ArrayList<ArrayList<String>> entityLists;
	HashSet<String> entSet;
	ArrayList<String> candEnts;
	int queryNum;
	
public TagClassifier (int qNum, String term, String[] snippets) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException, NoSuchAlgorithmException, KeyManagementException {
	
	
	
		queryNum = qNum;
		queryTerm = term;
		
		this.snippets = new ArrayList<String>();
		for (int i = 0; i < snippets.length; i++) {
			if (snippets[i] != null) 
				this.snippets.add(snippets[i]);
		}
		clusters = new HashMap<String, ArrayList<String>>();
		assignEnt = new HashMap<Integer, String>();
		semEvalOut = new HashMap<String, ArrayList<String>>();
		String path = "/home/rakesh/Downloads/new_annot/".concat(queryTerm);
		Path file = Paths.get(path);
//		// Create a trust manager that does not validate certificate chains
//		  TrustManager[] trustAllCerts = new TrustManager[] { 
//		    new X509TrustManager() {
//		      public X509Certificate[] getAcceptedIssuers() { 
//		        return new X509Certificate[0]; 
//		      }
//		      public void checkClientTrusted(X509Certificate[] certs, String authType) {}
//		      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
//		  }};
//
//		  // Ignore differences between given hostname and certificate hostname
//		  HostnameVerifier hv = new HostnameVerifier() {
//		    public boolean verify(String hostname, SSLSession session) { return true; }
//		  };
//
//		  // Install the all-trusting trust manager
//		  
//		    SSLContext sc = SSLContext.getInstance("SSL");
//		    sc.init(null, trustAllCerts, new SecureRandom());
//		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//		    HttpsURLConnection.setDefaultHostnameVerifier(hv);
//		  
//		 
		  
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
			for (int i = 0; i < snippets.length; i++)
			{
						
				val2 = snippets[i].replace(' ','+');
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
		
		// Extract distinct entities represented by the query term
		
		entSet = new HashSet<String>();
		for (ArrayList<String> entSnip : entityLists) {
			for (String ent : entSnip) {
				String[] entSplit = ent.split(" ");
				String[] termSplit = term.split("_");
				if (termSplit.length <= entSplit.length)
				{
					boolean flag = true;
					for (int j = 0; j < termSplit.length; j++)
					{
						if (!termSplit[j].equals(entSplit[j].toLowerCase()))
							flag = false;
					}
					if (flag) {
						entSet.add(ent);
					}
				}
			}
		}
		candEnts = new ArrayList<String>(entSet);
		
		for (String ent: entSet) {
			System.out.println(ent);
		}
		
		System.out.println();
		System.out.println("========***************==============");
		System.out.println();
		
		
	
	}
	void getClustering () {
		
		int maxEnt, index;
		for (int i = 0; i <entityLists.size(); i++) {
			
			ArrayList<String> entList = entityLists.get(i);
			HashSet<String> snipEnts = new HashSet<String>(entList);
			snipEnts.retainAll(entSet);
	//		if (snipEnts.size() > 1)
	//		System.out.println("########################################################");
			for (String ent: snipEnts) 
			{
				assignEnt.put(i, ent);
				
			}
			
		}
		
		for (Map.Entry<Integer, String> entry : assignEnt.entrySet())
		{
			String ent = entry.getValue();
			maxEnt = candEnts.indexOf(ent);
			index = entry.getKey();
			String key = Integer.toString(queryNum+1) + "." + Integer.toString(maxEnt+1);
			String val = Integer.toString(queryNum+1) + "." + Integer.toString(index+1);
			
			if (clusters.get(ent) == null)
			{
				ArrayList<String> snipList = new ArrayList<String>();
				ArrayList<String> numList = new ArrayList<String>();
				numList.add(val);
				snipList.add(snippets.get(entry.getKey()));
				clusters.put(ent, snipList);
				semEvalOut.put(key, numList);
			}
			else {
				clusters.get(ent).add(snippets.get(entry.getKey()));
				semEvalOut.get(key).add(val);
			}
		}
	
	}
	void printClustering () {
		for (Map.Entry<String, ArrayList<String>> entry: clusters.entrySet())
		{
			System.out.println();
			System.out.println("== results under " + entry.getKey() + ":" );
			for (String snip: entry.getValue())
				System.out.println("\t" + snip);
		}
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
	public static void main (String[] args) throws IOException, KeyManagementException, ClassNotFoundException, NoSuchAlgorithmException {

		String queryFileM = "/home/rakesh/Downloads/MORESQUE/topics.txt";
		String resultFileM = "/home/rakesh/Downloads/MORESQUE/results.txt";
		String queryFileT = "/home/rakesh/Downloads/testdata/topics.txt";
		String resultFileT = "/home/rakesh/Downloads/testdata/results.txt";
		String outputFile = "/home/rakesh/Downloads/testdata/tagSTRel.txt";
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
		for (int i = 0; i < 100; i++) {
			
			System.out.println();
			System.out.println("-------No. " + (i+1) + " -------");
			TagClassifier tagClass = new TagClassifier(i,ambDS.data[i].query, ambDS.data[i].results);
			tagClass.getClustering();
			tagClass.printClustering();
			tagClass.printOutput(pw);
		}	
		pw.close();
	}
}
