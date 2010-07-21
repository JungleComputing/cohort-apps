package datachallenge.workflow;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;

public class DetectJob extends SimpleActivity {

    private static final long serialVersionUID = 4332877306677438467L;
    
    private final String file;
    private final String tmpDir;
    private final int threshold;
    
    public DetectJob(ActivityIdentifier parent, String tmpDir, String file, int threshold) {
        super(parent, Context.COHORT);
        this.tmpDir = tmpDir;
        this.file = file;
        this.threshold = threshold;
    }

    @Override
    public void simpleActivity() throws Exception {
        cohort.send(identifier(), parent, LocalConfig.detect(tmpDir, file, threshold));
    }
}
