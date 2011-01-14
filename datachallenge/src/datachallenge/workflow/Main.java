package datachallenge.workflow;

import java.util.ArrayList;
import java.util.HashMap;

import ibis.cohort.ActivityContext;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Cohort;
import ibis.cohort.CohortFactory;
import ibis.cohort.Event;
import ibis.cohort.FlexibleEventCollector;
import ibis.cohort.MessageEvent;
import ibis.cohort.MultiEventCollector;
import ibis.cohort.context.OrActivityContext;
import ibis.cohort.context.UnitActivityContext;

public class Main {

    private static final String DEFAULT_TMP = "/tmp";
     
    private static String dataDir; 
    private static String execDir; 
    private static String tmpDir = DEFAULT_TMP; 
    private static String cluster; 
    private static String [] clusters; 
    
    private static boolean isMaster = false;
    
    static class Job { 
        
        public final String problem;
        public final long size;
        public final ArrayList<UnitActivityContext> context = new ArrayList<UnitActivityContext>();
        
        public Job(String problem, long size, UnitActivityContext context) { 
            this.problem = problem;
            this.size = size;
            this.context.add(context);
        }
        
        public void addContext(UnitActivityContext c) {
            this.context.add(c); 
        }

        public ActivityContext getContext() {
            
            if (context.size() == 1) { 
                return context.get(0);
            }
            
            return new OrActivityContext(context.toArray(
                    new UnitActivityContext[context.size()]));
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
        
        for (int i=0;i<l.problems.length;i++) {
            
            String problem = l.problems[i];
            long size = l.sizes[i];
            
            Job tmp = jobs.get(problem);
            
            if (tmp == null) { 
                tmp = new Job(problem, size, new UnitActivityContext(l.cluster, size));
                jobs.put(problem, tmp);
            } else { 
                tmp.addContext(new UnitActivityContext(l.cluster, size));
            }
        }
    }
    
    public static void main(String [] args) {

        try { 
            ArrayList<CompareResult> res = new ArrayList<CompareResult>();
            
            parseCommandLine(args);
            
            LocalConfig.configure(cluster, dataDir, execDir, tmpDir);
            
            long start = System.currentTimeMillis();
            
            Cohort cohort = CohortFactory.createCohort();
            cohort.activate();
        
            if (isMaster){ 
                // Wohoo! I'm in charge.

                System.out.println("Master started");
                
                // First send a 'list' job to all clusters
                MultiEventCollector c = new MultiEventCollector(
                        new UnitActivityContext("master"), clusters.length);
                ActivityIdentifier id = cohort.submit(c);
                
                for (String cluster : clusters) { 
                
                    System.out.println("Master submit listjob " + cluster);
                         
                    cohort.submit(new ListJob(id, cluster));
                }
              
                // Wait for the results and merge them into a single set 
                Event [] results = c.waitForEvents();

                for (Event e : results) { 
                    processList((ProblemList) ((MessageEvent) e).message);
                }
    
                FlexibleEventCollector f = new FlexibleEventCollector(
                        new UnitActivityContext("master"));
                id = cohort.submit(f);
      
                int count = jobs.size();
                
//              Find the biggest job in the set
                long size = 0;
                
                for (Job job : jobs.values()) { 
                    if (job.size > size) { 
                        size = job.size;
                    }
                }
                
                // Submit a job for every image pair
                for (Job job : jobs.values()) { 
                    cohort.submit(new LaunchJob(id, job.getContext(), 
                    		job.problem, 8));
                }
                
                while (count > 0) { 
            
                    long t = System.currentTimeMillis();
                    
                    System.out.println((t-start) + " Master waiting for " + count + " results");
                    
                    Event [] tmp = f.waitForEvents();
             
                    System.out.println((t-start) + " Master received " + tmp.length + " results");
                    
                    for (Event e : tmp) { 
                        res.add((CompareResult) ((MessageEvent) e).message);
                    }
                    
                    count -= tmp.length;
                }
                
                long t = System.currentTimeMillis();
                
                System.out.println((t-start) + " Master DONE");
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
