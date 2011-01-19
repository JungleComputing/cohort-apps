package datachallenge.simple;

import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.SimpleActivity;

public class CompareJob extends SimpleActivity {

    private static final long serialVersionUID = -653442064273941414L;
    
    private final Job job;
    
    public CompareJob(ActivityIdentifier parent, Job job) {
        super(parent, job.getContext(), true);
        this.job = job;
    }

    @Override
    public void simpleActivity() throws Exception {
        executor.send(new Event(identifier(), parent, LocalConfig.compare(job)));
    }
}
