package datachallenge.workflow;

import ibis.cohort.ActivityContext;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;

public class DetectJob extends SimpleActivity {

    private static final long serialVersionUID = 4332877306677438467L;

    private static String script = "stage1.sh";
    
    private final String file;
    private final String tmpDir;
    private final int threshold;
    
    public DetectJob(ActivityIdentifier parent, ActivityContext c, String tmpDir, 
            String file, int threshold) {
        
        super(parent, c, true);
        
        this.tmpDir = tmpDir;
        this.file = file;
        this.threshold = threshold;
    }

    @Override
    public void simpleActivity() throws Exception {
        
        ScriptResult result = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript(script), tmpDir, file, "" + threshold }); 
        
        executor.send(identifier(), parent, result);
    }
}
