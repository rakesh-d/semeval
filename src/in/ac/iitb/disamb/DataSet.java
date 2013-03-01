package in.ac.iitb.disamb;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
class Clustering {
	String query;
	ArrayList<String> subTopics;
	int size;
	HashMap<String, ArrayList<String>> clusterMap;
	public Clustering (String query) {
		this.query = query;
		subTopics = new ArrayList<String>();
		clusterMap = new HashMap<String, ArrayList<String>>();
	}
	
}

public class DataSet {
	public String[] queryString;
	public String[][] resultURL;
	public String[][] resultTitle;
	public String[][] resultSnippet;
	public HashMap<String, String> subTopicMap;
	public HashMap<String, String> resultMap;
	public HashMap<String, String> standardAssign;
	public HashMap<String, Clustering> clusters;
	public HashMap<String, String>queryStringMap;
	
	public DataSet (String queryFileName, String resultFileName, String subTopicFileName, String assignFileName, int nQueries) throws IOException 
	{
		queryString = new String[nQueries];
		resultURL = new String[nQueries][100];
		resultTitle = new String[nQueries][100];
		resultSnippet = new String[nQueries][100];
		clusters = new HashMap<String, Clustering>();
		queryStringMap = new HashMap<String, String>();
	
		
		BufferedReader brQ = new BufferedReader(new FileReader(queryFileName));
		BufferedReader brR = new BufferedReader(new FileReader(resultFileName));
		BufferedReader brS = new BufferedReader(new FileReader(subTopicFileName));
		BufferedReader brA = new BufferedReader(new FileReader(assignFileName));

		
		brQ.readLine();
		brR.readLine();
		brS.readLine();
		brA.readLine();
		String[] t;
		resultMap = new HashMap<String, String>();
		
		for (int i = 0; i < nQueries; i++) 
		{
			String s = brQ.readLine();
			queryString[i] = s.split("\\t")[1];
			queryStringMap.put(s.split("\\t")[0], queryString[i]);
			for (int j = 0; j < 100; j++)
			{
				t = brR.readLine().split("\t");
				if (t.length < 3) continue;
				
				if (t.length < 4)
				{ 
						resultMap.put(t[0], t[2]);
						resultURL[i][j] = t[1];
						resultTitle[i][j] = t[2];
						continue;
				}
				resultURL[i][j] = t[1];
				resultTitle[i][j] = t[2];
				resultSnippet[i][j] = t[3];
				resultMap.put(t[0], t[2]+ " " + t[3]);

					
			}
	
						
		}
		//Reading subTopic File
		subTopicMap = new HashMap<String, String>();
		Scanner tsc = new Scanner(brS);
		while (tsc.hasNext()) {
			String line = tsc.nextLine();
			String subTopicID = line.split("\\t")[0];
			String subTopic = line.split("\\t")[1];
			subTopicMap.put(subTopicID, subTopic);
		}
		
		// Storing associations
		Scanner asc = new Scanner(brA);
		standardAssign = new HashMap<String, String>();
		while (asc.hasNext())
		{
			String subtopicInd = asc.next(), resultInd = asc.next();
			String query = queryStringMap.get(subtopicInd.split("\\.")[0]);
			String result = resultMap.get(resultInd), subtopic = subTopicMap.get(subtopicInd);
			standardAssign.put(result, subtopic);
			if (clusters.containsKey(query) == false) {
				Clustering temp = new Clustering(query);
				ArrayList<String> tempList = new ArrayList<String>();
				tempList.add(result);
				temp.clusterMap.put(subtopic, tempList);
				temp.subTopics.add(subtopic);
				clusters.put(query, temp);
			}
			else if (clusters.get(query).clusterMap.containsKey(subtopic)) {
				clusters.get(query).clusterMap.get(subtopic).add(result);
			}
			else
			{
				clusters.get(query).subTopics.add(subtopic);
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(result);
				
				clusters.get(query).clusterMap.put(subtopic, temp);
			}
				
							
		}
		
		for (Clustering cluster: clusters.values())
		{
			int count = 0;
			for (ArrayList<String> results: cluster.clusterMap.values())
				count += results.size();
			cluster.size = count;
		}
		
	}
	public void printDataSet()
	{
		/*for (String[] resultSet: resultTitle)
		{
			for (String title: resultSet)
				System.out.println(title);
			System.out.println("---------------------------");
					
		}
		*/
		for (String s: queryString)
		{
			System.out.println(s);
		}
		for (Map.Entry<String, String> ent: standardAssign.entrySet())
		{
			System.out.println(ent.getKey());
			System.out.println("**** Assigned concept - " + ent.getValue() + " ****");
			System.out.println();
		}
	}
	void printClusters (String query) {
		for (String subtopic: clusters.get(query).subTopics) {
			System.out.println("results under " + subtopic + ":");
			for (String result: clusters.get(query).clusterMap.get(subtopic)) 
				System.out.println("\t" + result);
		}
	}
	public static void main (String args[]) throws IOException {
		String queries = "/home/rakesh/Downloads/ambient/mytopics.txt";
		String subtopics = "/home/rakesh/Downloads/ambient/mysubTopics.txt";
		String results = "/home/rakesh/Downloads/ambient/myresults.txt";
		String assoc = "/home/rakesh/Downloads/ambient/mySTRel.txt";
		DataSet ds = new DataSet(queries,results,subtopics,assoc,10);
		ds.printClusters("Monte_Carlo");
	}
}
