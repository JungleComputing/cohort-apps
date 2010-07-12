package nbia;

import nbia.kernels.Scale;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;
import ibis.imaging4j.Image;

public class Scaler extends SimpleActivity {

    private final ActivityIdentifier parent;
    private final Image imageIn;
    private final int scale;
    
    public Scaler(ActivityIdentifier parent, Image imageIn, int scale) {
        super(Context.ANY);
        this.parent = parent;
        this.imageIn = imageIn;
        this.scale = scale;
    }
    
    private Image scaleImage() throws Exception { 
        
      //  Scale scaler = (Scale) EquiKernel.select("scaler");
      //  Scale scaler = Scale.selectScaler(type);
      //  return scaler.scale(imageIn, null, scale);
        
        Scale scaler = Scale.selectScaler("fake");
        
        return scaler.scale(imageIn, null, scale);
    }
    
    @Override
    public void simpleActivity() throws Exception {
        // Directly create & sumbit new color convertor.
        cohort.submit(new Convertor(parent, scaleImage()));
    }
}
