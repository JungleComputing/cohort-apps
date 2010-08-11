package datachallenge.util;

import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

public class FileServer {

    public static final int BUFFER_SIZE = 128*1024;
    
    public static final byte OPCODE_LIST = 11;
    public static final byte OPCODE_GET  = 22;
    public static final byte OPCODE_MGET  = 33;
     
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
        
        private void sendList(DataOutputStream dout) throws IOException { 
            
            File [] files = dir.listFiles(filter);
    
            dout.writeByte(REPLY_LIST);
            dout.writeInt(files.length);
            
            for (int i=0;i<files.length;i++) { 
                dout.writeUTF(files[i].getName());
                dout.writeLong(files[i].length());
            }
         
            dout.flush();
        }
        
        private void sendFile(DataOutputStream dout, String filename) throws IOException { 
 
            long start = System.currentTimeMillis();
            
            File tmp = 
                new File(dir.getCanonicalFile() + File.separator + filename);
            
            if (!tmp.exists() || !tmp.canRead() || !tmp.isFile()) { 
                dout.writeByte(REPLY_ERROR);
                dout.writeUTF(filename);
                return;
            }
            
            long total = tmp.length();
            long bytes = 0;
            
            dout.writeByte(REPLY_FILE);
            dout.writeUTF(filename);
            dout.writeLong(total);
            
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmp));
            
            try { 
                while (bytes < total) { 

                    int size = BUFFER_SIZE;
                    
                    if (total-bytes < BUFFER_SIZE) { 
                        size = (int) (total-bytes);
                    } 
                    
                    int read = in.read(buffer, 0, size);

                    if (read == -1) { 
                        System.err.println("Unexpected end of file: " + tmp.getPath());
                        return;
                    }

                    dout.write(buffer, 0, read);
                    bytes += read;
                } 
            } catch (Exception e) {
                System.err.println("Failed to fully send " + tmp.getPath());
                e.printStackTrace();
            }
       
            dout.flush();
            in.close();
 
            long end = System.currentTimeMillis();
            
            double mbit = ((total * 8.0) / ((end-start)/1000.0)) / (1000.0*1000.0);   
            
            System.out.println("Send " + filename + " / " + total + " in " 
                    + (end-start) + " ms " + mbit + " Mbit/s");
        }
     
        private void handle() {
            
            VirtualSocket s = dequeue();
           
            long start = System.currentTimeMillis();
            
            DataInputStream din = null;
            DataOutputStream dout = null;
            
            try { 
                s.setSoTimeout(1000);
                s.setTcpNoDelay(true);
                
                din = new DataInputStream(
                        new BufferedInputStream(s.getInputStream()));
              
                dout = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream()));
          
                int opcode = din.readByte();
                
                long end = System.currentTimeMillis();
                
                switch (opcode) {
                case OPCODE_GET:
                    
                    String filename = din.readUTF();
                    sendFile(dout, filename);
                    break;
                
                case OPCODE_MGET: 
                    int count = din.readInt();
                   
                    String [] files = new String[count];
                    
                    for (int i=0;i<count;i++) { 
                        files[i] = din.readUTF();
                    }
                
                    for (int i=0;i<count;i++) { 
                        sendFile(dout, files[i]);
                    }
                    
                    break;
                    
                case OPCODE_LIST:
                    sendList(dout);
                    break;
                    
                default:
                    System.err.println("Unkown opcode " + opcode);
                }
            } catch (Exception e) { 
                System.err.println("Connection failed " + e);
                e.printStackTrace();
            } finally { 
                try { 
                    dout.flush();
                    dout.close();
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
   
        System.out.println("FileServer running on: " + ss);
        
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
        notifyAll();
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
