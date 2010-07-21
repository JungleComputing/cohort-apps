package datachallenge.simple;

import java.util.ArrayList;
import java.util.HashMap;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Cohort;
import ibis.cohort.CohortFactory;
import ibis.cohort.Context;
import ibis.cohort.Event;
import ibis.cohort.FlexibleEventCollector;
import ibis.cohort.MessageEvent;
import ibis.cohort.MultiEventCollector;
import ibis.cohort.context.ContextSet;
import ibis.cohort.context.UnitContext;

public class Main {

    private static final String DEFAULT_EXEC = "dach.sh";
    private static final String DEFAULT_TMP = "/tmp";
     
    private static String dataDir; 
    private static String execDir; 
    private static String tmpDir = DEFAULT_TMP; 
    private static String exec = DEFAULT_EXEC; 
    private static String cluster; 
    private static String [] clusters; 
    
    private static boolean isMaster = false;
    
    static class Job { 
        
        public final String problem;
        public Context context;
        
        public Job(String problem, Context context) { 
            this.problem = problem;
            this.context = context;
        }
        
        public void addContext(UnitContext c) {
            
            if (context instanceof UnitContext) { 
                context = new ContextSet((UnitContext) context);
            }
            
            ((ContextSet)context).add(c);
        }
    }

    private static HashMap<String, Job> jobs = new HashMap<String, Job>();
    
    private static void parseCommandLine(String [] args) throws Exception { 
    
        ArrayList<String> c = new ArrayList<String>();
        
        for (int i=0;i<args.length;i++) { 
            
            String tmp = args[i];
            
            if (tmp.equalsIgnoreCase("-dataDir")) { 
                dataDir = args[++i];
            } else if (tmp.equalsIgnoreCase("-execDir")) { 
                execDir = args[++i];
            } else if (tmp.equalsIgnoreCase("-tmpDir")) { 
                tmpDir = args[++i];
            } else if (tmp.equalsIgnoreCase("-exec")) { 
                exec = args[++i];
            } else if (tmp.equalsIgnoreCase("-cluster")) { 
                cluster = args[++i];
            } else if (tmp.equalsIgnoreCase("-master")) { 
                isMaster = true;
            } else if (tmp.equalsIgnoreCase("-clusters")) { 
             
                while (i+1 < args.length && !args[i+1].startsWith("-")) { 
                    c.add(args[++i]);
                }
            }
        }
    
        if (dataDir == null) { 
            throw new Exception("Data directory not set!");
        }
   
        if (execDir == null) { 
            throw new Exception("Exec directory not set!");
        }
        
        if (tmpDir == null) { 
            throw new Exception("Exec directory not set!");
        }
        
        if (cluster == null) { 
            throw new Exception("Cluster name not set!");
        }
        
        if (c.size() == 0) { 
            throw new Exception("Cluster list set!");
        }
   
        clusters = c.toArray(new String [c.size()]);
    }
    
    private static Context generateContext(String [] clusters) { 
        
        if (clusters.length == 0) { 
            return new UnitContext(clusters[0]);
        }
        
        ContextSet set = new ContextSet(new UnitContext(clusters[0]));
        
        for (int i=1;i<clusters.length;i++) { 
            set.add(new UnitContext(clusters[i]));
        }
        
        return set;
    }
    
    private static void processList(ProblemList l) { 
        
        UnitContext c = new UnitContext(l.cluster);
        
        for (String problem : l.problems) { 
            
            Job tmp = jobs.get(problem);
            
            if (tmp == null) { 
                tmp = new Job(problem, c);
                jobs.put(problem, tmp);
            } else { 
                tmp.addContext(c);
            }
        }
    }
    
    public static void main(String [] args) {

        try { 
            ArrayList<Result> res = new ArrayList<Result>();
            
            parseCommandLine(args);
            
            LocalConfig.configure(cluster, dataDir, execDir, tmpDir, exec);
            
            Cohort cohort = CohortFactory.createCohort();
            cohort.activate();
        
            if (isMaster){ 
                // Wohoo! I'm in charge.

                // First send a 'list' job to all clusters
                MultiEventCollector c = new MultiEventCollector(clusters.length);
                ActivityIdentifier id = cohort.submit(c);
                
                for (String cluster : clusters) { 
                    cohort.submit(new ListJob(id, cluster));
                }
              
                // Wait for the results and merge them into a single set 
                Event [] results = c.waitForEvents();

                for (Event e : results) { 
                    processList((ProblemList) ((MessageEvent) e).message);
                }
    
                FlexibleEventCollector f = new FlexibleEventCollector();
                id = cohort.submit(f);
      
                int count = jobs.size();
                
                // Submit a job for every image pair
                for (Job job : jobs.values()) { 
                    cohort.submit(new CompareJob(id, job.context, job.problem));
                }
                
                
                while (count > 0) { 
                    
                    System.out.println("Master waiting for " + count + " results");
                    
                    Event [] tmp = f.waitForEvents();
             
                    System.out.println("Master received " + tmp.length + " results");
                    
                    for (Event e : tmp) { 
                        res.add((Result) ((MessageEvent) e).message);
                    }
                    
                    count -= tmp.length;
                }
                
            }
            
            cohort.done();
        
            for (Result r : res) { 
                System.out.println(r);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
