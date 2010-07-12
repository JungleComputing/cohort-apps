package nbia;

import nbia.kernels.Conversion;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.imaging4j.Image;

public class Convertor extends SimpleActivity {

    private final ActivityIdentifier parent;
    private final Image imageIn;
    //private final String type;
    
    public Convertor(ActivityIdentifier parent, Image imageIn) {
        super(Context.ANY);
        this.parent = parent;
        this.imageIn = imageIn;
    //    this.type = type;
    }
    
    private Image convert() throws Exception {
        Conversion c = Conversion.createConversion("fake");
        return c.convert(imageIn, null);
    }
    
    @Override
    public void simpleActivity() throws Exception {
  
        // Convert to correct colorspace here
        Image tmp = convert();
        
        // Create LBP activity & submit
        cohort.submit(new LBP(parent, tmp));
        
        // Create Stat activity & submit
        cohort.submit(new Statistics(parent, tmp));        
    }
}
