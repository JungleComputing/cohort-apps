package nbia.kernels;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import ibis.imaging4j.Format;
import ibis.imaging4j.Image;
import ibis.imaging4j.Imaging4j;

public abstract class Scale {

    public abstract Image scale(Image in, Image out, int scale) throws Exception;
    
    public static Scale createScaler(String name) throws Exception { 

        if (name.equalsIgnoreCase("cuda")) { 
            return new CUDAScale();
        }
        
        if (name.equalsIgnoreCase("simple")) { 
            return new SimpleScale();
        }
        
        if (name.equalsIgnoreCase("fake")) { 
            return new FakeScale();
        }

        throw new Exception("Unknown scaler: " + name);
   }
    
    public static Scale selectScaler(String list) throws Exception { 
        
        StringTokenizer tok = new StringTokenizer(list);
        
        Scale tmp = null;
        
        while (tok.hasMoreTokens()) { 
            
            try { 
                tmp = createScaler(tok.nextToken().trim());
                return tmp;
            } catch (Exception e) {
                // Should log failure here...
            }
        }
        
        throw new Exception("No suitable scaler found!");
    }
    
    public static void main(String [] args) throws Exception { 
        
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        int scale = Integer.parseInt(args[2]);
        int count = Integer.parseInt(args[3]);
        
        int newW = width/scale;
        int newH = height/scale;
        
        Image in = new Image(Format.ARGB32, width, height);
        Image out = new Image(Format.ARGB32, newW, newH);
 
        ByteBuffer b = in.getData();
        
        byte [] tmp = new byte[width*4];
        
        b.position(0);
        b.limit(width*height*4);
        
        for (int h=0;h<height;h++) { 
            for (int w=0;w<width*4;w+=4) { 
                tmp[w] = (byte) 255;
                tmp[w+1] = (byte) (h & 0xff); 
                tmp[w+2] = (byte) (w & 0xff); 
                tmp[w+3] = (byte) ((h+w) & 0xff); 
            }
            
            b.put(tmp);
        }
    
        Scale scaler = createScaler(args[3]);
        
        long start = System.currentTimeMillis();
        
        for (int i=0;i<count;i++) { 
            scaler.scale(in, out, scale);
        }
        
        long end = System.currentTimeMillis();
         
        System.out.println("Total time " + (end-start) + " ( " 
                + ((end-start)/count) + " ms/scale)");
        
        Imaging4j.save(in, new File("input.argb"));
        Imaging4j.save(out, new File("output.argb"));
    }    
}
