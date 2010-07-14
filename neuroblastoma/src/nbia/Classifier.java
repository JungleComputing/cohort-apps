package nbia;

import ibis.cohort.Activity;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.Event;
import ibis.cohort.MessageEvent;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;

public class Classifier extends Activity {

    private final ActivityIdentifier parent;
    
    private Result statisticsResult; 
    private Result reduceResult; 
   
    public Classifier(ActivityIdentifier parent) {
        super(new UnitContext("CPU"));
        this.parent = parent;
    }

    private Result classifier() { 
        
        // Do something smart here with the results. 
        
        return new Result((MetaData) statisticsResult.getMetaData(), 
                new Object(), Result.CLASSIFIER);
    }
    
    @Override
    public void process(Event e) throws Exception {
   
        Result r = (Result) ((MessageEvent)e).message;
        
        int type = r.getType();
        
        if (type == Result.STATISTICS) { 
            statisticsResult = r;
        } else if (type == Result.REDUCE) {
            reduceResult = r;
        }
        
        // Wait until both results are in
        if (statisticsResult != null && reduceResult != null) { 
            finish();
        } else { 
            suspend();  
        }
    }
    
    @Override
    public void initialize() throws Exception {
        suspend();
    }
    
    @Override
    public void cancel() throws Exception {
        // not used
    }

    @Override
    public void cleanup() throws Exception {
        // Run the classifier and send the result to our parent
        cohort.send(identifier(), parent, classifier());
    }
}
