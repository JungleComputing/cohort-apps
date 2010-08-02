package datachallenge.workflow;

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
import ibis.cohort.context.OrContext;
import ibis.cohort.context.UnitContext;

public class Main {

    private static final String DEFAULT_TMP = "/tmp";
     
    private static String dataDir; 
    private static String execDir; 
    private static String tmpDir = DEFAULT_TMP; 
    private static String cluster; 
    private static String host; 
    private static String [] clusters; 
    
    private static boolean isMaster = false;
    
    static class Job { 
        
        public final String problem;
        public final ArrayList<UnitContext> context = new ArrayList<UnitContext>();
        
        public Job(String problem, UnitContext context) { 
            this.problem = problem;
            this.context.add(context);
        }
        
        public void addContext(UnitContext c) {
            this.context.add(c); 
        }

        public Context getContext() {
            
            if (context.size() == 1) { 
                return context.get(0);
            }
            
            return new OrContext(context.toArray(
                    new UnitContext[context.size()]), null);
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
            } else if (tmp.equalsIgnoreCase("-cluster")) { 
                cluster = args[++i];
            } else if (tmp.equalsIgnoreCase("-host")) { 
                host = args[++i];
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
        
        if (host == null) { 
            throw new Exception("Host name not set!");
        }
        
        if (c.size() == 0) { 
            throw new Exception("Cluster list set!");
        }
   
        clusters = c.toArray(new String [c.size()]);
    }
    
    /*
    private static Context generateContext(String [] clusters) { 
        
        if (clusters.length == 0) { 
            return new UnitContext(clusters[0]);
        }
        
        ContextSet set = new ContextSet(new UnitContext(clusters[0]));
        
        for (int i=1;i<clusters.length;i++) { 
            set.add(new UnitContext(clusters[i]));
        }
        
        return set;
    }*/
    
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
            ArrayList<CompareResult> res = new ArrayList<CompareResult>();
            
            parseCommandLine(args);
            
            LocalConfig.configure(cluster, host, dataDir, execDir, tmpDir);
            
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
                    cohort.submit(new CompareJob(id, job.getContext(), 
                            job.problem, 8));
                }
                
                while (count > 0) { 
                    
                    System.out.println("Master waiting for " + count + " results");
                    
                    Event [] tmp = f.waitForEvents();
             
                    System.out.println("Master received " + tmp.length + " results");
                    
                    for (Event e : tmp) { 
                        res.add((CompareResult) ((MessageEvent) e).message);
                    }
                    
                    count -= tmp.length;
                }
                
            }
            
            cohort.done();
        
            for (CompareResult r : res) { 
                System.out.println(r);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
