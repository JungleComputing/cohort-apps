package datachallenge.simple;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;

public class LaunchJob extends SimpleActivity {

    private static final long serialVersionUID = -6249140235894579296L;

    private final Job job;
    
    public LaunchJob(ActivityIdentifier parent, Job job) {        
        super(parent, job.getContext(), false);
        this.job = job;
    }

    @Override
    public void simpleActivity() throws Exception {

        // The only goal of this job is to launch a new job on whatever location 
        // we end up on. This new job is given a restricted context, such that 
        // it cannot move to another machine.
        executor.submit(new CompareJob(parent, job));
    }

}
