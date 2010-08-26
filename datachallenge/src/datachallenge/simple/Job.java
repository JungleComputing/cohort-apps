package datachallenge.simple;

import ibis.cohort.ActivityContext;
import ibis.cohort.context.OrActivityContext;
import ibis.cohort.context.UnitActivityContext;

import java.io.Serializable;
import java.util.ArrayList;

public class Job implements Serializable {
    
    private static final long serialVersionUID = 1609633321956465930L;

	public String ID;
    
    public final String beforeFileName;
	public final long beforeFileSize;
	
	public final String afterFileName;
	public final long afterFileSize;
	
    public final long size;
    
    public final ArrayList<UnitActivityContext> context = new ArrayList<UnitActivityContext>();
    public final ArrayList<String> servers = new ArrayList<String>();
    
    public Job(Problem problem, String server, UnitActivityContext context) { 
        
    	this.ID = problem.name;
        
        this.beforeFileName = problem.beforeFileName;
        this.beforeFileSize = problem.beforeFileSize;
        
        this.afterFileName = problem.afterFileName;
        this.afterFileSize = problem.afterFileSize;
        
        this.size = problem.beforeFileSize + problem.afterFileSize;
    	
        this.context.add(context);
        
        if (server != null) { 
        	servers.add(server);
        }
    }
    
    public void addContext(UnitActivityContext c) {
        this.context.add(c); 
    }

    public void addServer(String server) {
    	
    	// Bit expensive ... ?
    	if (!servers.contains(server)) { 
    		servers.add(server);
    	}
    }
    
    public ActivityContext getContext() {
        
        if (context.size() == 1) { 
            return context.get(0);
        }
        
        return new OrActivityContext(context.toArray(new UnitActivityContext[context.size()]));
    }
}