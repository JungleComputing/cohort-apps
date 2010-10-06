package datachallenge.wf;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Cohort;
import ibis.cohort.CohortFactory;
import ibis.cohort.Event;
import ibis.cohort.Executor;
import ibis.cohort.FlexibleEventCollector;
import ibis.cohort.MessageEvent;
import ibis.cohort.MultiEventCollector;
import ibis.cohort.SimpleExecutor;
import ibis.cohort.StealPool;
import ibis.cohort.WorkerContext;
import ibis.cohort.context.OrWorkerContext;
import ibis.cohort.context.UnitActivityContext;
import ibis.cohort.context.UnitWorkerContext;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    private static HashMap<String, Job> jobs = new HashMap<String, Job>();

    private static UnitActivityContext getContext(Problem p, String cluster,
            int contextType) { 

        long size = p.beforeFileSize + p.afterFileSize;

        switch (contextType) { 
        case LocalConfig.DEFAULT_CONTEXT:
            return UnitActivityContext.DEFAULT;
        case LocalConfig.DEFAULT_CONTEXT_SORTED:
            return new UnitActivityContext("DEFAULT", size);
        case LocalConfig.LOCATION_CONTEXT:
            return new UnitActivityContext(cluster);
        case LocalConfig.LOCATION_CONTEXT_SORTED:
            return new UnitActivityContext(cluster, size);
        case LocalConfig.SIZE_CONTEXT:
            return new UnitActivityContext(getSizeTag(size));
        case LocalConfig.SIZE_CONTEXT_SORTED:
            return new UnitActivityContext(getSizeTag(size), size);
        default:
            System.out.println("WARNING: Unknown context type " + contextType);
        return UnitActivityContext.DEFAULT;
        }
    }
    
    private static WorkerContext getWorkerContext(String cluster, String host, 
    		int contextType, String executorSize, boolean fallback) { 

    	switch (contextType) { 
        case LocalConfig.DEFAULT_CONTEXT:
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext("DEFAULT"), new UnitWorkerContext(cluster), new UnitWorkerContext(host, UnitWorkerContext.SMALLEST) }, true); 
        
        case LocalConfig.DEFAULT_CONTEXT_SORTED:
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext("DEFAULT", UnitWorkerContext.BIGGEST), new UnitWorkerContext(cluster), new UnitWorkerContext(host, UnitWorkerContext.SMALLEST) }, true); 
                    
        case LocalConfig.LOCATION_CONTEXT:
        	
        	if (!fallback) { 
        		return new UnitWorkerContext(cluster);
        	} else { 
        		return new OrWorkerContext(new UnitWorkerContext [] 
        					{ new UnitWorkerContext(host), new UnitWorkerContext(cluster), new UnitWorkerContext("DEFAULT") },
        					true); 
        	}	
        case LocalConfig.LOCATION_CONTEXT_SORTED:
        
        	if (!fallback) { 
        		return new UnitWorkerContext(cluster, UnitWorkerContext.BIGGEST);
        	} else { 
        		return new OrWorkerContext(new UnitWorkerContext [] 
        					{ new UnitWorkerContext(cluster, UnitWorkerContext.BIGGEST), 
        				      new UnitWorkerContext("DEFAULT", UnitWorkerContext.BIGGEST) 
        					}, true); 
        	}
       
        case LocalConfig.SIZE_CONTEXT:
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext(executorSize), new UnitWorkerContext(cluster) }, true); 
        
        case LocalConfig.SIZE_CONTEXT_SORTED:
        
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext(executorSize, UnitWorkerContext.BIGGEST), new UnitWorkerContext(cluster) }, true); 
        
        default:
            System.out.println("WARNING: Unknown context type " + contextType);
            return UnitWorkerContext.DEFAULT;
        }
    }
    
    private static void addFallBack(Job job, int contextType) { 
        
        long size = job.beforeFileSize + job.afterFileSize;

        switch (contextType) { 
        case LocalConfig.LOCATION_CONTEXT:
            job.addContext(UnitActivityContext.DEFAULT);
            return;
        case LocalConfig.LOCATION_CONTEXT_SORTED:
            job.addContext(new UnitActivityContext("DEFAULT", size));
            return;
        default:
            return;
        }
    }

    
    private static void processList(ProblemList l, int contextType) {

        for (Problem p : l.problems) {

            UnitActivityContext c = getContext(p, l.cluster, contextType); 

            Job tmp = jobs.get(p.name);

            if (tmp == null) { 
                tmp = new Job(p, l.server, c, 1000);
                jobs.put(p.name, tmp);
            } else { 
                tmp.addContext(c);
                tmp.addServer(l.server);
            }
        }
    }

    private static String getSizeTag(long size) { 

        LocalConfig.Size [] sizes = LocalConfig.getSizes();

        for (LocalConfig.Size s : sizes) { 
            if (size >= s.from && size <= s.to) { 
                return s.name;
            }
        }

        return null;
    }

    public static void main(String [] args) {

        try { 
            ArrayList<Job> res = new ArrayList<Job>();

            LocalConfig.configure(args);

            String [] clusters = LocalConfig.getClusters();

            long start = System.currentTimeMillis();

            LocalConfig.startMonitor(1000);

            StealPool master = new StealPool("master");
            
            Executor [] e = null;
            
            if (LocalConfig.isMaster()) {
            	// The master has 1 executor
            	e = new Executor[] { new SimpleExecutor(master, StealPool.NONE, new UnitWorkerContext("master")) };
            } else { 
            	// Workers have a variable number of executors            	
            	int exec = LocalConfig.getExecutorCount();
                
            	e = new Executor[exec];
                
                WorkerContext wc = getWorkerContext(
                        LocalConfig.cluster(), 
                        LocalConfig.host(), 
                        LocalConfig.getContextConfiguration(), 
                		LocalConfig.getExecutorType(), 
                		LocalConfig.allowFallback());
                	
                int executorPoolSize = LocalConfig.getExecutorPoolSize();
                
                for (int i=0;i<exec;i++) { 
                	
                	if (i % executorPoolSize == 0) {
                		System.out.println("Creating executor that steals from master!");
                		e[i] = new SimpleExecutor(StealPool.NONE, master, wc);
                	} else { 
                		System.out.println("Creating executor that steals locally!");
                    	e[i] = new SimpleExecutor(StealPool.NONE, StealPool.NONE, 
                        		new UnitWorkerContext(LocalConfig.host(), UnitWorkerContext.SMALLEST));
                	}
            	}
            }
            
            Cohort cohort = CohortFactory.createCohort(e);
            cohort.activate();

            if (LocalConfig.isMaster()){ 
                // Wohoo! I'm in charge.

                int contextType = LocalConfig.getContextConfiguration();

                System.out.println("MASTER Using context config: " + contextType);

                // First send a 'list' job to all clusters
                MultiEventCollector c = new MultiEventCollector(
                        new UnitActivityContext("master"), clusters.length);

                ActivityIdentifier id = cohort.submit(c);

                for (String cluster : clusters) { 
                    cohort.submit(new ListJob(id, cluster));
                }

                // Wait for the results and merge them into a single set 
                Event [] results = c.waitForEvents();

                for (Event m : results) { 
                    processList((ProblemList) ((MessageEvent) m).message, 
                            contextType);
                }

                FlexibleEventCollector f = new FlexibleEventCollector(
                        new UnitActivityContext("master"));

                id = cohort.submit(f);

                int count = jobs.size();

                boolean fallback = LocalConfig.allowFallback();
                
                // Submit a job for every image pair
                for (Job job : jobs.values()) {
                    
                    if (fallback) { 
                        addFallBack(job, contextType);
                    }
                    
                    cohort.submit(new LaunchJob(id, job));
                }


                while (count > 0) { 

                    long t = System.currentTimeMillis();

                    System.out.println((t-start) + " Master waiting for " + count + " results");

                    Event [] tmp = f.waitForEvents();

                    t = System.currentTimeMillis();

                    System.out.println((t-start) + " Master received " + tmp.length + " results");

                    for (Event m : tmp) { 
                        res.add((Job) ((MessageEvent) m).message);
                    }

                    count -= tmp.length;
                }

                long t = System.currentTimeMillis();

                System.out.println((t-start) + " Master DONE");

            }

            cohort.done();

            LocalConfig.stopMonitor();
            
            for (Job r : res) { 
                System.out.println(r.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
