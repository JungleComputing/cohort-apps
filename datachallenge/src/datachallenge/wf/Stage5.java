package datachallenge.wf;

import java.util.StringTokenizer;

import ibis.cohort.Activity;
import ibis.cohort.ActivityContext;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Event;
import ibis.cohort.MessageEvent;
import ibis.cohort.context.UnitActivityContext;

public class Stage5 extends Activity {

	private static final long serialVersionUID = 2353416132427552293L;

	private Job job;
	private ActivityIdentifier parent;
	
	private String tmpdir;

	private int results;
	
	private long start;
	
	public Stage5(ActivityContext context, ActivityIdentifier parent, Job job) { 
		super(context);
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

		ScriptResult result = (ScriptResult) ((MessageEvent) e).message;

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

			// NOTE: Stage 7 intentionally has rank of 1 to ensure that we finish the job ASAP.  
			executor.submit(new Stage7(new UnitActivityContext(LocalConfig.host(), 1), parent, job));			
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
