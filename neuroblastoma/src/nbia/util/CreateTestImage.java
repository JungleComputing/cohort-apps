package nbia.util;

import ibis.imaging4j.Format;
import ibis.imaging4j.Image;
import ibis.imaging4j.Imaging4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import nbia.OnDiskImage;

public class CreateTestImage {

    private static Format parseFormat(String line) throws IOException { 
    
        if (line.equalsIgnoreCase("RGB24")) { 
            return Format.RGB24;
        } else if (line.equalsIgnoreCase("ARGB32")) { 
            return Format.ARGB32;
        }
        
        throw new IOException("Unknown image format: " + line);
    }
 
    
    public static void main(String [] args) throws Exception { 
        
        String outputname = args[0];
       
        Image source = Imaging4j.load(new File(args[1]));
   
        if (source.getFormat() != Format.RGB24) { 
            source = Imaging4j.convert(source, Format.RGB24);
        }
        
        ByteBuffer buf = source.getData();
        
        int dupX = Integer.parseInt(args[2]);
        int dupY = Integer.parseInt(args[3]);
        
        long width = source.getWidth() * dupX;
        long height = source.getHeight() * dupY;
        
        OnDiskImage image = new OnDiskImage(outputname, outputname + ".header",
                outputname + ".data", width, height, source.getFormat());
        
        for (int y=0;y<dupY;y++) { 
            for (int x=0;x<dupX;x++) { 
                image.putSubImage(source, x*source.getWidth() , y*source.getHeight());
            }
        }
        
        image.close();
    }
    
}
