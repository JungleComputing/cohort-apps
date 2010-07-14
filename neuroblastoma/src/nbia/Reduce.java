package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class Reduce extends SimpleActivity {

    private final ActivityIdentifier parent;
    private final Result input;
    
    public Reduce(ActivityIdentifier parent, Result input) { 
        super(new UnitContext("CPU"));
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
