package nbia.kernels;

import ibis.imaging4j.Image;

public abstract class Conversion {

    public abstract Image convert(Image in, Image out) throws Exception;
    
    public static Conversion createConversion(String name) throws Exception { 
        
        if (name.equalsIgnoreCase("cuda")) { 
            return new CUDAConversion();
        }
        
        if (name.equalsIgnoreCase("java")) { 
            return new SimpleConversion();
        }
        
        if (name.equalsIgnoreCase("fake")) { 
            return new FakeConversion();
        }
        
        throw new Exception("Unknown conversion: " + name);
    }
    
    
}
