package datachallenge.simple;

import ibis.cohort.Context;
import ibis.cohort.context.OrContext;
import ibis.cohort.context.UnitContext;

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
    
    public final ArrayList<UnitContext> context = new ArrayList<UnitContext>();
    public final ArrayList<String> servers = new ArrayList<String>();
    
    public Job(Problem problem, String server, UnitContext context) { 
        
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
    
    public void addContext(UnitContext c) {
        this.context.add(c); 
    }

    public void addServer(String server) {
    	
    	// Bit expensive ... ?
    	if (!servers.contains(server)) { 
    		servers.add(server);
    	}
    }
    
    public Context getContext() {
        
        if (context.size() == 1) { 
            return context.get(0);
        }
        
        return new OrContext(
                context.toArray(new UnitContext[context.size()]), 
                null);
    }
}