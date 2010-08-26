package datachallenge.workflow;

import ibis.cohort.ActivityContext;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;

public class LaunchJob extends SimpleActivity {

    private static final long serialVersionUID = -6249140235894579296L;

    private final String input;
    private final int split;
    
    public LaunchJob(ActivityIdentifier parent, ActivityContext c, 
            String input, int split) {
        
        super(parent, c, false);
         
        this.input = input;
        this.split = split;
    }

    @Override
    public void simpleActivity() throws Exception {

        // The only goal of this job is to launch a new job on whatever location 
        // we end up on. This new job is given a restricted context, such that 
        // it cannot move to another machine.
    	cohort.submit(new CompareJob(parent, getContext(), input, split));
    }

}
