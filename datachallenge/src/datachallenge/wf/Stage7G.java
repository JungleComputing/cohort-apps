package datachallenge.wf;

import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;

import ibis.constellation.ActivityContext;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

public class Stage7G extends SimpleActivity {

	private static final long serialVersionUID = 7906153662103178461L;

	private static String script = "stage7G.sh";
	
	private Job job;
	
	private static final Filter f = new Filter();
	
	private static class Filter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return (name != null && name.contains("_gputile."));
		} 
	}
	
	protected Stage7G(ActivityContext context, ActivityIdentifier parent, Job job) { 
		super(parent, context, true);
		this.job = job;
	}
	
	private int getTiles(String tmpdir) {
		
		File dir = new File(tmpdir);
		
		String [] files = dir.list(f);
		
		if (files == null || files.length == 0) { 
			return 0;
		}
		
		return files.length;
	}
	
	@Override
	public void simpleActivity() throws Exception {
		
		long start = System.currentTimeMillis();
		
		int tiles = getTiles(job.tmpdir);
		
		ScriptResult result;
		
		if (tiles > 0) { 
		
			long now = System.currentTimeMillis();
			
			System.out.println("GPU " + now + " 0.0");
			System.out.println("GPU " + now + " 1.0");
			
			// Start the GPU processing
			result = LocalConfig.runScript(new String [] { 
						LocalConfig.getScript(script), job.tmpdir, 
						job.beforeFileName }, 0); 
	
			now = System.currentTimeMillis();
			
			System.out.println("GPU " + now + " 1.0");
			System.out.println("GPU " + now + " 0.0");
			
		} else { 
			result = new ScriptResult("STAGE 7G skipped", 0, 0, 0);
		}
		
		long end = System.currentTimeMillis();
	
		job.addResult(result);
			
		if (result.exit != 0) { 
			// Job failed 
			System.out.println("JOB " + job.ID + " STAGE 7G FAILED after " + (end-start) +
					" ms. STDERR " + new String(result.err));
				
			executor.send(new Event(identifier(), parent, job));
			return;
		} 
			
		System.out.println("JOB " + job.ID + " STAGE 7G FINISHED after " + (end-start) + " ms.");
		
		executor.submit(new Stage7(new UnitActivityContext(LocalConfig.host(), 7), parent, job));			
	}	
}
