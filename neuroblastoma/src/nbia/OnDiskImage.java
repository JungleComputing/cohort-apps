package nbia;

import ibis.imaging4j.Format;
import ibis.imaging4j.Image;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class OnDiskImage {
    
    // This class represents an on-disk which may have a very high resolution
    // (100K or more). Most of the image remains on disk, although some 
    // recently used part may be cached in memory. 
    
    // NOTE: at the moment we only support a very simple image format consisting
    // of a seperate header and (RAW) data file.
    
    private final String headerFile; 
    private final String dataFile; 
    
    private final boolean readOnly;
    
    private final String ID;
    
    private final FileChannel file;
        
    private final long width; 
    private final long height; 
 
    private final Format format; 
    
    public OnDiskImage(String header, String data, boolean readOnly) throws NumberFormatException, IOException { 
    
        this.headerFile = header;
        this.dataFile = data;
        this.readOnly = readOnly;
        
        BufferedReader tmp = new BufferedReader(new FileReader(header));
        
        ID = readLine(tmp);
        width = Long.parseLong(readLine(tmp));
        height = Long.parseLong(readLine(tmp));
        format = parseFormat(readLine(tmp));
        
        tmp.close();
        
        file = new RandomAccessFile(data, readOnly ? "r" : "rw").getChannel();
    }
    
    public OnDiskImage(File header, File data, boolean readOnly) throws NumberFormatException, IOException { 
    
        this.headerFile = header.getName();
        this.dataFile = data.getName();
        this.readOnly = readOnly;
        
        BufferedReader tmp = new BufferedReader(new FileReader(header));
        
        ID = readLine(tmp);
        width = Long.parseLong(readLine(tmp));
        height = Long.parseLong(readLine(tmp));
        format = parseFormat(readLine(tmp));
        
        tmp.close();
        
        file = new RandomAccessFile(data, readOnly ? "r" : "rw").getChannel();
    }
    
    
    public OnDiskImage(String ID, String headerFile, String dataFile, 
            long width, long height, Format format) throws IOException { 
        
        this.headerFile = headerFile;
        this.dataFile = dataFile;
        this.readOnly = false;
        this.ID = ID;
        this.width = width;
        this.height = height;
        this.format = format;
        
        PrintWriter tmp = new PrintWriter(new BufferedWriter(new FileWriter(headerFile)));
        
        tmp.println(ID);
        tmp.println(width);
        tmp.println(height);
        tmp.println(format.getName());
        
        tmp.close();
        
        file = new RandomAccessFile(dataFile, "rw").getChannel();
        
    }
    
    private String readLine(BufferedReader in) throws IOException {
        
        String line = in.readLine();
        
        while (line != null) { 
            
            line = line.trim();
            
            if (!line.startsWith("#")) { 
                return line;
            }
            
            line = in.readLine();
        }
        
        throw new EOFException("Header ended unexpectedly!");
    }
 
    private Format parseFormat(String line) throws IOException { 
    
        if (line.equalsIgnoreCase("RGB24")) { 
            return Format.RGB24;
        } else if (line.equalsIgnoreCase("ARGB32")) { 
            return Format.ARGB32;
        }
        
        throw new IOException("Unknown image format: " + line);
    }
    
    public long getWidth() { 
        return width;
    }
    
    public long getHeight() { 
        return height;
    }
    
    public Format getFormat() { 
        return format;
    }
    
    public Image getSubImage(long offsetW, long offsetH, int w, int h) throws IOException { 
       
        // NOTE: Assumes the sub-image will fit in memory. We should also 
        // check that the pixels actually exist!
  
        // NOTE: assumes pixels are interleaved and fit into integral number 
        // of bytes
        
        int depth = format.getBitsPerPixel() / 8;
        
        MetaData data = new MetaData(ID, offsetW, offsetH, w, h, 1);
        
        Image tmp = new Image(format, w, h, 0, data);
        
        ByteBuffer buffer = tmp.getData();
        
        buffer.position(0);
        
        long offset = (width * offsetH + offsetW) * depth; 
        
        long r = 0;
        
        for (int i=0;i<h;i++) { 
            buffer.limit((i+1)*w*depth);
        
            System.out.println("Reading " + buffer.position() + " " 
                    + buffer.limit() + " from " + offset + " (" + r + ")");
            
            r += file.read(buffer, offset);
            
            offset += width*depth;
        }
        
        return tmp;
    }
    
    public void putSubImage(Image img, long offW, long offH) throws IOException { 
        
        final int w = img.getWidth();
        final int h = img.getHeight();
       
        // NOTE: assumes pixels are interleaved and fit into integral number 
        // of bytes
        
        final int depth = format.getBitsPerPixel() / 8;
       
        if (img.getFormat() != format) { 
            throw new IOException("Wrong image format!");
        }
        
        long tmp = (offH + h) * width + offW + w;
        
        if (tmp > width*height*depth) { 
            throw new IOException("Image out of bounds!");
        }
        
        ByteBuffer buffer = img.getData();
        
        buffer.position(0);
        
        long offset = (width * offH + offW) * depth; 
        
        long wrote = 0;
        
        for (int i=0;i<h;i++) { 
  
            buffer.limit((i+1)*w*depth);
        
            System.out.println("Writing " + buffer.position() + " - " 
                    + buffer.limit() + " to " + offset + " ("  + wrote + ")");
            
            wrote += file.write(buffer, offset);
            
            offset += width*depth;
        }
    }

    public void close() throws IOException {
        
        if (file.isOpen()) {
            file.close();
        }
    }  
}
