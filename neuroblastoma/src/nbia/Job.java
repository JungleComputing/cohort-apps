package nbia;

import ibis.cohort.Activity;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.Event;
import ibis.cohort.MessageEvent;
import ibis.imaging4j.Image;

public class Job extends Activity {

    private final ActivityIdentifier parent;
    private final Image imageIn;
    private int scale;
    
    private Result statisticsResult; 
    private Result reduceResult; 
    private Result classifierResult; 
    
    public Job(ActivityIdentifier parent, Image imageIn, int initialScale) {
        super(Context.ANY);
        this.parent = parent;
        this.imageIn = imageIn;
        this.scale = initialScale;
    }
   
    @Override
    public void initialize() throws Exception {
        cohort.submit(new Scaler(identifier(), imageIn, scale));
        suspend();
    }

    private boolean classifier() { 
        
        // Do something smart here with the results. 
        statisticsResult = null;
        reduceResult = null;
      
        scale = scale / 2;
        
        if (scale == 0) { 
            // When we run out of scale a result is forced.
            classifierResult = new Result((MetaData) imageIn.getMetaData(), 
                    new Object(), Result.CLASSIFIER);
        
            return true;
        }
   
        return false;
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
       
            // Run the classifier to determine if we are finished
            boolean done = classifier();
            
            // If we are, send the result to our parent
            if (done) { 
                cohort.send(identifier(), parent, classifierResult);
                finish();
                return;
            }
         
            // Otherwise, submit a new job performing the calculation on a 
            // larger scale
            cohort.submit(new Scaler(identifier(), imageIn, scale));
        } 
  
        // We suspend here waiting for more results.
        suspend();
    }

    @Override
    public void cancel() throws Exception {
        // Not used
    }
    
    @Override
    public void cleanup() throws Exception {
        // Not used
    }
}
