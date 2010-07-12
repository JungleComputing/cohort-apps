package nbia.kernels;

import java.io.File;
import java.nio.ByteBuffer;

import ibis.imaging4j.Format;
import ibis.imaging4j.Image;
import ibis.imaging4j.Imaging4j;

public class CUDAScale extends Scale {

    // NOTE: should count this!
    private static long FLOPS_PER_PIXEL_4 = 1;
    private static long FLOPS_PER_PIXEL_16 = 1;
    
    private static boolean libraryLoaded = false;

    static {
        try { 
            loadLibrary();
        } catch (Exception e) {
            // ignore
        }
    }

    private static void loadLibrary() throws Exception {

        if (libraryLoaded) { 
            return;
        }

        // We start by figuring out which library we should load by looking at 
        // the OS specified in the System properties. 

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String version = System.getProperty("os.version").toLowerCase();

        System.out.println("Running on: " + os + ", " + version + ", " + arch);

        String library = null;

        String name = "SIMPLE_CUDA_SCALER";

        if (os.equals("linux")) { 

            if (arch.equals("i386")) { 
                library = name + "-Linux-i386";
            } else if (arch.equals("amd64")) { 
                library = name + "-Linux-amd64";                
            } else if (arch.equals("x86_64")) { 
                library = name + "-Linux-x86_64";                
            } else { 
                throw new Exception("Unsupported OS/architecture: " + os 
                        + "/" + arch);
            }

        } else { 
            throw new Exception("Unsupported OS: " + os + "/" + arch);
        }

        System.out.println("Library to load: " + library);

        try { 
            System.loadLibrary(library);
        } catch (Throwable e) { 
            System.err.println("Failed to load library: " + library + " " 
                    + e.getMessage());
            e.printStackTrace();
            throw new Exception("Failed to load library: " + library, e);
        }

        libraryLoaded = true;        
    }
    
    private static native int scaleRGBA(ByteBuffer in, int w, int h, int scale,
            ByteBuffer out);
    
    private static native int scaleRGBA(ByteBuffer in, int w, int h, int scale,
            byte [] out);
    
    private static native int scaleRGBA(byte [] in, int w, int h, int scale,
            ByteBuffer out);
    
    private static native int scaleRGBA(byte [] in, int w, int h, int scale,
            byte [] out);
    
    public Image scale(Image in, Image out, int scale) throws Exception { 
        
        if (in.getFormat() != Format.ARGB32) { 
            throw new Exception("Can only handle ARGB32 input");
        }
    
        int w = in.getWidth();
        int h = in.getHeight();
        
        if (w % scale != 0 || h % scale != 0) { 
            throw new Exception("Can only handle integral scaling");
        }
        
        int newW = w / scale;
        int newH = h / scale;
   
        if (out != null) { 
            if (out.getFormat() != Format.ARGB32) { 
                throw new Exception("Can only handle ARGB32 output");
            }

            if (out.getWidth() != newW || out.getHeight() != newH) { 
                throw new Exception("Wrong output format");
            }
            
        } else { 
            out = new Image(Format.ARGB32, newW, newH);
        }
        
        ByteBuffer inbuf = in.getData();
        ByteBuffer outbuf = out.getData();
        
        if (inbuf.isDirect()) { 
            if (outbuf.isDirect()) { 
                scaleRGBA(inbuf, w, h, scale, outbuf);
            } else {
                scaleRGBA(inbuf, w, h, scale, outbuf.array());
            }
        } else {
            if (outbuf.isDirect()) { 
                scaleRGBA(inbuf.array(), w, h, scale, outbuf);
            } else {
                scaleRGBA(inbuf.array(), w, h, scale, outbuf.array());
            }
        }
        return out;
    }
    
   
}
