package datachallenge.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;

public class FileServer {

    public static final int BUFFER_SIZE = 128*1024;
    
    public static final byte OPCODE_LIST = 22;
    public static final byte OPCODE_GET  = 33;
    
    public static final byte REPLY_LIST  = 42;
    public static final byte REPLY_FILE  = 53;
    public static final byte REPLY_ERROR = 65;
    
    static class Filter implements FileFilter {

        private final String postfix;

        public Filter(String postfix) { 
            this.postfix = postfix;
        }
        
        public boolean accept(File f) {

            if (f.isDirectory()) { 
                return false;
            }

            if (!f.canRead()) { 
                return false;
            }

            return f.getName().endsWith(postfix);
        } 
    }
    
    class WorkerThread extends Thread { 
 
        private byte [] buffer = new byte[BUFFER_SIZE]; 
        
        private void sendList(OutputStream out) throws IOException { 
            
            DataOutputStream dout = 
                new DataOutputStream(new BufferedOutputStream(out));
            
            File [] files = dir.listFiles(filter);
    
            dout.writeByte(REPLY_LIST);
            dout.writeInt(files.length);
            
            for (int i=0;i<files.length;i++) { 
                dout.writeUTF(files[i].getName());
                dout.writeLong(files[i].length());
            }
         
            dout.flush();
        }
        
        private void sendFile(OutputStream out, String filename) throws IOException { 
 
            // Unbuffered
            DataOutputStream dout = new DataOutputStream(out);
            
            File tmp = 
                new File(dir.getCanonicalFile() + File.separator + filename);
            
            if (!tmp.exists() || !tmp.canRead() || !tmp.isFile()) { 
                dout.writeByte(REPLY_ERROR);
                dout.flush();
                return;
            }
            
            long total = tmp.length();
            long bytes = 0;
            
            dout.writeByte(REPLY_FILE);
            dout.writeLong(total);
            
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmp));
            
            try { 
                while (bytes < total) { 

                    int read = in.read(buffer, 0, BUFFER_SIZE);

                    if (read == -1) { 
                        System.err.println("Unexpected end of file: " + tmp.getPath());
                        return;
                    }

                    dout.write(buffer, 0, read);
                    bytes += read;
                } 
            } catch (Exception e) {
                System.err.println("Failed to fully send " + tmp.getPath());
            }       
        }
     
        private void handle() {
            
            VirtualSocket s = dequeue();
           
            DataInputStream din = null;
            OutputStream out = null;
            
            try { 
                s.setSoTimeout(1000);
                
                din = new DataInputStream(
                        new BufferedInputStream(s.getInputStream()));
                
                out = s.getOutputStream();
                
                int opcode = din.readByte();
                
                switch (opcode) {
                case OPCODE_GET: 
                    String filename = din.readUTF();
                    sendFile(out, filename);
                    break;
                    
                case OPCODE_LIST:
                    sendList(out);
                    break;
                    
                default:
                    System.err.println("Unkown opcode " + opcode);
                }
            } catch (Exception e) { 
                System.err.println("Connection failed " + e);
                e.printStackTrace();
            } finally { 
                try { 
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    // ignored
                }
                
                try { 
                    din.close();
                } catch (Exception e) {
                    // ignored
                }
                
                try {
                    s.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
        
        public void run() { 
            while (true) { 
                handle(); 
            }
        }
    }
    
    private final VirtualServerSocket ss;
    private final LinkedList<VirtualSocket> waiting = new LinkedList<VirtualSocket>();
    
    private final File dir;
    private final FileFilter filter;
    
    public FileServer(int port, int workers, File dir, FileFilter filter) 
        throws InitializationException, IOException { 
        
        this.dir = dir;
        this.filter = filter;
        
        VirtualSocketFactory vsf = VirtualSocketFactory.createSocketFactory();
        ss = vsf.createServerSocket(port, 64, null);
   
        for (int i=0;i<workers;i++) { 
            new WorkerThread().start();
        }
    }
    
    private boolean accept() {
        
        try { 
            VirtualSocket s = ss.accept();
            System.err.println("Accepted connection: " + s.getRemoteSocketAddress());
            enqueue(s);
            return true;
        } catch (Exception e) {
            System.err.println("ServerSocket failed: " + e);
            e.printStackTrace();
   
            try {
                ss.close();
            } catch (IOException ie) {
                // ignored
            }
        }
        
        return false;
    }
    
    public void run() { 
        while (accept());
    }
    
    synchronized void enqueue(VirtualSocket s) { 
        waiting.addLast(s);
        waiting.notifyAll();
    }
    
    synchronized VirtualSocket dequeue() { 
        
        while (waiting.size() == 0) { 
            try { 
                wait();
            } catch (InterruptedException e) {
                // ignore
            }
        }
        
        return waiting.removeFirst();
    }
    
    public static void main(String [] args) { 
        
        try {
            int port = Integer.parseInt(args[0]);
            int workers = Integer.parseInt(args[1]);
            
            File dir = new File(args[2]);
            
            if (!dir.exists() || !dir.canRead() || !dir.isDirectory()) { 
                System.err.println("Invalid dir: " + args[2]);
                System.exit(1);
            }
            
            Filter f = new Filter(args[3]);
            
            FileServer server = new FileServer(port, workers, dir, f);
            server.run();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}