package datachallenge.workflow;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class DetectJob extends SimpleActivity {

    private static final long serialVersionUID = 4332877306677438467L;

    private static String script = "stage1.sh";
    
    private final String file;
    private final String tmpDir;
    private final int threshold;
    
    public DetectJob(ActivityIdentifier parent, Context c, String tmpDir, 
            String file, int threshold) {
        
        super(parent, c);
        
        this.tmpDir = tmpDir;
        this.file = file;
        this.threshold = threshold;
    }

    @Override
    public void simpleActivity() throws Exception {
        
        ScriptResult result = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript(script), tmpDir, file, "" + threshold }); 
        
        cohort.send(identifier(), parent, result);
    }
}
