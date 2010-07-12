package nbia.kernels;

import ibis.imaging4j.Format;
import ibis.imaging4j.Image;

public class FakeConversion extends Conversion {

    private static final double GPU_TIME_ON_POSTER = 2.23;   // in ms. 
    private static final double CPU_TIME_ON_POSTER = 267.92; // in ms. 
    
    private static final int SIZE_IN_PAPER = 1024*1024; // in pixels
    
    private int delayGPU(int w, int h) { 
        double factor = (w*h)/SIZE_IN_PAPER; 
        return (int)(factor*GPU_TIME_ON_POSTER*1000);
    }
    
    private int delayCPU(int w, int h) { 
        double factor = (w*h)/SIZE_IN_PAPER; 
        return (int)(factor*CPU_TIME_ON_POSTER*1000);
    }
    
    @Override
    public Image convert(Image in, Image out) throws Exception {

        if (out == null) { 
            out = new Image(Format.LAB, in.getWidth(), in.getHeight());
        }
        
        int delay = delayGPU(in.getWidth(), in.getHeight());
        
        try { 
            Thread.sleep(delay);
        } catch (Exception e) {
            // ignored
        }
        
        return null;
    }

}
