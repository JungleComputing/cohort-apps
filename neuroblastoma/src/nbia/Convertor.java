package nbia;

import nbia.kernels.Conversion;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class Convertor extends SimpleActivity {

    private final ActivityIdentifier parent;
    private final Image imageIn;
    
    public Convertor(ActivityIdentifier parent, Image imageIn) {
        super(new UnitContext("GPU"));
        this.parent = parent;
        this.imageIn = imageIn;
    }
    
    private Image convert() throws Exception {
        Conversion c = Conversion.createConversion("fake");
        return c.convert(imageIn, null);
    }
    
    @Override
    public void simpleActivity() throws Exception {
  
        // Convert to correct colorspace here
        Image tmp = convert();
        
        ActivityIdentifier id = cohort.submit(new Classifier(parent));
        
        // Create Stat activity & submit
        cohort.submit(new Statistics(id, tmp));        
    
        // Create LBP activity & submit
        cohort.submit(new LBP(id, tmp));
    }
}
