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

	private static void processList(ProblemList l, int contextType) { 

		if (contextType == LocalConfig.NO_CONTEXT) {
			UnitActivityContext c = new UnitActivityContext("none");

			for (Problem p : l.problems) {

				Job tmp = jobs.get(p.name);

				if (tmp == null) { 
					tmp = new Job(p, l.server, c);
					jobs.put(p.name, tmp);
				} else { 
					tmp.addServer(l.server);
				}
			}

		} else if (contextType == LocalConfig.LOCATION_CONTEXT) {
			for (Problem p : l.problems) {

				Job tmp = jobs.get(p.name);

				if (tmp == null) { 
					tmp = new Job(p, l.server, new UnitActivityContext(l.cluster, tmp.size));
					jobs.put(p.name, tmp);
				} else { 
					tmp.addContext(new UnitActivityContext(l.cluster, tmp.size));
					tmp.addServer(l.server);
				}
			}
			
		} else if (contextType == LocalConfig.SIZE_CONTEXT) {

			for (Problem p : l.problems) {

				Job tmp = jobs.get(p.name);

				if (tmp == null) {
					
					UnitActivityContext c = getSizeContext(tmp.size);
					
					if (c == null) { 
						System.err.println("Job " + tmp.ID + " has unknown size! " + tmp.size + " SKIPPING!");
					} else { 
						tmp = new Job(p, l.server, c);
						jobs.put(p.name, tmp);
					}
				} else { 
					tmp.addServer(l.server);
				}
			}
		}
	}

	private static UnitActivityContext getSizeContext(long size) { 
		
		LocalConfig.Size [] sizes = LocalConfig.getSizes();

		for (LocalConfig.Size s : sizes) { 
			if (size >= s.from && size <= s.to) { 
				return new UnitActivityContext(s.name, size);
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

			Cohort cohort = CohortFactory.createCohort();
			cohort.activate();

			if (LocalConfig.isMaster()){ 
				// Wohoo! I'm in charge.

				boolean sorted = LocalConfig.getSortedConfiguration();
				int contextType = LocalConfig.getContextConfiguration();

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
					processList((ProblemList) ((MessageEvent) e).message, contextType);
				}

				FlexibleEventCollector f = new FlexibleEventCollector(
						new UnitActivityContext("master"));

				id = cohort.submit(f);

				int count = jobs.size();

				// Submit a job for every image pair
				for (Job job : jobs.values()) {
					cohort.submit(new CompareJob(id, job));
					//cohort.submit(new LaunchJob(id, job.getContext(), 
					//        (int) (size-job.size), job));
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
