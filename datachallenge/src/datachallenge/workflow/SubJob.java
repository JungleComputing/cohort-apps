package datachallenge.workflow;

import ibis.cohort.ActivityContext;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;

public class SubJob extends SimpleActivity {

    private static final long serialVersionUID = 1209061178355255853L;

    private static String script = "stage3.sh";
    
    private final String tmpDir;
    private final String before;
    private final String after;
    
    private final int threshold;
    private final int rank;
    private final int size;
    
    public SubJob(ActivityIdentifier parent, ActivityContext c, String tmpDir, 
            String before, String after, int threshold, int rank, int size) {
        super(parent, c, true);
        this.tmpDir = tmpDir;
        this.before = before;
        this.after = after;
        this.threshold = threshold;
        this.rank = rank;
        this.size = size;
    }

    @Override
    public void simpleActivity() throws Exception {
        
        ScriptResult result = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript(script), tmpDir, before, after, 
                "" + threshold, "" + rank, "" + size });
        
        cohort.send(identifier(), parent, result);         
    }

}
