package datachallenge.workflow;

import ibis.util.RunProcess;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

public class LocalConfig {

    private static boolean configured = false;

    private static String cluster; 
  
    private static String dataDir; 
    private static String execDir; 
    private static String tmpDir; 

    private static String executableStage1;
    private static String executableStage2;
    private static String executableStage3;
    private static String executableStage4;

    private static String cpExec = "/bin/cp";
    
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

    public static synchronized void configure(String clusterName, 
            String dataD, String execD, String tmpD) throws Exception { 

        if (configured) { 
            return;
        }

        cluster = clusterName;
        dataDir = dataD;
        execDir = execD;
        tmpDir = tmpD;

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

        if (tmpDir == null) { 
            throw new Exception("Tmp dir not specified!");
        }

        if (!directoryExists(tmpDir)) { 
            throw new Exception("Tmp dir not useable!");
        }

        executableStage1 = execDir + File.separator + "stage1.sh";
        executableStage2 = execDir + File.separator + "stage2.sh";
        executableStage3 = execDir + File.separator + "stage3.sh";
        executableStage4 = execDir + File.separator + "stage4.sh";
        
        if (!fileExists(executableStage1)) { 
            throw new Exception("Executable not found: " + executableStage1);
        }

        if (!fileExists(executableStage2)) { 
            throw new Exception("Executable not found: " + executableStage2);
        }
        
        if (!fileExists(executableStage3)) { 
            throw new Exception("Executable not found: " + executableStage3);
        }
        
        if (!fileExists(executableStage4)) { 
            throw new Exception("Executable not found: " + executableStage4);
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

    public static String beforeName(String problem) { 
        return problem + "_t0.fits";
    }

    public static String afterName(String problem) { 
        return problem + "_t1.fits";
    }

    public static ProblemList listProblems() throws Exception { 

        if (!isConfigured()) { 
            throw new Exception("LocalConfig not configured!");
        }

        System.out.println("Generating list of input files in " + dataDir);
        
        ArrayList<String> result = new ArrayList<String>();

        File dir = new File(dataDir);
        File [] files = dir.listFiles(new DataFilter());

        System.out.println("Potential problems: " + files.length);
        
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
        /**
         * 
         */
        
    }

    public static String cluster() { 
        return cluster;
    }
    
    public static String execDir() { 
        return execDir;
    }
    
    private static int run(String [] cmd, StringBuilder out, StringBuilder err) { 

        RunProcess p = new RunProcess(cmd);
        p.run();

        out.append(new String(p.getStdout()));
        err.append(new String(p.getStderr()));

        return p.getExitStatus();               
    }
    
    private static boolean localCopy(String src, String dst) { 

        long start = System.currentTimeMillis();

        System.out.println("Copying local file: " + src + " to local file " + dst);

        if (!fileExists(src)) { 
            System.out.println("Input file " + src + " not found\n");
            return false;
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        int exit = run(new String [] { cpExec, src, dst }, stdout, stderr); 

        if (exit != 0) {
            System.out.println("Failed to locally copy file " + src 
                + " (stdout: " + stdout + ") (stderr: " + stderr + ")\n");
            return false;
        }

        long end = System.currentTimeMillis();

        System.out.println("Local copying " + src + " took " + (end-start) 
                + " ms.");

        return true;            
    }

/*
    public static Result compare(String input) {

        long start = System.currentTimeMillis();
        
        String before = dataDir + File.separator + beforeName(input);
        String after = dataDir + File.separator + afterName(input);
        String tmpBefore = tmpDir + File.separator + beforeName(input);
        String tmpAfter = tmpDir + File.separator + afterName(input);

        try { 

            if (!isConfigured()) { 
                throw new Exception("LocalConfig not configured!");
            }

            if (!fileExists(before)) { 
                throw new Exception("Before file not found: " + before);
            }

            if (!fileExists(after)) { 
                throw new Exception("After file not found: " + after);
            }

            if (!localCopy(before, tmpBefore)) { 
                throw new Exception("Failed to copy before file: " + before 
                        + " to " + tmpBefore);
            }

            if (!localCopy(after, tmpAfter)) {
                throw new Exception("Failed to copy after file: " + after 
                        + " to " + tmpAfter);
            }
           
            long copy = System.currentTimeMillis();
            
            RunProcess p = new RunProcess(new String [] { executable, 
                    "-w", tmpDir, tmpBefore, tmpAfter });
            p.run();

            byte [] out = p.getStdout();
            String err = new String(p.getStderr());

            int exit = p.getExitStatus();           

            if (exit != 0) {
                throw new Exception("Failed to run comparison: (stderr: " + err + ")\n");
            }

            long end = System.currentTimeMillis();
        
            return new Result(input, cluster, copy-start, end-copy, out);
            
        } catch (Exception e) {
            return new Result(input, cluster, e);
        }
    }
     */
    

    public static ScriptResult runScript(String [] command) {
        
        long start = System.currentTimeMillis();
        
        RunProcess p = new RunProcess(command);
        p.run();

        byte [] out = p.getStdout();
        byte [] err = p.getStderr();

        int exit = p.getExitStatus();           

        long end = System.currentTimeMillis();
        
        return new ScriptResult(command, out, err, exit, end-start);
    }

    public static File generateTmpDir(String input) throws IOException {
        
        // Create a temp file, remember its name, and remove it again.
        File tmp = File.createTempFile("TMP" + input, "", new File(tmpDir));
        String name = tmp.getCanonicalPath();
        tmp.delete();
        
        // Create a dir with the same unique name
        File dir = new File(name);
        dir.mkdirs();
        return dir;
    }

    public static String prepare(String filename, String tmpDir) throws Exception {
        
        String src = dataDir + File.separator + filename;
        String dst = tmpDir + File.separator + filename;
        
        if (!localCopy(src, dst)) { 
            throw new Exception("Failed to copy file: " + src + " -> " + dst);
        }

        return dst;
    }
   
    public static String getScript(String script) {
        return execDir + File.separator + script;
    }
}
