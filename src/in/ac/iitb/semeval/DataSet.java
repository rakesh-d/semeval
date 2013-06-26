package in.ac.iitb.semeval;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
class SearchResultSet {
	String query;
	String[] titles;
	String[] urls;
	String[] results;
	public SearchResultSet (String query, int nResults) {
		results = new String[nResults];
		urls = new String[nResults];
		titles = new String[nResults];
		this.query = query;
	}
	
}
public class DataSet {
	
	SearchResultSet[] data;
	
	public DataSet (String queryFile, String resultFile, boolean test) throws IOException {
		
		int nResults;
		if (test) {
			nResults = 64;
			data = new SearchResultSet[100];
		}
		else {
			nResults = 100;
			data = new SearchResultSet[114];
		}
		BufferedReader br = new BufferedReader(new FileReader(queryFile));
		Scanner sc = new Scanner(br);
		sc.nextLine();
		for (int i = 0; sc.hasNext(); i++) {
			sc.nextInt();
			data[i] = new SearchResultSet(sc.next(),nResults);
			
		}
		sc.close();
		br.close();
		br = new BufferedReader(new FileReader(resultFile));
		sc = new Scanner(br);
		sc.nextLine();
		for (int i = 0; sc.hasNext(); i++) {
			for (int j = 0; j < nResults; j++)
			{
				String line = sc.nextLine();
			//	System.out.println(line);
				String[] parts = line.split("\t");
				if (parts.length == 4) {
					data[i].results[j] = parts[3];
					data[i].titles[j] = parts[2];
					data[i].urls[j] = parts[1];
				}
				else {
					data[i].results[j] = parts[2];
					data[i].urls[j] = parts[1];
				}
			//	System.out.println((i+1) + "." + (j+1) + "   " + data[i].results[j]);
				
			}
		}
		sc.close();
		
		
	}
	void printData () {
		for (int i = 0; i < data.length; i++) {
			System.out.println((i+1) + " "+ data[i].query + " *** ");
			for (int j = 0; j < data[i].results.length; j++) {
				System.out.println(data[i].urls[j] + " " + data[i].titles[j]);
				System.out.println("     " + (i+1) + "." + (j+1) + "    " + data[i].results[j]);
			}
		}
	}
	public static void main (String[] args) throws IOException {
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
		DataSet ds = new DataSet(queryFile, resultFile, TEST);
		ds.printData();
		
	}

}
