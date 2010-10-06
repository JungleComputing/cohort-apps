package datachallenge.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ProcessCPU {

    static class Filter implements FileFilter {

        private final String prefix;
        
        public Filter(String prefix) { 
            this.prefix = prefix;
        }
        
        public boolean accept(File f) {

            if (f.isDirectory()) { 
                return false;
            }

            if (!f.canRead()) { 
                return false;
            }

            return f.getName().startsWith(prefix);
        } 
    }
 
    static double [] usedCores; 
    
    static int prevTime; 
    
    private static void processLine(String line) {
        
        StringTokenizer tok = new StringTokenizer(line);
        
        if (tok.countTokens() != 2) { 
            System.err.println("Parse error in line: " + line);
            return;
        }
        
        int time = (int) Math.ceil((Long.parseLong(tok.nextToken()) / 1000.0));
        double used = Double.parseDouble(tok.nextToken());
        
        for (int i=prevTime;i<time;i++) {        
            usedCores[i] += used;
        }
        
        prevTime = time;
    }
        
    private static void processFile(File f) throws IOException { 
 
        BufferedReader r = new BufferedReader(new FileReader(f));
        
        String l = r.readLine();
        
        while (l != null) { 
            processLine(l);
            l = r.readLine();
        } 
        
        r.close();
    }
    
    public static void main(String [] args) throws IOException { 
        
        String dirname = args[0];
        String prefix = args[1];
        int maxTime = Integer.parseInt(args[2]);
        //int cores = Integer.parseInt(args[3]);
        
        File dir = new File(dirname);
        File [] files = dir.listFiles(new Filter(prefix));
        
        final int count = files.length;
        
        System.err.println("Found " + count + " input files");
        
        usedCores = new double[maxTime];
        
        for (int i=0;i<files.length;i++) { 
            
        	prevTime = 0;
        	
            if (files[i] != null) { 
                processFile(files[i]);
            } else { 
                System.err.println("EEP: File[" + i +"] = null");
            }
        }
        
        for (int i=0;i<maxTime;i++) { 
            
  //          double percent = (usedCores[i] * 100.0) / count;
//            System.out.printf("%d %.1f\n", i, percent);
            System.out.printf("%d %.1f\n", i, usedCores[i]);

        }
    }
}
