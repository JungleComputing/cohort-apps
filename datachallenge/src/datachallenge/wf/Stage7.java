package datachallenge.wf;

import ibis.cohort.ActivityContext;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;

public class Stage7 extends SimpleActivity {

	private static String script = "stage7.sh";
	private Job job;
	
	protected Stage7(ActivityContext context, ActivityIdentifier parent, Job job) { 
		super(parent, context, true);
		this.job = job;
	}
	
	@Override
	public void simpleActivity() throws Exception {
		
		long start = System.currentTimeMillis();
		
		ScriptResult result = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript(script), job.tmpdir, job.beforeFileName, job.afterFileName }, 0); 
		
		job.addResult(result);
		
		long end = System.currentTimeMillis();
		
		System.out.println("JOB " + job.ID + " STAGE 7 FINISHED after " + (end-start) + " ms.");
		
		// send event to signify that we are done
		executor.send(identifier(), parent, job);
	}
}
