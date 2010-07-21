package datachallenge.workflow;

import java.util.ArrayList;

import ibis.cohort.Activity;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.Event;
import ibis.cohort.MessageEvent;
import ibis.cohort.SimpleActivity;

public class CompareJob extends Activity {

    private static final int START       = 0;
    private static final int COPY        = 2;
    private static final int DETECT      = 3;
    private static final int MATCH       = 4;
    private static final int SUB         = 5;
    private static final int POSTPROCESS = 6;
    private static final int DONE        = 7;
    
    private static final long serialVersionUID = -653442064273941414L;
    
    private final ActivityIdentifier parent;
    private final String input;
    private final int split;
    
    private int state = START;
    
    private DetectResult detectBefore;
    private DetectResult detectAfter;
    
    private ArrayList<SubResult> sub;
   
    private String before;
    private String after;

    private String result;
    
    private String tmpDir;
    
    public CompareJob(ActivityIdentifier parent, Context c, String input, 
            int split) {
        super(c);
        this.parent = parent;
        this.input = input;
        this.split = split;
    }
    
    @Override
    public void cancel() throws Exception {
        // Not used 
    }

   
    @Override
    public void initialize() throws Exception {

        state = COPY;
     
        tmpDir = LocalConfig.generateTmp();
        
        before = LocalConfig.beforeName(input);
        after = LocalConfig.beforeName(input);
      
        before = LocalConfig.prepare(before, tmpDir);
        after = LocalConfig.prepare(after, tmpDir);
        
        state = DETECT;
        
        cohort.submit(new DetectJob(identifier(), tmpDir, before, 1000));
        cohort.submit(new DetectJob(identifier(), tmpDir, after, 1000));
        
        suspend();
    }

    private void handleDetectResult(DetectResult r) { 
    
        if (r.isBefore()) { 
            detectBefore = r;
        } else { 
            detectAfter = r;
        }
        
        if (detectBefore != null && detectAfter != null) {

            state = MATCH;
            
            LocalConfig.match(tmpDir, before, after);
        
            state = SUB;
            
            for (int i=0;i<split;i++) { 
                cohort.submit(new SubJob(identifier(), tmpDir, before, after, 
                        1000, i, split));
            }
        }
    }
    
    private void handleSubResult(SubResult r) { 
        
        sub.add(r);
        
        if (sub.size() == split) { 
            // all results are in
            state = POSTPROCESS;
            
            LocalConfig.postprocess(tmpDir, before, after);
            
            state = DONE;
        }
    }
    
    @Override
    public void process(Event e) throws Exception {

        switch (state) { 
        case DETECT:
            handleDetectResult((DetectResult) ((MessageEvent) e).message); 
            break;     
        case SUB:
            handleSubResult((SubResult) ((MessageEvent) e).message); 
            break;     
        } 
        
        if (state == DONE) { 
            finish();
        } else { 
            suspend();
        }
    }
    
    @Override
    public void cleanup() throws Exception {
        cohort.send(identifier(), parent, new Result(input, null, 0, 0, result));
    }
}
