package datachallenge.wf;

import ibis.constellation.ActivityContext;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;

public class Stage6G extends SimpleActivity {

	private static final long serialVersionUID = 7906153662103178461L;

	private static String script = "stage6G.sh";
	
	private Job job;
	private int index;
	private int tiles;
		
	protected Stage6G(ActivityContext context, ActivityIdentifier parent, Job job, 
			int index, int tiles) { 
		super(parent, context, true);
		this.job = job;
		this.index = index;
		this.tiles = tiles;
	}
	
	@Override
	public void simpleActivity() throws Exception {
		
		long start = System.currentTimeMillis();
		
		ScriptResult result = LocalConfig.runScript(new String [] { 
	                LocalConfig.getScript(script), job.tmpdir, 
	                job.beforeFileName, job.afterFileName, "" + job.threshold, 
	                "" + index, "" + tiles }, index); 

		long end = System.currentTimeMillis();
		
		System.out.println("JOB " + job.ID + " STAGE 6G." + index + " FINISHED after " + (end-start) + " ms.");
		
		// send event to signify that we are done
		executor.send(new Event(identifier(), parent, result));
	}	
}
