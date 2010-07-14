package nbia.kernels;

import ibis.imaging4j.Image;

public abstract class Conversion {

    public abstract Image convert(Image in, Image out) throws Exception;
   
    private static String DEFAULT = "java";
    private static String name;
    
    private static String getName() { 
        
        if (name == null) { 
            name = System.getProperty("nbia.conversion");
            
            if (name == null) { 
                name = DEFAULT;
            }
        }
        
        return name;
    }
    
    public static Conversion createConversion() throws Exception { 
        return createConversion(getName());
    }
        
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
