package nbia;

import nbia.kernels.Conversion;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class Convertor extends SimpleActivity {

    private static final long serialVersionUID = 4551613286451029630L;

    private final Image imageIn;
    
    public Convertor(ActivityIdentifier parent, Image imageIn) {
        super(parent, new UnitContext("GPU"));
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
