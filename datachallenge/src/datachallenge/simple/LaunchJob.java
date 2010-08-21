package datachallenge.simple;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class LaunchJob extends SimpleActivity {

    private static final long serialVersionUID = -6249140235894579296L;

    private final Job job;
    
    public LaunchJob(ActivityIdentifier parent, Context c, int rank, Job job) {        
        super(parent, c, rank, false);
        this.job = job;
    }

    @Override
    public void simpleActivity() throws Exception {

        // The only goal of this job is to launch a new job on whatever location 
        // we end up on. This new job is given a restricted context, such that 
        // it cannot move to another machine.
        
        Context c = new UnitContext(LocalConfig.cluster());
        cohort.submit(new CompareJob(parent, c, getRank(), job));
    }

}
