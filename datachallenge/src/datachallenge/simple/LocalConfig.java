package datachallenge.simple;

import ibis.util.RunProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LocalConfig {

	public static final int NO_CONTEXT       = 0;
	public static final int LOCATION_CONTEXT = 1;
	public static final int SIZE_CONTEXT     = 2;
	
	private static int contextType;
	private static boolean sorted;
	
    private static final String DEFAULT_EXEC = "dach.sh";
    private static final String DEFAULT_TMP = "/tmp";
    private static final String DEFAULT_CONFIGURATION = "location_and_sorted";
    
    private static String configuration;
    
    private static boolean configured = false;
    
    private static boolean isMaster = false;
    
    private static boolean useHTTPFileServer = false;
    private static String httpServer;
    private static String problemList;
    private static String httpCpExec = "/usr/bin/wget";

    /*
    private static boolean useFileServer = false;
    private static FileClient fileClient;
    */
    
    private static String cluster; 
    private static String [] clusters; 
    
    private static String dataDir; 
    private static String execDir; 
    private static String tmpDir = DEFAULT_TMP; 
    private static String exec = DEFAULT_EXEC; 
    
    private static String executable; 

    private static String cpExec = "/bin/cp";
    
    public static class Size { 
    	
    	public final String name;
    	public final long from; 
    	public final long to;
    	
		public Size(String name, long from, long to) {
			super();
			this.name = name;
			this.from = from;
			this.to = to;
		}  
    }

    private static ArrayList<Size> sizes; 
    
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
            } else if (tmp.equalsIgnoreCase("-configuration")) { 
                configuration = args[++i];
            } else if (tmp.equalsIgnoreCase("-master")) { 
                isMaster = true;
                
            } else if (tmp.equalsIgnoreCase("-size")) { 
                String name = args[++i];
                long start = Long.parseLong(args[++i]);
                long end = Long.parseLong(args[++i]);
            
                if (end == -1) { 
                	end = Long.MAX_VALUE;
                }
                
                Size s = new Size(name, start, end);
                
                if (sizes == null) { 
                	sizes = new ArrayList<LocalConfig.Size>();
                }
                
                sizes.add(s);
                
                /*
            } else if (tmp.equalsIgnoreCase("-fileserver")) { 
                useFileServer = true;
                fileClient = new FileClient(args[++i]);
                */
            } else if (tmp.equalsIgnoreCase("-httpserver")) { 
                useHTTPFileServer = true;
                httpServer = args[++i];
                problemList = args[++i];
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
       
        sorted = false;
        contextType = LOCATION_CONTEXT;
        
        if (configuration == null) {
        	sorted = true;
        	contextType = LOCATION_CONTEXT;
        } else if (configuration.equals("random")) {
        	sorted = false;
        	contextType = NO_CONTEXT;
        } else if (configuration.equals("sorted")) {
        	sorted = true;
        	contextType = NO_CONTEXT;
        } else if (configuration.equals("location_and_sorted")) {
        	sorted = true;
        	contextType = LOCATION_CONTEXT;
        } else if (configuration.equals("location")) {
        	sorted = false;
        	contextType = LOCATION_CONTEXT;
        } else if (configuration.equals("size")) {
        	sorted = false;
        	contextType = SIZE_CONTEXT;
        	
        	if (sizes == null) { 
        		throw new Exception("Selected size context without defining sizes!");
        	}        	
        } else { 
        	throw new Exception("Unknown configuration: " + configuration);
        }
        
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

    public static int getContextConfiguration() {
        return contextType;
    }
    
    public static boolean getSortedConfiguration() {
        return sorted;
    }
    
    public static Size [] getSizes() {
    	if (sizes == null) { 
    		return new Size[0];
    	}
    	
    	return sizes.toArray(new Size[sizes.size()]);
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
    
    private static Problem parseLine(String line) { 
    	
    	StringTokenizer tok = new StringTokenizer(line);
    	
    	try { 
    		String id = tok.nextToken();
    		String beforeFile = tok.nextToken();
    		long beforeSize = Long.parseLong(tok.nextToken());
    		
    		String afterFile = tok.nextToken();
    		long afterSize = Long.parseLong(tok.nextToken());
    		
    		return new Problem(id, beforeFile, beforeSize, afterFile, afterSize);
    	
    	} catch (Exception e) {
    		System.out.println("Failed to parse problem line: " + line + " " + e);
    		e.printStackTrace();
    		return null;
    	}
    }
    
    private static ProblemList parseProblemList(String problemFile) { 
    	
    	ProblemList list = new ProblemList(cluster, httpServer);
    	
    	try { 
    		BufferedReader r = new BufferedReader(new FileReader(new File(problemFile)));
    	
    		String line = r.readLine();
    		
    		while (line != null && line.length() > 0) { 
    			
    			Problem p = parseLine(line);
    			
    			if (p != null) { 
    				list.addProblem(p);
    			}
    			
    			line = r.readLine();
    		}
    		
    	} catch (Exception e) {
    	
    		System.out.println("Failed to parse problem list file: " + problemFile + " " + e);
    		e.printStackTrace();

    		list.addException(e);
    	}
    	
    	return list;
    }
    
    public static ProblemList listProblems() throws Exception { 

        if (!isConfigured()) { 
            throw new Exception("LocalConfig not configured!");
        }
        
        /*
        if (useFileServer) { 
         
            FileInfo [] info = fileClient.list();
            
            HashMap<String, FileInfo> set = new HashMap<String, FileInfo>();
            
            for (FileInfo f : info) { 

                String problem = problemName(f.filename);

                System.out.println("* Potential problem set: " + problem + " ( " 
                        + f.filename + ")");

                FileInfo other = set.remove(problem);
                
                if (other != null) { 
                    System.out.println("Add pair " + problem + " " + other.size + " " + f.size);
                    result.add(problem);
                    sizes.add(other.size + f.size);                    
                } else { 
                    set.put(problem, f);
                }
            }
            
        } else*/
        if (useHTTPFileServer) {
        	
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            int exit = run(new String [] { httpCpExec, httpServer + "/" + problemList, "-O", tmpDir + File.separator + "problemList" }, stdout, stderr); 

            if (exit != 0) {
                System.out.println("Failed to retrieve problem list " + httpServer + "/" + problemList   
                		+ " (stdout: " + stdout + ") (stderr: " + stderr + ")\n");
            }

            return parseProblemList(tmpDir + File.separator + "problemList");
            
        } else { 
         
        	ProblemList list = new ProblemList(cluster, null);
        	
            File dir = new File(dataDir);
            File [] files = dir.listFiles(new DataFilter());

            for (File before : files) { 

                String problem = problemName(before.getName());

                System.out.println("Potential problem set: " + problem + " ( " 
                        + before.getName() + ")");

                File after = new File(dataDir + File.separator + afterName(problem));
                
                if (fileExists(after)) {
                    
                	System.out.println("Add pair " + problem);
                    
                    Problem p = new Problem(problem, before.getName(), before.length(), after.getName(), after.length());
                    
                    list.addProblem(p);
                }
            }
            
            return list;
        }
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

    /*
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
    */
    

    private static boolean remoteCopy(String remoteFile, String localFile) 
    	throws Exception {
    
    	long start = System.currentTimeMillis();

    	String uri = httpServer + "/" + remoteFile;
    	
    	System.out.println("Copying remote file " + uri
    			+ " to local file "  + localFile);

    	StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        int exit = run(new String [] { httpCpExec, uri, "-O", localFile }, stdout, stderr); 

        if (exit != 0) {
            System.out.println("Failed to remotely copy file " +  uri
                + " (stdout: " + stdout + ") (stderr: " + stderr + ")\n");
            return false;
        }
    	
    	long end = System.currentTimeMillis();

    	System.out.println("Remote copying " +  uri
    			+ " took " + (end-start) + " ms.");

    	return true;            
    }

    private static boolean remoteCopy(String server, String remoteFile, String localFile) { 
	
    	long start = System.currentTimeMillis();

    	String uri = server + "/" + remoteFile;
	
    	System.out.println("Copying remote file " + uri + " to local file "  + localFile);

    	StringBuilder stdout = new StringBuilder();
    	StringBuilder stderr = new StringBuilder();

    	int exit = run(new String [] { httpCpExec, uri, "-O", localFile }, stdout, stderr); 

    	if (exit != 0) {
    		System.out.println("Failed to remotely copy file " +  uri
    				+ " (stdout: " + stdout + ") (stderr: " + stderr + ")\n");
    		return false;
    	}
    	
    	long end = System.currentTimeMillis();

    	System.out.println("Remote copying " +  uri
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
            
            /*
            if (useFileServer) {
                
                String [] files = new String [] { beforeName(input), afterName(input) }; 
                
                if (!remoteCopy(files, tmpDir)) { 
                    throw new Exception("Failed to copy files: " 
                            + Arrays.toString(files) + " to " + tmpDir);
                }
            } else */
            
            if (useHTTPFileServer) { 
            
            	if (!remoteCopy(before, tmpBefore)) { 
                    throw new Exception("Failed to remote copy before file: " + before 
                            + " to " + tmpBefore);
                }

                if (!remoteCopy(after, tmpAfter)) {
                    throw new Exception("Failed to remote copy after file: " + after 
                            + " to " + tmpAfter);
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

    public static Result compare(Job job) {

    	long start = System.currentTimeMillis();
        
    	if (useHTTPFileServer) { 
    		return compareHTTPJob(job);
    	} else { 
    		// NOTE: this only works is the machines only gets local jobs! 
    		return compare(problemName(job.beforeFileName));
    	}
    }

    private static Result compareHTTPJob(Job job) { 
        	
    	if (!isConfigured()) {
        	return new Result(job.ID, cluster, new Exception("LocalConfig not configured!"));
        }
        
    	if (job.servers.size() == 0) { 
    		return new Result(job.ID, cluster, new Exception("No servers found!"));
        }
        
    	long start = System.currentTimeMillis();
        
    	String tmpBefore = tmpDir + File.separator + beforeName(job.ID);
        String tmpAfter = tmpDir + File.separator + afterName(job.ID);

        // Randomize the order of the servers, but ensure our local server is 
        // tried first (provided it is on the list).
        ArrayList<String> servers = job.servers;
        
        if (servers.size() > 1) {
        	
        	boolean containsLocalServer = servers.contains(httpServer);
        	
        	if (containsLocalServer) {
        		servers.remove(httpServer);
        	}
        	
        	if (servers.size() > 1) { 
        		Collections.shuffle(servers);
        	}
        	
        	if (containsLocalServer) { 
        		servers.add(0, httpServer); 
        	}	
        } 
        
        int index = 0;
        boolean success = false;
        
        while (!success && index < servers.size()) { 
        	String server = servers.get(index++);
        	success = remoteCopy(server, job.beforeFileName, tmpBefore);
        } 

        if (!success) { 
        	return new Result(job.ID, cluster, new Exception("Failed to copy " + job.beforeFileName + " from any of the available servers!"));
        }
            	
        index = 0;
        success = false;
        
        while (!success && index < servers.size()) { 
        	String server = servers.get(index++);
        	success = remoteCopy(server, job.afterFileName, tmpAfter);
        } 

        if (!success) { 
        	return new Result(job.ID, cluster, new Exception("Failed to copy " + job.afterFileName + " from any of the available servers!"));
        }

        long copy = System.currentTimeMillis();
            
        RunProcess p = new RunProcess(new String [] { executable, 
        		"-w", tmpDir, tmpBefore, tmpAfter });
            	
        p.run();

        byte [] out = p.getStdout();
        String err = new String(p.getStderr());
        
        int exit = p.getExitStatus();           
        
        if (exit != 0) {
        	return new Result(job.ID, cluster, new Exception("Failed to run comparison: (stderr: " + err + ")\n"));
        }

        long end = System.currentTimeMillis();
        
        System.out.println("Processing " + job.ID + " took " + (end-copy) + " ms.");
        System.out.println("Total time " + job.ID + " is " + (end-start) + " ms.");
            
        System.out.println("Output on stderr:\n " + err);
        
        return new Result(job.ID, cluster, copy-start, end-copy, out);
    }

    
}
