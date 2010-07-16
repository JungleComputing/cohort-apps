package datachallenge.simple;

import java.io.Serializable;

public class ProblemList implements Serializable {

    private static final long serialVersionUID = -2975371497978099183L;
    
    public final String cluster; 
    public final String [] problems;
    
    public ProblemList(final String cluster, final String[] problems) {
        super();
        this.cluster = cluster;
        this.problems = problems;
    }
}
