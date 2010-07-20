package datachallenge.simple;

import java.io.Serializable;

public class Result implements Serializable {

    private static final long serialVersionUID = 2306611001688349402L;
 
    public final String problem;
    public final String cluster;
    
    public final long copyTime;
    public final long processTime;
    
    public final byte [] output;
    public final Exception exception;
    
    public Result(String problem, String cluster, long copyTime, long processTime, byte [] output) {
        super();
        this.problem = problem;
        this.cluster = cluster;
        this.copyTime = copyTime;
        this.processTime = processTime;
        this.output = output;
        this.exception = null;
    }
    
    public Result(String problem, String cluster, Exception e) {
        super();
        this.problem = problem;
        this.cluster = cluster;
        this.processTime = 0;
        this.copyTime = 0;
        this.output = null;
        this.exception = e;
    }
    
    public String toString() { 
        
        if (output != null) { 
            return problem + " processed on " + cluster + " in " + copyTime 
                + " + " + processTime + " ms. : " + new String(output);
        } else { 
            return problem + " failed on " + cluster + " : " + exception;
        }
    }
}
