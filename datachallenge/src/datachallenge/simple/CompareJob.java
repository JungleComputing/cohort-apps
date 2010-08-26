package datachallenge.simple;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;

public class CompareJob extends SimpleActivity {

    private static final long serialVersionUID = -653442064273941414L;
    
    private final Job job;
    
    public CompareJob(ActivityIdentifier parent, Job job) {
        super(parent, job.getContext(), true);
        this.job = job;
    }

    @Override
    public void simpleActivity() throws Exception {
        cohort.send(identifier(), parent, LocalConfig.compare(job));
    }
}
