package in.ac.iitb.disamb;
import com.hp.hpl.jena.rdf.model.*;


import java.io.*;

public class DBPediaTester {
	public static void main (String[] args) throws FileNotFoundException {
		Model model = ModelFactory.createDefaultModel();
		FileInputStream fis = new FileInputStream("/home/rakesh/Downloads/DBPedia/disambiguations_en.nt");
		model.read(fis, null, "N-TRIPLE");
		
		StmtIterator iter = model.listStatements(
			    new SimpleSelector(null, null,(RDFNode) null) {
			        public boolean selects(Statement s)
			            {return s.getObject().asResource().getLocalName().contains("Beagle");}
			            });
		while (iter.hasNext())
			System.out.println(iter.next().toString());
		
		
		
	}

}
