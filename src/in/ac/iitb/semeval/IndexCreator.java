package in.ac.iitb.semeval;

import org.apache.jena.larq.IndexBuilderString;
import org.apache.jena.larq.IndexLARQ;

import com.hp.hpl.jena.rdf.model.Model;

public class IndexCreator {
	
	Model model;
	IndexLARQ getIndex()
    {
        // ---- Read and index all literal strings.
        IndexBuilderString larqBuilder = new IndexBuilderString("/home/rakesh/Downloads/LarqIndex/");
        
        
        // build the index after the model has been created. 
        larqBuilder.indexStatements(model.listStatements()) ;
        
        // ---- Finish indexing
        larqBuilder.closeWriter() ;
        model.unregister(larqBuilder) ;
        
        // ---- Create the access index  
        IndexLARQ index = larqBuilder.getIndex() ;
        return index ; 
    }
		
	public IndexCreator(Model model) {

		
		this.model = model;
	}

}
