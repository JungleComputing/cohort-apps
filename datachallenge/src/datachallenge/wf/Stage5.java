package datachallenge.wf;

import ibis.constellation.Activity;
import ibis.constellation.ActivityContext;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class Stage5 extends Activity {

	private static final long serialVersionUID = 2353416132427552293L;

	private Job job;
	private ActivityIdentifier parent;
	
//	private String tmpdir;

	private int results;
	
	private long start;
	
	public Stage5(ActivityContext context, ActivityIdentifier parent, Job job) { 
		super(context, true, true);
		this.job = job; 
		this.parent = parent;
	}
	
	@Override
	public void initialize() throws Exception {

		start = System.currentTimeMillis();
		
		for (int i=0;i<job.tiles;i++) { 
			executor.submit(new Stage6(new UnitActivityContext(LocalConfig.host(), 6), identifier(), job, i+1, job.tiles));
		}
		
		suspend();
	}

	@Override
	public void process(Event e) throws Exception {

		ScriptResult result = (ScriptResult) e.data;

		job.addResult(result);
		
		if (result.exit != 0) { 
			System.out.println("JOB " + job.ID + " STAGE 2 FAILED " + 
					" STDERR " + new String(result.err));
			
			System.out.println("JOB " + job.ID + " STAGE 2 FAILED " + 
					" STDOUT " + new String(result.out));
		}
		
		results++;
		
		if (results == job.tiles) { 
			
			long end = System.currentTimeMillis();
			
			job.addResult(new ScriptResult("Stage 5 finished", 0, end-start));

			executor.submit(new Stage7(new UnitActivityContext(LocalConfig.host(), 7), parent, job));			
			finish();			
			
			System.out.println("JOB " + job.ID + " STAGE 5 FINISHED after " + (end-start) + " ms.");
		} else { 
			suspend();			
		} 
	}
	
	@Override
	public void cancel() throws Exception {
		// not used
	}

	@Override
	public void cleanup() throws Exception {
		// not used
	}	
}
