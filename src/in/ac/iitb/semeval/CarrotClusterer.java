package in.ac.iitb.semeval;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;

public class CarrotClusterer {
	int queryNum;
	String[][] data;
	String queryTerm;
	HashMap<String, ArrayList<String>> semEvalOut;
	public CarrotClusterer (int qNum, DataSet ambDS) {
		
		queryNum = qNum;
		queryTerm = ambDS.data[qNum].query;
		int n = ambDS.data[qNum].results.length;
		semEvalOut = new HashMap<String, ArrayList<String>>();
		data = new String[n][3];
		
		for (int i = 0; i < n; i++) {
			data[i][0] = ambDS.data[qNum].urls[i];
			data[i][1] = ambDS.data[qNum].titles[i];
			data[i][2] = ambDS.data[qNum].results[i];
		}
	}
	void getClusters() {	
		
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
    
         attributes.put("BisectingKMeansClusteringAlgorithm.clusterCount", 8);
 //        attributes.put("LingoClusteringAlgorithm.clusterMergingThreshold", 0.55);



         
        ProcessingResult byTopicClusters = controller.process(attributes, BisectingKMeansClusteringAlgorithm.class);
//		final ProcessingResult byTopicClusters = controller.process(documents, queryTerm,
//		LingoClusteringAlgorithm.class);
		final List<Cluster> clustersByTopic = byTopicClusters.getClusters();
		
		for (Cluster cluster: clustersByTopic) {
			String key = Integer.toString(queryNum+1) + "." + Integer.toString(cluster.getId()+1);
			ArrayList<String> temp = new ArrayList<String>();
			
			for (Document doc: cluster.getAllDocuments()) {
				
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
	
	public static void main(String [] args) throws IOException
    {

		// [[[start:clustering-document-list]]]
		/* A few example documents, normally you would need at least 20 for reasonable clusters. */
		
		String queryFileM = "/home/rakesh/Downloads/MORESQUE/topics.txt";
		String resultFileM = "/home/rakesh/Downloads/MORESQUE/results.txt";
		String queryFileT = "/home/rakesh/Downloads/testdata/topics.txt";
		String resultFileT = "/home/rakesh/Downloads/testdata/results.txt";
		String outputFile = "/home/rakesh/Downloads/testdata/carrotSTRel.txt";
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
		for (int i = 0; i < ambDS.data.length; i++)
		{
			CarrotClusterer cc = new CarrotClusterer(i, ambDS);
			cc.getClusters();
			cc.printOutput(pw);
		}
		pw.close();
						

		
    }
	

}
