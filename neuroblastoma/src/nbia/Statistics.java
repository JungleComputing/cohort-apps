package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class Statistics extends SimpleActivity {

    private static final long serialVersionUID = 6417797064770666866L;

    private final Image imageIn;
    
    public Statistics(ActivityIdentifier parent, Image imageIn) {
        super(parent, new UnitContext("GPU"));
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
