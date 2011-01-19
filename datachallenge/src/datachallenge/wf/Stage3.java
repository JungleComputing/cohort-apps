package datachallenge.wf;

import java.util.StringTokenizer;

import ibis.constellation.Activity;
import ibis.constellation.ActivityContext;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class Stage3 extends Activity {

	private static final long serialVersionUID = -3166586121152082951L;
	
	private static String script = "stage3.sh";
	
	private Job job;
	private ActivityIdentifier parent;
	
	private int results = 0;
	
	private long start;
	
	public Stage3(ActivityContext context, ActivityIdentifier parent, Job job) { 
		super(context, true, true);
		this.job = job; 
		this.parent = parent;
	}
	
	@Override
	public void initialize() throws Exception {

		start = System.currentTimeMillis();
		
		// Run the matching		
		ScriptResult result = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript(script), job.tmpdir, job.beforeFileName, job.afterFileName }, 0); 

		job.addResult(result);
		
		if (result.exit != 0) { 
			// Failed to perform matching
			executor.send(new Event(identifier(), parent, job));
			finish();
			return;
		}
			
		// spawn the histogram jobs
		executor.submit(new Stage4(new UnitActivityContext(LocalConfig.host(), 4), identifier(), job, 0));
		executor.submit(new Stage4(new UnitActivityContext(LocalConfig.host(), 4), identifier(), job, 1));
	
		suspend();
	}
	
	private int getTiles(ScriptResult out) {

		/*
		System.out.println("JOB " + job.ID + " STAGE 3 HARDCODED TO 6 TILES!");
		return 6;
		 */
		
		if (out.out == null) {
			System.out.println("JOB " + job.ID + " STAGE 3 FAILED TO DETECT TILES (no output)!");
			return 0; 
		}
		
		StringTokenizer tok = new StringTokenizer(new String(out.out), "\n");
		
		
		while (tok.hasMoreTokens()) { 
			
			String line = tok.nextToken();
				
			if (line.contains("Tiles")) { 
				// Found the line that we are looking for!
				
				StringTokenizer tok2 = new StringTokenizer(line);
				
				while (tok2.hasMoreTokens()) { 
					
					String word = tok2.nextToken();
						
					if (word.equals("Tiles")) { 
						
						int x = Integer.parseInt(tok2.nextToken());
						tok2.nextToken();
						int y = Integer.parseInt(tok2.nextToken());
						
						System.out.println("JOB " + job.ID + " STAGE 3 DETECT TILES " + x + " x " + y + " (" + (x*y) + ")");
						
						return x*y;
					}
				}
			}
		}
		
		System.out.println("JOB " + job.ID + " STAGE 3 FAILED TO DETECT TILES!");
		return 0; 
	}
	
	@Override
	public void process(Event e) throws Exception {

		ScriptResult result = (ScriptResult) e.data;

		job.addResult(result);
		
		results++;
		
		if (results == 2) { 
			
			long end = System.currentTimeMillis();
			
			job.addResult(new ScriptResult("Stage 3 finished", 0, end-start));
			
			job.tiles = getTiles(result);
			
			// FIXME: HACK send about 10% of work to CPU instead of GPU
			if (LocalConfig.hasGPU() /*&& Math.random() > 0.15*/) { 
				System.out.println("JOB " + job.ID + " SEND TO GPU BY STAGE 3");
				executor.submit(new Stage5G(new UnitActivityContext(LocalConfig.host(), 100), parent, job));			
			} else { 
				System.out.println("JOB " + job.ID + " SEND TO CPU BY STAGE 3");
				executor.submit(new Stage5(new UnitActivityContext(LocalConfig.host(), 5), parent, job));			
			}
			
			System.out.println("JOB " + job.ID + " STAGE 3 FINISHED after " + (end-start) + " ms.");
			
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
