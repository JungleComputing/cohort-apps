package nbia;

import nbia.kernels.Scale;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.SimpleActivity;
import ibis.cohort.context.UnitContext;
import ibis.imaging4j.Image;

public class Scaler extends SimpleActivity {
    
    private static final long serialVersionUID = -7298739515939791781L;
 
    private final Image imageIn;
    private final int scale;
    
    public Scaler(ActivityIdentifier parent, Image imageIn, int scale) {
        super(parent, new UnitContext("CPU"));
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
