package datachallenge.simple;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Constellation;
import ibis.constellation.ConstellationFactory;
import ibis.constellation.Event;
import ibis.constellation.Executor;
import ibis.constellation.FlexibleEventCollector;
import ibis.constellation.MultiEventCollector;
import ibis.constellation.SimpleExecutor;
import ibis.constellation.StealPool;
import ibis.constellation.StealStrategy;
import ibis.constellation.WorkerContext;
import ibis.constellation.context.OrWorkerContext;
import ibis.constellation.context.UnitActivityContext;
import ibis.constellation.context.UnitWorkerContext;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    private static HashMap<String, Job> jobs = new HashMap<String, Job>();

    private static UnitActivityContext getContext(Problem p, String cluster,
            int contextType, boolean ordered) { 

        long size = p.beforeFileSize + p.afterFileSize;

        switch (contextType) { 
        case LocalConfig.DEFAULT_CONTEXT:
        	if (!ordered) { 
        		return UnitActivityContext.DEFAULT;
        	} else { 
        		return new UnitActivityContext("DEFAULT", size);
        	} 
        	
        case LocalConfig.LOCATION_CONTEXT:
        	if (!ordered) { 
        		return new UnitActivityContext(cluster);
        	} else {
        		return new UnitActivityContext(cluster, size);
        	}
        
        case LocalConfig.SIZE_CONTEXT:
    	case LocalConfig.SPECIALIZED_HW:
        	if (!ordered) {
        		return new UnitActivityContext(getSizeTag(size));
        	} else { 
        		return new UnitActivityContext(getSizeTag(size), size);
        	}
        	
        default:
            System.out.println("WARNING: Unknown context type " + contextType);
            return UnitActivityContext.DEFAULT;
        }
    }
       
    private static StealStrategy getStealStrategy(int stealOrder) { 
    	switch (stealOrder) { 
        case LocalConfig.BIGGEST_FIRST:
        	return StealStrategy.BIGGEST;
        
        case LocalConfig.ANY_ORDER:
            return StealStrategy.ANY;
              
        default:
            System.out.println("WARNING: Unknown steal strategy " + stealOrder);
            return StealStrategy.ANY;
        }
    }
    
    private static WorkerContext getWorkerContext(String cluster, 
    		int contextType, String executorSize, boolean fallback) { 

    	switch (contextType) { 
        case LocalConfig.DEFAULT_CONTEXT:
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext("DEFAULT"), new UnitWorkerContext(cluster) }, true); 
                    
        case LocalConfig.LOCATION_CONTEXT:
        	
        	if (!fallback) { 
        		return new UnitWorkerContext(cluster);
        	} else { 
        		return new OrWorkerContext(new UnitWorkerContext [] 
        					{ new UnitWorkerContext(cluster), new UnitWorkerContext("DEFAULT") },
        					true); 
        	}	
        
        case LocalConfig.SIZE_CONTEXT:
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext(executorSize), new UnitWorkerContext(cluster) }, true); 
        
        	/*
        case LocalConfig.SIZE_CONTEXT_SORTED:
        
        	// HACK!
        	if (executorSize.equals("XXLARGE")) { 
        		return new OrWorkerContext(new UnitWorkerContext [] { 
        				new UnitWorkerContext("XXLARGE", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext("LARGE", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext("MEDIUM", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext("SMALL", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext(cluster) }, true); 
            	
        	} else if (executorSize.equals("LARGE")) { 
        		return new OrWorkerContext(new UnitWorkerContext [] { 
        				new UnitWorkerContext("LARGE", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext("MEDIUM", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext("SMALL", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext(cluster) }, true); 
            	
        	} else if (executorSize.equals("MEDIUM")) { 

        		return new OrWorkerContext(new UnitWorkerContext [] {
        				new UnitWorkerContext("MEDIUM", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext("SMALL", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext(cluster) }, true); 

        	} else if (executorSize.equals("SMALL")) { 
        		return new OrWorkerContext(new UnitWorkerContext [] {
        				new UnitWorkerContext("SMALL", UnitWorkerContext.BIGGEST), 
        				new UnitWorkerContext(cluster) }, true); 

    		}

        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { new UnitWorkerContext(executorSize, UnitWorkerContext.BIGGEST), new UnitWorkerContext(cluster) }, true); 
           */
        
        case LocalConfig.SPECIALIZED_HW:
            	
        	if (executorSize.equals("GPU")) { 
           		return new OrWorkerContext(
           				new UnitWorkerContext [] { 
           				   new UnitWorkerContext(cluster), 
           				   new UnitWorkerContext("GPU"),
           				   new UnitWorkerContext("CPU") }, true); 
        	} else { 
          		return new OrWorkerContext(
           				new UnitWorkerContext [] { 
           				   new UnitWorkerContext(cluster), 
           				   new UnitWorkerContext("CPU") }, true); 
        	}
        	
        default:
            System.out.println("WARNING: Unknown context type " + contextType);
            return UnitWorkerContext.DEFAULT;
        }
    }
    
    private static void addFallBack(Job job, int contextType, int stealOrder) { 
        
        if (contextType == LocalConfig.LOCATION_CONTEXT) { 
       
        	if (stealOrder == LocalConfig.BIGGEST_FIRST) { 
        		long size = job.beforeFileSize + job.afterFileSize;
        		job.addContext(new UnitActivityContext("DEFAULT", size));
        	} else { 
        		job.addContext(UnitActivityContext.DEFAULT);
        	}
        }
    }
    
    private static void processList(ProblemList l, int contextType, boolean ordered) {

        for (Problem p : l.problems) {

            UnitActivityContext c = getContext(p, l.cluster, contextType, ordered); 

            Job tmp = jobs.get(p.name);

            if (tmp == null) { 
                tmp = new Job(p, l.server, c);
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
            ArrayList<Result> res = new ArrayList<Result>();

            LocalConfig.configure(args);

            String [] clusters = LocalConfig.getClusters();

            long start = System.currentTimeMillis();

            StealPool master = new StealPool("master");
            
            int exec = LocalConfig.getExecutorCount();
            
            Executor [] e = new Executor[exec];
            
            WorkerContext wc = getWorkerContext(
                    LocalConfig.cluster(), 
            		LocalConfig.getContextConfiguration(), 
            		LocalConfig.getExecutorType(), 
            		LocalConfig.allowFallback());
     
            StealStrategy st = getStealStrategy(LocalConfig.getStealOrder());
            
            boolean ordered = !st.equals(StealStrategy.ANY);
            
            if (LocalConfig.isMaster()) {
            	// The master sacrifes 1 executor
            	for (int i=0;i<exec-1;i++) { 
            		e[i] = new SimpleExecutor(StealPool.NONE, master, wc, st, st, st);
            	}
            	e[exec-1] = new SimpleExecutor(master, StealPool.NONE, 
            				new UnitWorkerContext("master"), st, st, st);
            } else { 

                LocalConfig.startMonitor(1000);

            	for (int i=0;i<exec;i++) { 
            		e[i] = new SimpleExecutor(StealPool.NONE, master, wc, st, st, st);
            	}
            }
            
            Constellation cn = ConstellationFactory.createConstellation(e);
            cn.activate();

            if (LocalConfig.isMaster()){ 
                // Wohoo! I'm in charge.

                int contextType = LocalConfig.getContextConfiguration();

                System.out.println("MASTER Using context config: " + contextType);

                // First send a 'list' job to all clusters
                MultiEventCollector c = new MultiEventCollector(
                        new UnitActivityContext("master"), clusters.length);

                ActivityIdentifier id = cn.submit(c);

                for (String cluster : clusters) { 
                    cn.submit(new ListJob(id, cluster));
                }

                // Wait for the results and merge them into a single set 
                Event [] results = c.waitForEvents();

                for (Event m : results) { 
                    processList((ProblemList) m.data, contextType, ordered);
                }

                FlexibleEventCollector f = new FlexibleEventCollector(
                        new UnitActivityContext("master"));

                id = cn.submit(f);

                int count = jobs.size();

                boolean fallback = LocalConfig.allowFallback();
                
                // Submit a job for every image pair
                for (Job job : jobs.values()) {
                    
                    if (fallback) { 
                        addFallBack(job, contextType, LocalConfig.getStealOrder());
                    }
                    
                    cn.submit(new LaunchJob(id, job));
                }


                while (count > 0) { 

                    long t = System.currentTimeMillis();

                    System.out.println((t-start) + " Master waiting for " + count + " results");

                    Event [] tmp = f.waitForEvents();

                    t = System.currentTimeMillis();

                    System.out.println((t-start) + " Master received " + tmp.length + " results");

                    for (Event m : tmp) { 
                        res.add((Result) m.data);
                    }

                    count -= tmp.length;
                }

                long t = System.currentTimeMillis();

                System.out.println((t-start) + " Master DONE");

            }

            cn.done();

            if (!LocalConfig.isMaster()){ 
            	LocalConfig.stopMonitor();
            }
            
            for (Result r : res) { 
                System.out.println(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
