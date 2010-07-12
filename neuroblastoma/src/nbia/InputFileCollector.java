package nbia;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class InputFileCollector {

    private File dir;

    static class HeaderFilter implements FileFilter {

        public boolean accept(File f) {
            
            if (f.isDirectory()) { 
                return false;
            }
            
            if (!f.canRead()) { 
                return false;
            }
            
            return f.getName().endsWith(".header");
        } 
    }

    static class DataFilter implements FileFilter {

        public boolean accept(File f) {
            
            if (f.isDirectory()) { 
                return false;
            }
            
            if (!f.canRead()) { 
                return false;
            }
            
            return f.getName().endsWith(".data");
        } 
    }
    
    public InputFileCollector(File dir) { 
        this.dir = dir;
    }  

    private File findData(File [] files, String header) {
        
        String data = header.substring(0, header.length()-7) + ".data";
        
        for (int i=0;i<files.length;i++) { 
            
            File tmp = files[i];
            
            if (tmp != null) { 
                
                if (tmp.getName().equals(data)) { 
                    files[i] = null;
                    return tmp;
                }
            }
        }
        
        // Not found
        return null;
    }
    
    public OnDiskImage [] getInputFiles() {

        ArrayList<OnDiskImage> results = new ArrayList<OnDiskImage>();
        
        File [] headers = dir.listFiles(new HeaderFilter());
        File [] data = dir.listFiles(new DataFilter());

        if (headers.length == 0) { 
            System.err.println("No input files found!");
            return new OnDiskImage[0];
        }

        for (File f : headers) { 

            File tmp = findData(data, f.getName());

            if (tmp != null) { 
                System.out.println("Found input: " + f.getName() + " / " 
                        + tmp.getName());
                
                try { 
                    results.add(new OnDiskImage(f, tmp, true));
                } catch (Exception e) { 
                    System.err.println("Failed to add input pair: " 
                            + f.getName() + " / " 
                            + tmp.getName());
                    e.printStackTrace(System.err);
                }
            } 
        }

        return results.toArray(new OnDiskImage[results.size()]);
    }
}
