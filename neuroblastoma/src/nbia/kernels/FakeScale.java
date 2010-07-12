package nbia.kernels;

import ibis.imaging4j.Image;

public class FakeScale extends Scale {

    private static final double TIME_MEASURED = 3.148; // in ms.
    private static final double SIZE_MEASURED = 1024.0*1024.0; // in pixels
    
    private int delay(int w, int h, int scale) { 
        
        // Simply assume that 512x512 takes 1 ms. Then multiply such that the 
        // time corresponds to the pixelcount;
        double factor = (w*h)/SIZE_MEASURED; 
        return (int)(factor*TIME_MEASURED*1000);
    }
    
    
    @Override
    public Image scale(Image in, Image out, int scale) throws Exception {

        if (out == null) { 
            out = new Image(in.getFormat(), 
                    in.getWidth() / scale, 
                    in.getHeight() / scale);
        }
        
        int delay = delay(in.getWidth(), in.getHeight(), scale);
        
        try { 
            Thread.sleep(delay);
        } catch (Exception e) {
            // ignored
        }
        
        return out;
    }

}
