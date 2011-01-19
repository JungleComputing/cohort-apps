package datachallenge.wf;

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
            int contextType, int stealStrategy) { 

        long size = p.beforeFileSize + p.afterFileSize;

        switch (contextType) { 
        case LocalConfig.DEFAULT_CONTEXT:
        	if (stealStrategy == LocalConfig.ANY_ORDER) { 
        		return UnitActivityContext.DEFAULT;
        	} else { 
        		return new UnitActivityContext("DEFAULT", size);
        	}
       
        case LocalConfig.LOCATION_CONTEXT:
        	if (stealStrategy == LocalConfig.ANY_ORDER) { 
        		return new UnitActivityContext(cluster);
        	} else {
        		return new UnitActivityContext(cluster, size);
        	}
        	
       	case LocalConfig.SIZE_CONTEXT:
       		if (stealStrategy == LocalConfig.ANY_ORDER) { 
       			return new UnitActivityContext(getSizeTag(size));
       		} else { 
       			return new UnitActivityContext(getSizeTag(size), size);
       		}
       
       	case LocalConfig.HW_CONTEXT:
       		if (stealStrategy == LocalConfig.ANY_ORDER) { 
       			return new UnitActivityContext(getSizeTag(size));
       		} else { 
       			return new UnitActivityContext(getSizeTag(size), size);
       		}
       		
       	default:
            System.out.println("WARNING: Unknown context type " + contextType);
            return UnitActivityContext.DEFAULT;
        }
    }
    
    private static WorkerContext getWorkerContext(String cluster, String host, 
    		int contextType, String executorSize, boolean hasGPU, boolean fallback) { 

    	switch (contextType) { 
        case LocalConfig.DEFAULT_CONTEXT:
            // NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { 
        			new UnitWorkerContext("DEFAULT"), 
        			new UnitWorkerContext(cluster), 
        			new UnitWorkerContext(host) }, true); 
                    
        case LocalConfig.LOCATION_CONTEXT:
            	
        	if (!fallback) { 
        		return new UnitWorkerContext(cluster);
        	} else { 
        		return new OrWorkerContext(new UnitWorkerContext [] { 
        				new UnitWorkerContext(host), 
        				new UnitWorkerContext(cluster), 
        				new UnitWorkerContext("DEFAULT") }, true); 
        	}	
        	
        case LocalConfig.SIZE_CONTEXT:
      
        	// NOTE, we need the cluster to get the initial 'list' job to run.  
        	return new OrWorkerContext(new UnitWorkerContext [] { 
        			new UnitWorkerContext(executorSize), 
        			new UnitWorkerContext(cluster) }, true); 
        
        case LocalConfig.HW_CONTEXT:
       
        	if (hasGPU) { 
        		return new OrWorkerContext(new UnitWorkerContext [] { 
        				new UnitWorkerContext(host), 
        				new UnitWorkerContext("GPU"), 
        				new UnitWorkerContext("DEFAULT"), 
        				new UnitWorkerContext(cluster) }, true); 
        	} else { 
         		return new OrWorkerContext(new UnitWorkerContext [] { 
        				new UnitWorkerContext(host), 
        				new UnitWorkerContext("DEFAULT"), 
        				new UnitWorkerContext(cluster) }, true); 
        	}
        	  	
        default:
            System.out.println("WARNING: Unknown context type " + contextType);
            return UnitWorkerContext.DEFAULT;
        }
    }
    
    private static void addFallBack(Job job, int contextType, int stealStrategy) { 
        
    	if (contextType == LocalConfig.LOCATION_CONTEXT) { 
    		if (stealStrategy == LocalConfig.ANY_ORDER) { 
    			job.addContext(UnitActivityContext.DEFAULT);
    		} else { 
    			long size = job.beforeFileSize + job.afterFileSize;
    			job.addContext(new UnitActivityContext("DEFAULT", size));
    		}
    	}
    }
    
    private static void processList(ProblemList l, int contextType, int stealStrategy) {

        for (Problem p : l.problems) {

            UnitActivityContext c = getContext(p, l.cluster, contextType, stealStrategy); 

            Job tmp = jobs.get(p.name);

            if (tmp == null) { 
                tmp = new Job(p, l.server, c, 1000);
                jobs.put(p.name, tmp);
        
            	System.out.println("Adding job " + tmp.ID + " with context " + c);
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

            StealPool master = new StealPool("master");
            
            Executor [] e = null;
            
            int stealStrategy = LocalConfig.getStealStrategy();
            
            if (LocalConfig.isMaster()) {
            	// The master has 1 executor
            	e = new Executor[] { 
            			new SimpleExecutor(master, StealPool.NONE, new UnitWorkerContext("master")) };
            } else { 

                LocalConfig.startMonitor(1000);

            	// Workers have a variable number of executors            	
            	int exec = LocalConfig.getExecutorCount();
                
            	e = new Executor[exec];
                
                WorkerContext wc = getWorkerContext(
                        LocalConfig.cluster(), 
                        LocalConfig.host(), 
                        LocalConfig.getContextConfiguration(), 
                		LocalConfig.getExecutorType(),
                		LocalConfig.hasGPU(),
                		LocalConfig.allowFallback());
                	
                int executorPoolSize = LocalConfig.getExecutorPoolSize();
                
                /*
                StealStrategy local;
                
                if (LocalConfig.depthFirst()) { 
                	local = StealStrategy.BIGGEST;
                } else { 
                	local = StealStrategy.SMALLEST;
                }
                */
                
                for (int i=0;i<exec;i++) { 
                	
                	if (i % executorPoolSize == 0) {
                		System.out.println("Creating CPU executor that steals from master!");
                	
                		if (LocalConfig.getStealStrategy() == LocalConfig.SORTED) { 
                			e[i] = new SimpleExecutor(StealPool.NONE, master, wc, 
                					StealStrategy.BIGGEST);
                		} else { 
                			e[i] = new SimpleExecutor(StealPool.NONE, master, wc, 
                					StealStrategy.ANY);
                    	}
                	} else { 
                		// FIXME: this will fail if exec-1 % executorPoolSize == 0!!
                		if ((i == exec-1) && LocalConfig.hasGPU()) { 
                			System.out.println("Creating GPU executor that steals locally!");
                			
                			e[i] = new SimpleExecutor(
                    			StealPool.NONE, StealPool.NONE, 
                        		new UnitWorkerContext(LocalConfig.host() + "-GPU"));

                			/*
                		} else if ((i == exec-2) && LocalConfig.hasGPU()) { 
                   		
                			System.out.println("Creating GPU executor that steals locally!");
                    			
                			e[i] = new SimpleExecutor(
                				StealPool.NONE, StealPool.NONE, 
                            	new UnitWorkerContext(LocalConfig.host() + "-GPU"));
                			*/
                		} else { 
                
                			/*
                			if (i % 2 == 0) { 
                				System.out.println("Creating CPU executor that steals SMALL locally!");
                			
                				e[i] = new SimpleExecutor(
                						StealPool.NONE, StealPool.NONE, 
                						new UnitWorkerContext(LocalConfig.host()), 
                        				StealStrategy.SMALLEST);
                			} else { 
                			*/	System.out.println("Creating CPU executor that steals BIG locally!");
                    			
                				e[i] = new SimpleExecutor(
                						StealPool.NONE, StealPool.NONE, 
                						new UnitWorkerContext(LocalConfig.host()), 
                        				StealStrategy.BIGGEST);	
                			//}
                		}
                  }
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
                    processList((ProblemList) m.data, contextType, stealStrategy);
                }

                FlexibleEventCollector f = new FlexibleEventCollector(
                        new UnitActivityContext("master"));

                id = cn.submit(f);

                int count = jobs.size();

                // FIXME: no longer correct ?
                boolean fallback = LocalConfig.allowFallback();
                
                // Submit a job for every image pair
                for (Job job : jobs.values()) {
                    
                    if (fallback) { 
                        addFallBack(job, contextType, stealStrategy);
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
                        res.add((Job) m.data);
                    }

                    count -= tmp.length;
                }

                long t = System.currentTimeMillis();

                System.out.println((t-start) + " Master DONE");

            }

            cn.done();

            if (!LocalConfig.isMaster()) {
            	LocalConfig.stopMonitor();
            }
            
            for (Job r : res) { 
                System.out.println(r.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
