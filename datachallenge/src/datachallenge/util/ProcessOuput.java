package datachallenge.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

public class ProcessOuput {

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
 
    static int [] active; 
 
    private static void processLine(String line) {
        
        StringTokenizer tok = new StringTokenizer(line);
        
        if (tok.countTokens() != 9) { 
            System.err.println("Parse error in line: " + line);
            return;
        }
        
        tok.nextToken();
        tok.nextToken();
        
        int start = Integer.parseInt(tok.nextToken()) / 1000;
        
        tok.nextToken();
        
        int end = Integer.parseInt(tok.nextToken()) / 1000;
        
        for (int i=start;i<end;i++) { 
            active[i]++;
        }
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
        
        File dir = new File(dirname);
        File [] files = dir.listFiles(new Filter(prefix));
        
        int count = files.length;
        
        System.err.println("Found " + count + " input files");
        
        active = new int[maxTime];
        
        for (int i=0;i<files.length;i++) { 
            
            if (files[i] != null) { 
                processFile(files[i]);
            } else { 
                System.err.println("EEP: File[" + i +"] = null");
            }
        }
        
        for (int i=0;i<maxTime;i++) { 
            
            double percent = (active[i] * 100.0) / count;
            System.out.printf("%d %.1f\n", i, percent);
        }
    }
}
