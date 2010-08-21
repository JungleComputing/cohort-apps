package datachallenge.simple;

import java.io.Serializable;
import java.util.ArrayList;

public class ProblemList implements Serializable {

    private static final long serialVersionUID = -2975371497978099183L;
    
    public final String cluster;
    public final String server;
    
    public final ArrayList<Problem> problems = new ArrayList<Problem>();
    public Exception exception;
   
    
    public ProblemList(final String cluster, final String server) {
        super();
        this.cluster = cluster;        
        this.server = server;
    }
    
    public void addProblem(Problem p) { 
    	problems.add(p);
    }
    
    public void addException(Exception e) { 
    	exception = e;
    }
}
