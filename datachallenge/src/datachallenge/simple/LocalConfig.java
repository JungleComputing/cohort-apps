package datachallenge.simple;

import ibis.util.RunProcess;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import datachallenge.util.FileClient;

public class LocalConfig {

    private static final String DEFAULT_EXEC = "dach.sh";
    private static final String DEFAULT_TMP = "/tmp";
    
    private static boolean configured = false;
    
    private static boolean isMaster = false;
    
    private static boolean useFileServer = false;
    private static FileClient fileClient;
    
    private static String cluster; 
    private static String [] clusters; 
    
    private static String dataDir; 
    private static String execDir; 
    private static String tmpDir = DEFAULT_TMP; 
    private static String exec = DEFAULT_EXEC; 
    
    private static String executable; 

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

    public static synchronized void configure(String [] args) throws Exception { 

        ArrayList<String> c = new ArrayList<String>();
            
        for (int i=0;i<args.length;i++) { 
            
            String tmp = args[i];
                
            if (tmp.equalsIgnoreCase("-dataDir")) { 
                dataDir = args[++i];
            } else if (tmp.equalsIgnoreCase("-execDir")) { 
                execDir = args[++i];
            } else if (tmp.equalsIgnoreCase("-tmpDir")) { 
                tmpDir = args[++i];
            } else if (tmp.equalsIgnoreCase("-exec")) { 
                exec = args[++i];
            } else if (tmp.equalsIgnoreCase("-cluster")) { 
                cluster = args[++i];
            } else if (tmp.equalsIgnoreCase("-master")) { 
                isMaster = true;
            } else if (tmp.equalsIgnoreCase("-fileserver")) { 
                useFileServer = true;
                fileClient = new FileClient(args[++i]);
            } else if (tmp.equalsIgnoreCase("-clusters")) { 
                 
                while (i+1 < args.length && !args[i+1].startsWith("-")) { 
                    c.add(args[++i]);
                }
            }
        }
        if (dataDir == null) { 
            throw new Exception("Data directory not set!");
        }

        if (execDir == null) { 
            throw new Exception("Exec directory not set!");
        }

        if (tmpDir == null) { 
            throw new Exception("Exec directory not set!");
        }

        if (cluster == null) { 
            throw new Exception("Cluster name not set!");
        }

        if (c.size() == 0) { 
            throw new Exception("Cluster list set!");
        }

        executable = execDir + File.separator + exec;

        if (!fileExists(executable)) { 
            throw new Exception("Executable not found: " + executable);
        }

        clusters = c.toArray(new String [c.size()]);
        
        configured = true;
    }
    
    /*
    public static synchronized void configure(String clusterName, String dataD, 
            String execD, String tmpD, String execN) throws Exception { 

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

        executable = execDir + File.separator + execN;

        if (!fileExists(executable)) { 
            throw new Exception("Executable not found: " + executable);
        }

        configured = true;
    }
     */
    
    public static synchronized boolean isConfigured() {
        return configured;
    }

    /*
    private static boolean directoryExists(File dir) { 
        return dir.exists() && dir.canRead() && dir.isDirectory();
    }

    private static boolean directoryExists(String dir) {
        return directoryExists(new File(dir));
    }
    */

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
        ArrayList<Long> sizes = new ArrayList<Long>();
        
        File dir = new File(dataDir);
        File [] files = dir.listFiles(new DataFilter());

        for (File f : files) { 

            String problem = problemName(f.getName());

            System.out.println("Potential problem set: " + problem + " ( " 
                    + f.getName() + ")");

            if (fileExists(dataDir + File.separator + afterName(problem))) {
                System.out.println("Add pair " + problem);
                result.add(problem);
                sizes.add(f.length());
            }
        }

        System.out.println("Returning new ProblemList");

        long [] tmp = new long[sizes.size()];
        
        for (int i=0;i<sizes.size();i++) { 
            tmp[i] = sizes.get(i);
        }
        
        return new ProblemList(cluster, 
                result.toArray(new String[result.size()]), tmp);
    }

    public static int run(String [] cmd, StringBuilder out, StringBuilder err) { 

        RunProcess p = new RunProcess(cmd);
        p.run();

        out.append(new String(p.getStdout()));
        err.append(new String(p.getStderr()));

        return p.getExitStatus();               
    }
    
    private static boolean localCopy(String path, String localFile) { 

        long start = System.currentTimeMillis();

        System.out.println("Copying local file: " + path + " to local file " + localFile);

        if (!fileExists(path)) { 
            System.out.println("Input file " + path + " not found\n");
            return false;
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        int exit = run(new String [] { cpExec, path, localFile }, stdout, stderr); 

        if (exit != 0) {
            System.out.println("Failed to locally copy file " + path 
                + " (stdout: " + stdout + ") (stderr: " + stderr + ")\n");
            return false;
        }

        long end = System.currentTimeMillis();

        System.out.println("Local copying " + path + " took " + (end-start) 
                + " ms.");

        return true;            
    }

    private static boolean remoteCopy(String [] remoteFiles, String localDir) 
        throws Exception {
        
        long start = System.currentTimeMillis();

        System.out.println("Copying remote files: " 
                + Arrays.toString(remoteFiles) 
                + " to local dir "  + localDir);

        fileClient.get(remoteFiles, new File(localDir));

        long end = System.currentTimeMillis();

        System.out.println("Remote copying " + Arrays.toString(remoteFiles) 
                 + " took " + (end-start) + " ms.");

        return true;            
    }

    
    public static String cluster() { 
        return cluster;
    }
    
    public static boolean isMaster() { 
        return isMaster;
    }
   
    public static String [] getClusters() { 
        return clusters;
    }
    
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
            
            if (useFileServer) {
                
                String [] files = new String [] { before, after }; 
                
                if (!remoteCopy(files, tmpDir)) { 
                    throw new Exception("Failed to copy files: " 
                            + Arrays.toString(files) + " to " + tmpDir);
                }
            } else { 
                
                if (!localCopy(before, tmpBefore)) { 
                    throw new Exception("Failed to copy before file: " + before 
                            + " to " + tmpBefore);
                }

                if (!localCopy(after, tmpAfter)) {
                    throw new Exception("Failed to copy after file: " + after 
                            + " to " + tmpAfter);
                }
            }
           
            long copy = System.currentTimeMillis();
            
            RunProcess p = new RunProcess(new String [] { executable, 
                    "-w", tmpDir, tmpBefore, tmpAfter });
            p.run();

            byte [] out = p.getStdout();
            String err = new String(p.getStderr());

            int exit = p.getExitStatus();           

            if (exit != 0) {
                throw new Exception("Failed to run comparison: (stderr: " + err 
                        + ")\n");
            }

            long end = System.currentTimeMillis();
        
            System.out.println("Processing " + input + " took " + (end-copy) 
                    + " ms.");
            System.out.println("Total time " + input + " is " + (end-start) 
                    + " ms.");
            
            return new Result(input, cluster, copy-start, end-copy, out);
            
        } catch (Exception e) {
            return new Result(input, cluster, e);
        }
    }

}
