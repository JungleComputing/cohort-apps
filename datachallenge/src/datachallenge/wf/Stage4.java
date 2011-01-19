package datachallenge.wf;

import ibis.constellation.ActivityContext;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;

public class Stage4 extends SimpleActivity {
	
	private static final long serialVersionUID = 2499718890411272649L;

	private static String script = "stage4.sh";
	
	private Job job;
	private int id;
	
	protected Stage4(ActivityContext context, ActivityIdentifier parent, Job job, int id) { 
		super(parent, context, true);
		
		this.job = job;
		this.id = id; 
	}
	
	@Override
	public void simpleActivity() throws Exception {
		
		String file; 
		
		if (id == 0) { 
			file = job.beforeFileName;
		} else { 
			file = job.afterFileName;
		}
		
		ScriptResult result = LocalConfig.runScript(new String [] { 
	                LocalConfig.getScript(script), job.tmpdir, file }, id); 
	        
		// send event to signify that we are done
		executor.send(new Event(identifier(), parent, result));
	}
}
