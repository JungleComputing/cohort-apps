package datachallenge.simple;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;
import ibis.constellation.context.UnitActivityContext;

public class ListJob extends SimpleActivity {
    
    private static final long serialVersionUID = 7971328175183203989L;
    
    public ListJob(ActivityIdentifier parent, String cluster) {
        super(parent, new UnitActivityContext(cluster));
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, LocalConfig.listProblems()));
    }
}
