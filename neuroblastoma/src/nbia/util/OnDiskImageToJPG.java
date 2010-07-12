package nbia.util;

import ibis.imaging4j.Format;
import ibis.imaging4j.Image;
import ibis.imaging4j.Imaging4j;

import java.io.File;
import java.nio.ByteBuffer;

import nbia.OnDiskImage;

public class OnDiskImageToJPG {

    public static void main(String [] args) throws Exception { 
        
        OnDiskImage image = new OnDiskImage(args[0], args[1], true);
        
        System.out.println("Image " + image.getWidth() + "x" + image.getHeight() 
                + " " + image.getFormat() + " opened");
        
        if (image.getWidth() > 16*1024 || image.getHeight() > 16*1024) { 
            System.out.println("Image too large to convert to JPG");
            image.close();
            return;
        }
        
        Image tmp = image.getSubImage(0, 0, (int)image.getWidth(), (int)image.getHeight());
        
        image.close();
        
        tmp = Imaging4j.convert(tmp, Format.JPG);
       
        ByteBuffer b = tmp.getData();
        
        Imaging4j.save(tmp, new File(args[2]));
        
    }
    
}
