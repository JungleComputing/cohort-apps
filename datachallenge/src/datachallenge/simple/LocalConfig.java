package datachallenge.simple;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class LocalConfig {

    private static boolean configured = false;
    
    private static String cluster; 
    private static String dataDir; 
    private static String execDir; 
    private static String executable; 
    
    static class DataFilter implements FileFilter {

        public boolean accept(File f) {
            
            if (f.isDirectory()) { 
                return false;
            }
            
            if (!f.canRead()) { 
                return false;
            }
            
            return f.getName().endsWith("_t0.fits");
        } 
    }
    
    public static synchronized void configure(String clusterName, String dataD, 
            String execD, String execN) throws Exception { 
    
        if (configured) { 
            return;
        }
 
        cluster = clusterName;
        dataDir = dataD;
        execDir = execD;
        
        if (dataDir == null) { 
            throw new Exception("Data dir not specified!");
        }
        
        if (!directoryExists(dataDir)) { 
            throw new Exception("Data dir not useable!");
        }
        
        if (execDir == null) { 
            throw new Exception("Exec dir not specified!");
        }
        
        if (!directoryExists(execDir)) { 
            throw new Exception("Exec dir not useable!");
        }
        
        executable = execDir + File.separator + execN;
        
        if (!fileExists(executable)) { 
            throw new Exception("Executable not found: " + executable);
        }
        
        configured = true;
    }
        
    public static synchronized boolean isConfigured() {
        return configured;
    }
    
    private static boolean directoryExists(File dir) { 
        return dir.exists() && dir.canRead() && dir.isDirectory();
    }

    private static boolean directoryExists(String dir) {
        return directoryExists(new File(dir));
    }

    private static boolean fileExists(File file) { 
        return file.exists() && file.canRead() && !file.isDirectory();
    }

    private static boolean fileExists(String file) {
        return fileExists(new File(file));
    }
    
    private static String problemName(String filename) { 
        return filename.substring(0, filename.length()-8);
    }
 
    private static String beforeName(String problem) { 
        return problem + "_t0.fits";
    }
 
    private static String afterName(String problem) { 
        return problem + "_t1.fits";
    }
    
    public static ProblemList listProblems() throws Exception { 
    
        if (!isConfigured()) { 
            throw new Exception("LocalConfig not configured!");
        }
     
        ArrayList<String> result = new ArrayList<String>();
        
        File dir = new File(dataDir);
        File [] files = dir.listFiles(new DataFilter());
        
        for (File f : files) { 
            
            String problem = problemName(f.getName());
     
            System.out.println("Potential problem set: " + problem + " ( " 
                    + f.getName() + ")");
            
            if (fileExists(dataDir + File.separator + afterName(problem))) {
                System.out.println("Add pair " + problem);
                result.add(problem);
            }
        }
        
        System.out.println("Returning new ProblemList");
   
        return new ProblemList(cluster, 
                result.toArray(new String[result.size()]));
    }

    public static Result compare(String input) throws Exception {
        
        if (!isConfigured()) { 
            throw new Exception("LocalConfig not configured!");
        }
        
        return new Result("test");
    }
    
}
