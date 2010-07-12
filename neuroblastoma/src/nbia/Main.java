package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Cohort;
import ibis.cohort.CohortFactory;
import ibis.cohort.FlexibleEventCollector;

import java.io.File;

public class Main {
    
    private static void worker() throws Exception { 
        
        // This machine will be a worker. We must assume there is a command 
        // line configuration of what the hardware has to offer.             
        Cohort cohort = CohortFactory.createCohort();
        cohort.activate();
        cohort.done();
    }

    public static void main(String [] args) {

        // Determine machine type 
        // Init cohort with right number of GPUS, CPUS, etc
        // Start role depending on machine type 

        String role = args[0];

        try { 
            if (role.equals("source")) {
                
                Source source = new Source(new File(args[1]), Integer.parseInt(args[2]));
                source.run();
                
            } else if (role.equals("worker")) { 
                worker();
            } else { 
                System.err.println("Unknown machine role: " + role);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
