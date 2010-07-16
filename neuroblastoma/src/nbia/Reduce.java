package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class Reduce extends SimpleActivity {

    private static final long serialVersionUID = -8890166876978924388L;

    private final Result input;
    
    public Reduce(ActivityIdentifier parent, Result input) { 
        super(parent, new UnitContext("CPU"));
        this.parent = parent;
        this.input = input;
    }
    
    private Result processResults() { 
        // TODO: process result here
        
        return new Result(input.getMetaData(), 
                new Object(), Result.REDUCE);
    }
    
    @Override
    public void simpleActivity() throws Exception {
        // Reduce results here and send the result back to parent
        cohort.send(identifier(), parent, processResults());
    }
}
