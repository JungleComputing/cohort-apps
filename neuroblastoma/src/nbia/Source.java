package nbia;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Cohort;
import ibis.cohort.CohortFactory;
import ibis.cohort.Event;
import ibis.cohort.FlexibleEventCollector;
import ibis.cohort.MessageEvent;
import ibis.imaging4j.Image;

import java.io.File;
import java.io.IOException;

public class Source {

    private static int TILE_W = 512;
    private static int TILE_H = 512;
    
    private static int INITIAL_SCALE = 8;
    
    private final FlexibleEventCollector events;
    private final ActivityIdentifier id;
    private final int maximumConcurrentJobs;
    private final OnDiskImage [] input;
    private final Cohort cohort; 
    
    public Source(File dir, int maxConcurrentJobs) throws Exception { 

        this.maximumConcurrentJobs = maxConcurrentJobs;
        
        // This machine contains the input files and will act as 
        // a source of image tiles, and a sink for the results.  

        if (!dir.isDirectory()) { 
            throw new Exception("Source expects input directory!");
        }

        InputFileCollector collector = new InputFileCollector(dir);

        input = collector.getInputFiles();

        if (input.length == 0) { 
            throw new Exception("No input file found!");
        }
        
        System.out.println("Maximum concurrent jobs = " + maxConcurrentJobs);
        
        cohort = CohortFactory.createCohort();
        cohort.activate();

        events = new FlexibleEventCollector();
        id = cohort.submit(events);
    } 

    private int handleResults(Event [] result, int pendingJobs) { 

        for (Event e: result) { 
            Result r = (Result) ((MessageEvent) e).message;
            System.out.println("Finished " + r.getMetaData().ID);
        }
        
        return pendingJobs - result.length;
    }
    
    private int handleResults(int pendingJobs) { 

        if (pendingJobs == maximumConcurrentJobs) {             
            pendingJobs = handleResults(events.waitForEvents(), pendingJobs);            
        }
        
        return pendingJobs;
    }
    
    private int sumbit(OnDiskImage image, int pendingJobs) throws IOException { 

        int tilesW = (int) (image.getWidth() / TILE_W);
        int tilesH = (int) (image.getHeight() / TILE_H);
        
        for (int h=0;h<tilesH;h++) { 
            for (int w=0;w<tilesW;w++) { 
            
                pendingJobs = handleResults(pendingJobs);
                
                Image tmp = image.getSubImage(w*TILE_W, h*TILE_H, TILE_W, TILE_H);
                
                cohort.submit(new Job(id, tmp, INITIAL_SCALE));
                
                pendingJobs++;
            }
        }
        
        return pendingJobs;
    }
    
    public void run() throws IOException { 
                
        int pendingJobs = 0;
        
        for (OnDiskImage image : input) { 
            pendingJobs = sumbit(image, pendingJobs); 
        }

        while (pendingJobs > 0) {
            pendingJobs = handleResults(events.waitForEvents(), pendingJobs);
        }
                
        cohort.done();
    }    
}
