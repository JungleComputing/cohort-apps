package datachallenge.workflow;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitActivityContext;

public class ListJob extends SimpleActivity {
    
    private static final long serialVersionUID = 7971328175183203989L;
    
    public ListJob(ActivityIdentifier parent, String cluster) {
        super(parent, new UnitActivityContext(cluster));
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(identifier(), parent, LocalConfig.listProblems());
    }
}
