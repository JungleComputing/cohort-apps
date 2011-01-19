package datachallenge.wf;

import java.io.File;

import datachallenge.wf.LocalConfig;
import ibis.constellation.Activity;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class Stage1 extends Activity {

	private static final long serialVersionUID = -3166586121152082951L;

	private Job job;
	private ActivityIdentifier parent;
	
	private int results = 0;
	
	private long start;
	
	public Stage1(ActivityIdentifier parent, Job job) { 
		super(new UnitActivityContext(LocalConfig.host(), 1), true, true);
		this.job = job; 
		this.parent = parent;
	}
		
	@Override
	public void initialize() throws Exception {

		start = System.currentTimeMillis();
		
		// Prepare a tmp dir
		File tmp = LocalConfig.generateTmpDir(job.ID);	       
		job.tmpdir = tmp.getAbsolutePath();

		System.out.println("JOB " + job.ID + " STAGE 1 generated tmpdir " + job.tmpdir);
		
		// copy the input files to tmp dir
		ScriptResult error = LocalConfig.copyHTTPInput(job);
		
		if (error != null) { 
			// Job failed!
			System.out.println("ERROR: JOB " + job.ID + " STAGE 1 failed to copy input files!");

			job.addResult(error);
			executor.send(new Event(identifier(), parent, job));
			finish();

		} else { 		
			// spawn stage2
			executor.submit(new Stage2(new UnitActivityContext(LocalConfig.host(), 2), identifier(), job, 0));
			executor.submit(new Stage2(new UnitActivityContext(LocalConfig.host(), 2), identifier(), job, 1));
			suspend();
		}
	}

	@Override
	public void process(Event e) throws Exception {

		ScriptResult result = (ScriptResult) e.data;
		
		job.addResult(result);
		
		results++;

		if (result.exit != 0) { 
			System.out.println("JOB " + job.ID + " STAGE 2 FAILED " + 
					" STDERR " + new String(result.err));
			
			System.out.println("JOB " + job.ID + " STAGE 2 FAILED " + 
					" STDOUT " + new String(result.out));
		}
		
		if (results == 2) { 
			
			long end = System.currentTimeMillis();
			
			System.out.println("JOB " + job.ID + " STAGE 1 FINISHED after " + (end-start) + " ms.");

			job.addResult(new ScriptResult("Stage 1 finished", 0, end-start));

			executor.submit(new Stage3(new UnitActivityContext(LocalConfig.host(), 3), parent, job));			
			
			
			finish();			
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
