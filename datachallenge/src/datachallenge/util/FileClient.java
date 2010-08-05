package datachallenge.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.UnknownHostException;

import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

public class FileClient {

    private final VirtualSocketAddress serverAddress;
    private final VirtualSocketFactory factory;
    
    public FileClient(String serverAddress) 
        throws InitializationException, UnknownHostException, 
            MalformedAddressException { 
        
        this.serverAddress = new VirtualSocketAddress(serverAddress);
        this.factory = VirtualSocketFactory.createSocketFactory();
    
    }
   
    public FileInfo [] list() { 
        
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        
        try { 
            s = factory.createClientSocket(serverAddress, 10000, true, null);
            s.setSoTimeout(60000);
            
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
           
            out.writeByte(FileServer.OPCODE_LIST);
            out.flush();
            
            byte opcode = in.readByte();
            
            if (opcode != FileServer.REPLY_LIST) { 
                System.err.println("Unknown reply! " + opcode);
                return null;
            }
            
            int len = in.readInt();
            
            FileInfo [] result = new FileInfo[len];
            
            for (int i=0;i<len;i++) { 
                String name = in.readUTF();
                long size = in.readLong();
                result[i] = new FileInfo(name, size);
            }
            
            return result;
        
        } catch (Exception e) {
      
            System.err.println("Failed to retrieve list! " + e);
            e.printStackTrace(System.err);
            return null;
            
        } finally { 
        
            try { 
                out.close();
            } catch (Exception e) {
                // ignored
            }
            
            try { 
                in.close();
            } catch (Exception e) {
                // ignored
            }
           
            try { 
                s.close();
            } catch (Exception e) {
                // ignored
            }
        } 
    }
    
    public void get(String filename, File target) { 

        byte [] buffer = new byte[FileServer.BUFFER_SIZE];
        
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        BufferedOutputStream fout = null;
        
        try { 
            fout = new BufferedOutputStream(new FileOutputStream(target));
            
            s = factory.createClientSocket(serverAddress, 10000, true, null);
            s.setSoTimeout(60000);
            
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
           
            out.writeByte(FileServer.OPCODE_GET);
            out.writeUTF(filename);
            out.flush();
            
            byte opcode = in.readByte();
            
            if (opcode == FileServer.REPLY_ERROR) { 
                System.err.println("Failed to retrieve file!");
                return;
            }
            
            if (opcode != FileServer.REPLY_FILE) { 
                System.err.println("Unknown reply! " + opcode);
                return;
            }
            
            long total = in.readLong();
            long bytes = 0;
            
            while (bytes < total) { 

                int read = in.read(buffer, 0, FileServer.BUFFER_SIZE);

                if (read == -1) { 
                    System.err.println("Unexpected end of stream!");
                    return;
                }

                fout.write(buffer, 0, read);
                bytes += read;
            } 

            fout.close();
            
        } catch (Exception e) {
      
            System.err.println("Failed to retrieve list! " + e);
            e.printStackTrace(System.err);
            
        } finally { 
    
            try { 
                fout.close();
            } catch (Exception e) {
                // ignored
            }
    
            try { 
                out.close();
            } catch (Exception e) {
                // ignored
            }
            
            try { 
                in.close();
            } catch (Exception e) {
                // ignored
            }
           
            try { 
                s.close();
            } catch (Exception e) {
                // ignored
            }
        } 

        
    }
    
    public static void main(String [] args) { 
        
        try {
            FileClient client = new FileClient(args[0]);
            
            String operation = args[1];
            
            if (operation.equalsIgnoreCase("list")) { 
                FileInfo [] info = client.list();
                
                if (info != null) { 
                    for (int i=0;i<info.length;i++) { 
                        System.out.println(info[i].filename + " " + info[i].size);
                    }
                }
            } else if (operation.equalsIgnoreCase("get")) { 
                String filename = args[2];
                File local = new File(args[3]);
                client.get(filename, local);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
