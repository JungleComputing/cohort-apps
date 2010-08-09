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
import ibis.cohort.context.OrContext;
import ibis.cohort.context.UnitContext;

public class Main {

    static class Job { 
        
        public final String problem;
        public final long size;
        public final ArrayList<UnitContext> context = new ArrayList<UnitContext>();
        
        public Job(String problem, long size, UnitContext context) { 
            this.problem = problem;
            this.size = size;
            this.context.add(context);
        }
        
        public void addContext(UnitContext c) {
            this.context.add(c); 
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

    private static HashMap<String, Job> jobs = new HashMap<String, Job>();
   
    private static void processList(ProblemList l) { 
        
        UnitContext c = new UnitContext(l.cluster);
        
        for (int i=0;i<l.problems.length;i++) {
            
            String problem = l.problems[i];
            long size = l.sizes[i];
            
            Job tmp = jobs.get(problem);
            
            if (tmp == null) { 
                tmp = new Job(problem, size, c);
                jobs.put(problem, tmp);
            } else { 
                tmp.addContext(c);
            }
        }
    }
    
    public static void main(String [] args) {

        try { 
            ArrayList<Result> res = new ArrayList<Result>();
            
            LocalConfig.configure(args);
            
            String [] clusters = LocalConfig.getClusters();
            
            long start = System.currentTimeMillis();
            
            Cohort cohort = CohortFactory.createCohort();
            cohort.activate();
        
            if (LocalConfig.isMaster()){ 
                // Wohoo! I'm in charge.

                // First send a 'list' job to all clusters
                MultiEventCollector c = new MultiEventCollector(
                        new UnitContext("master"), clusters.length);
                
                ActivityIdentifier id = cohort.submit(c);
                
                for (String cluster : clusters) { 
                    cohort.submit(new ListJob(id, cluster));
                }
              
                // Wait for the results and merge them into a single set 
                Event [] results = c.waitForEvents();

                for (Event e : results) { 
                    processList((ProblemList) ((MessageEvent) e).message);
                }
    
                FlexibleEventCollector f = new FlexibleEventCollector(
                        new UnitContext("master"));
   
                id = cohort.submit(f);
      
                int count = jobs.size();
                
                // Find the biggest job in the set
                long size = 0;
                
                for (Job job : jobs.values()) { 
                    if (job.size > size) { 
                        size = job.size;
                    }
                }
                
                // Submit a job for every image pair
                for (Job job : jobs.values()) { 
                    //cohort.submit(new CompareJob(id, job.getContext(), job.problem));
                    cohort.submit(new LaunchJob(id, job.getContext(), 
                            (int) (size-job.size), job.problem));
                }
                
                
                while (count > 0) { 
                    
                    long t = System.currentTimeMillis();
                    
                    System.out.println((t-start) + " Master waiting for " + count + " results");
                    
                    Event [] tmp = f.waitForEvents();
             
                    t = System.currentTimeMillis();
                    
                    System.out.println((t-start) + " Master received " + tmp.length + " results");
                    
                    for (Event e : tmp) { 
                        res.add((Result) ((MessageEvent) e).message);
                    }
                    
                    count -= tmp.length;
                }
     
                long t = System.currentTimeMillis();
                
                System.out.println((t-start) + " Master DONE");
 
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
