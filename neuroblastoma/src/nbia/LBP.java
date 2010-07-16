package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class LBP extends SimpleActivity {

    private static final long serialVersionUID = -3429910827641400311L;
    
    private final Image imageIn;
    
    public LBP(ActivityIdentifier parent, Image imageIn) {
        super(parent, new UnitContext("GPU"));
        this.imageIn = imageIn;
    }
    
    private Result doLBP() { 
        
        // TODO: Do LBP + hist here...
        
        return new Result((MetaData) imageIn.getMetaData(), 
                new Object(), Result.LBP);
    }
    
    @Override
    public void simpleActivity() throws Exception {
        // Do LBP and hist and submit final analysis
        cohort.submit(new Reduce(parent, doLBP()));
    }
}
