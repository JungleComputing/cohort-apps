package datachallenge.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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

    private void readReply(DataInputStream in, byte [] buffer, File dir) { 

        BufferedOutputStream fout = null;

        try { 
            
            byte opcode = in.readByte();
            String file = in.readUTF();

            if (opcode == FileServer.REPLY_ERROR) {
                System.err.println("Failed to retrieve file! " + file);
                return;
            }

            if (opcode != FileServer.REPLY_FILE) { 
                System.err.println("Unknown reply! " + opcode);
                return;
            }

            File target = new File(dir.getCanonicalFile() + File.separator + file);

            fout = new BufferedOutputStream(new FileOutputStream(target));

            long total = in.readLong();
            long bytes = 0;

            while (bytes < total) { 
                
                int size = FileServer.BUFFER_SIZE;
                
                if (total-bytes < FileServer.BUFFER_SIZE) { 
                    size = (int) (total-bytes);
                } 
                
                int read = in.read(buffer, 0, size);
              
                if (read == -1) { 
                    System.err.println("Unexpected end of stream!");
                    return;
                }

                fout.write(buffer, 0, read);
                bytes += read;
            } 

            fout.close();

        } catch (Exception e) {
            System.err.println("Failed to read file! "+ e);
            e.printStackTrace(System.err);
        } finally { 
            try { 
                fout.close();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    public void get(String [] files, File dir) throws Exception { 

        byte [] buffer = new byte[FileServer.BUFFER_SIZE];

        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        BufferedOutputStream fout = null;

        try { 
            s = factory.createClientSocket(serverAddress, 10000, true, null);
            s.setSoTimeout(600000);
            s.setTcpNoDelay(true);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            if (files.length == 1) { 
                out.writeByte(FileServer.OPCODE_GET);
                out.writeUTF(files[0]);
                out.flush();

                readReply(in, buffer, dir);
                
            } else { 
                out.writeByte(FileServer.OPCODE_MGET);
                out.writeInt(files.length);
                
                for (int i=0;i<files.length;i++) { 
                    out.writeUTF(files[i]);
                }
                out.flush();

                for (int i=0;i<files.length;i++) { 
                    readReply(in, buffer, dir);
                }            
            }

        } catch (Exception e) {
            
            System.out.println("Failed to retrieve files!");
            e.printStackTrace(System.err);

            throw new Exception("Failed to retrieve files! ", e);

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
                String [] files = new String [] { args[2] };
                File dir = new File(args[3]);
                client.get(files, dir);
            } else if (operation.equalsIgnoreCase("mget")) {
                
                String [] files = new String[args.length-3];
                
                for (int i=2;i<args.length-1;i++) { 
                    files[i-2] = args[i];
                }
                
                File local = new File(args[args.length-1]);
                client.get(files, local);
            }
         } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
