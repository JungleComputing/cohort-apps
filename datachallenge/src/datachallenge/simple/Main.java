package datachallenge.simple;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Cohort;
import ibis.cohort.CohortFactory;
import ibis.cohort.Event;
import ibis.cohort.FlexibleEventCollector;
import ibis.cohort.MessageEvent;
import ibis.cohort.MultiEventCollector;
import ibis.cohort.context.UnitActivityContext;

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

            LocalConfig.startMonitor(1000);

            Cohort cohort = CohortFactory.createCohort();
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

                for (Event e : results) { 
                    processList((ProblemList) ((MessageEvent) e).message, 
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
