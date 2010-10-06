package datachallenge.wf;

import ibis.cohort.ActivityContext;
import ibis.cohort.context.OrActivityContext;
import ibis.cohort.context.UnitActivityContext;

import java.io.Serializable;
import java.util.ArrayList;

public class Job implements Serializable {

    private static final long serialVersionUID = 1609633321956465930L;

    public String ID;

    public final int threshold;
    
    public final String beforeFileName;
    public final long beforeFileSize;

    public final String afterFileName;
    public final long afterFileSize;

    public final long size;

    public final ArrayList<UnitActivityContext> context = new ArrayList<UnitActivityContext>();
    public final ArrayList<String> servers = new ArrayList<String>();

    public String tmpdir; 

    public ArrayList<ScriptResult> results;
    public String cluster;
    public long processingTime;
    public boolean error;
    
    public int tiles = 0;
    
    public Job(Problem problem, String server, UnitActivityContext context, int threshold) { 

        this.ID = problem.name;

        this.threshold = threshold;
        
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

    public void addResult(ScriptResult r) { 
    	
    	if (results == null) { 
    		results = new ArrayList<ScriptResult>();
    	}
    	
    	results.add(r);
    }
    
    public void addContext(UnitActivityContext c) {

        for (int i=0;i<context.size();i++) { 
            UnitActivityContext tmp = context.get(i);

            if (tmp.name.equals(c.name) && tmp.rank == c.rank) { 
                return;
            }
        }

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

	public String toString() { 

		StringBuilder tmp = new StringBuilder();
		
		if (!error) { 
			tmp.append("JOB "); 
			tmp.append(ID);
			tmp.append(" processed on ");
			tmp.append(cluster);
			tmp.append(" in ");
			tmp.append(processingTime);
			tmp.append(" ms");
		} else { 
			tmp.append("JOB "); 
			tmp.append(ID);
			tmp.append(" FAILED on ");
			tmp.append(cluster);

			for (ScriptResult r : results) { 
				tmp.append(r.toString());
				tmp.append("\n");
			}
		}
		
		return tmp.toString();
	}
}