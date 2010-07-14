package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class Statistics extends SimpleActivity {

    private final ActivityIdentifier parent;
    private final Image imageIn;
    
    public Statistics(ActivityIdentifier parent, Image imageIn) {
        super(new UnitContext("GPU"));
        this.parent = parent;
        this.imageIn = imageIn;
    }
    
    private Result analyze() { 
        // TODO: do statistical analysis of image here
        return new Result((MetaData)imageIn.getMetaData(), 
                new Object(), Result.STATISTICS);
    }
    
    @Override
    public void simpleActivity() throws Exception {
        cohort.send(identifier(), parent, analyze());
    }
 }
