package in.ac.iitb.disamb;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class NormTextVectors {
	List<TextVector> normVectors;
	
	Map<String, Double> idf;
	Double avgLen;
	public NormTextVectors (List<TextVector> vecList) {
		
		
		avgLen = 0.0;
		for (TextVector tv: vecList) {
			avgLen += tv.length;
		}
		avgLen = avgLen / vecList.size();
		idf = new HashMap<String, Double>();
		getIDF(vecList);
		
		
		
		normVectors = new ArrayList<TextVector>();
		for (TextVector tv: vecList) {
			TextVector vec = new TextVector();
			vec.text = tv.text;
			for (Map.Entry<String, Double> entry: tv.comps.entrySet())
			{
				Double tf = entry.getValue();
				tf = 1 + Math.log(1+ Math.log(tf));
				Double pivNorm = (1 - 0.4) + (0.4)*(tv.length/avgLen);
				tf = tf*idf.get(entry.getKey())/pivNorm;
				vec.comps.put(entry.getKey(), tf);
			}
			normVectors.add(vec);
			
		}
		
		
	}
	void printVectors () {
		for (TextVector vec : normVectors) {
			System.out.println(vec.text);
			System.out.println(vec.comps);
		}
	}
	void getIDF(List<TextVector> vecList) {
		for (TextVector tv: vecList) {
			for (String word: tv.comps.keySet()) {
				if (idf.containsKey(word) == false) {
					double count = 0.0;
					for (TextVector i: vecList) 
						if (i.comps.containsKey(word))
							count += 1;
					idf.put(word, vecList.size()/count);
						
					
				}
					
			}
		}
	}
	
}
