package nbia;

import ibis.cohort.Activity;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Event;
import ibis.cohort.MessageEvent;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class Job extends Activity {

    private final ActivityIdentifier parent;
    private final Image imageIn;
    private int scale;
    
    private Result classifierResult; 
    
    public Job(ActivityIdentifier parent, Image imageIn, int initialScale) {
        super(new UnitContext("CPU"));
        this.parent = parent;
        this.imageIn = imageIn;
        this.scale = initialScale;
    }
   
    @Override
    public void initialize() throws Exception {
        cohort.submit(new Scaler(identifier(), imageIn, scale));
        suspend();
    }
    
    @Override
    public void process(Event e) throws Exception {

        Result r = (Result) ((MessageEvent)e).message;
        
        boolean done = (Boolean) r.getResult();
        
        // If we are, send the result to our parent
        if (done) { 
            cohort.send(identifier(), parent, classifierResult);
            finish();
            return;
        }
         
        // Otherwise, submit a new job performing the calculation on a 
        // larger scale
        scale = scale / 2;
        cohort.submit(new Scaler(identifier(), imageIn, scale));
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
