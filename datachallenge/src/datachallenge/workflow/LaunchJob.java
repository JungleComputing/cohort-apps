package datachallenge.workflow;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class LaunchJob extends SimpleActivity {

    private static final long serialVersionUID = -6249140235894579296L;

    private final String input;
    private final int split;
    
    public LaunchJob(ActivityIdentifier parent, Context c, String input, 
            int split) {
        
        super(parent, c);
         
        this.input = input;
        this.split = split;
    }

    @Override
    public void simpleActivity() throws Exception {

        // The only goal of this job is to launch a new job on whatever location 
        // we end up on. This new job is given a restricted context, such that 
        // it cannot move to another machine.
        
        Context c = new UnitContext(LocalConfig.host());
        cohort.submit(new CompareJob(parent, c, input, split));
    }

}
