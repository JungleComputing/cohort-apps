package datachallenge.simple;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class ListJob extends SimpleActivity {
    
    private static final long serialVersionUID = 7971328175183203989L;
    
    public ListJob(ActivityIdentifier parent, String cluster) {
        super(parent, new UnitContext(cluster));
    }

    @Override
    public void simpleActivity() throws Exception {
        cohort.send(identifier(), parent, LocalConfig.listProblems());
    }
}
