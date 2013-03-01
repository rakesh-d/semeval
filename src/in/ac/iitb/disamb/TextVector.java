package in.ac.iitb.disamb;
import java.io.BufferedInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import weka.core.Stopwords;
import weka.core.stemmers.SnowballStemmer;
public class TextVector {
	String text;
	Map<String, Double> comps;
	double length;
	public TextVector () {
		comps = new HashMap<String, Double>();
	}
	
	public TextVector (String input) {
		
		text = input;
		String[] words = input.split("\\W+");
		comps = new HashMap<String, Double>();
		for (String word: words)
		{
			word = word.toLowerCase();
			if (Stopwords.isStopword(word))
				continue;
			SnowballStemmer stemmer = new SnowballStemmer();
			word = stemmer.stem(word);
			comps.put(word, comps.containsKey(word)?comps.get(word)+1.0:1.0);
			
		}
		length = 0;
		for (Double val:comps.values()) {
			length += val * val;
		}
		length = Math.sqrt(length);
		
	}
	int size () {
		return comps.size();
	}
	public static double similarity (TextVector t0, TextVector t1) {
		double sim = 0;
		if (t0.size() > t1.size()) {
			TextVector temp = t0;
			t0 = t1;
			t1 = temp;
		}
		for (Map.Entry<String, Double> entry: t0.comps.entrySet()) {
			if (t1.comps.containsKey(entry.getKey())) {
				sim += entry.getValue()*t1.comps.get(entry.getKey());
			}
		}
		
		
		return sim;
	}
	public String toString () {
		
		return comps.toString();
	}
	public static void main (String[] args) {
		BufferedInputStream bis = new BufferedInputStream(System.in);
		Scanner sc = new Scanner(bis);
		String test = sc.nextLine();
		TextVector tv = new TextVector(test);
		System.out.println(tv);
	}

}
