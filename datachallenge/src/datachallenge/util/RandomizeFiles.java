package datachallenge.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

public class RandomizeFiles {

    // Reads a list of file pairs and picks a random set of a specific size
    public static void main(String [] args) throws IOException { 
        
        ArrayList<String> names = new ArrayList<String>();
        
        String file = args[0];
       
        BufferedReader in = new BufferedReader(new FileReader(file));

        String line = in.readLine();

        while (line != null) { 

            // Only insert 'before'
            if (line.endsWith("_t0.fits")) { 
                names.add(line.substring(0, line.length()-8));
            }

            line = in.readLine();
        }

        in.close();

        Random r = new Random();
        
        for (int a=1;a<args.length;a+=2) { 
  
            String name = args[a];
            int size = Integer.parseInt(args[a+1]);

            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(name)));
            
            for (int i=0;i<size;i++) { 

                if (names.size() == 0) { 
                    System.err.println("Ran out of options!");
                    return;
                }

                int index = r.nextInt(names.size());

                String n = names.remove(index);

                out.println(n + "_t0.fits");
                out.println(n + "_t1.fits");
            }
            
            out.close();
        }
    }
}

