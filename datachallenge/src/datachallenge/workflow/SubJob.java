package datachallenge.workflow;

import ibis.cohort.Activity;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.Event;
import ibis.cohort.SimpleActivity;

public class SubJob extends SimpleActivity {

    private final String tmpDir;
    private final String before;
    private final String after;
    
    private final int threshold;
    private final int rank;
    private final int size;
    
    public SubJob(ActivityIdentifier parent, String tmpDir, String before, 
            String after, int threshold, int rank, int size) {
        super(parent, Context.COHORT);
        this.tmpDir = tmpDir;
        this.before = before;
        this.after = after;
        this.threshold = threshold;
        this.rank = rank;
        this.size = size;
    }

    @Override
    public void simpleActivity() throws Exception {
        cohort.send(identifier(), parent, LocalConfig.imsub(tmpDir, before, 
                after, threshold, rank, size));
    }

}
