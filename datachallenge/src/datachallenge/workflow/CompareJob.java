package datachallenge.workflow;

import ibis.cohort.Activity;
import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.Event;
import ibis.cohort.MessageEvent;

import java.io.File;
import java.util.ArrayList;

public class CompareJob extends Activity {

    private static final int START       = 0;
    private static final int COPY        = 2;
    private static final int DETECT      = 3;
    private static final int MATCH       = 4;
    private static final int SUB         = 5;
    private static final int POSTPROCESS = 6;
    
    private static final int DONE        = 7;
    
    private static final int ERROR       = 99;
    
    private static final long serialVersionUID = -653442064273941414L;
    
    private final ActivityIdentifier parent;
    private final String input;
    private final int split;
    
    private int state = START;
    
    // Gather all results...
    private ArrayList<ScriptResult> detect;
    private ScriptResult match;
    private ArrayList<ScriptResult> sub;
    private ScriptResult post;

    private String before;
    private String after;
    private String tmpDir;
    
    public CompareJob(ActivityIdentifier parent, Context c, String input, 
            int split) {
        super(c, true);
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

        detect = new ArrayList<ScriptResult>();
        sub = new ArrayList<ScriptResult>();
        
        state = COPY;
     
        File tmp = LocalConfig.generateTmpDir(input);
        
        tmpDir = tmp.getAbsolutePath();
        
        before = LocalConfig.beforeName(input);
        after = LocalConfig.afterName(input);
      
        // FIXME: copy these files in parallel!
        before = LocalConfig.prepare(before, tmpDir);
        after = LocalConfig.prepare(after, tmpDir);
        
        state = DETECT;
        
        cohort.submit(new DetectJob(identifier(), getContext(), tmpDir, before, 1000));
        cohort.submit(new DetectJob(identifier(), getContext(), tmpDir, after, 1000));
        
        suspend();
    }

    private void match() { 
        match = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript("stage2.sh"), tmpDir, before, after }); 
    } 
    
    private void postProcess() { 
        post = LocalConfig.runScript(new String [] { 
                LocalConfig.getScript("stage4.sh"), tmpDir, before, after }); 
    }

    
    private void handleDetectResult(ScriptResult r) { 
    
        detect.add(r);
        
        if (detect.size() == 2) { 

            if (detect.get(0).exit != 0 || detect.get(1).exit != 0) {
                state = ERROR;
                return;
            }
            
            state = MATCH;
            
            match();
     
            if (match.exit != 0) { 
                state = ERROR;
                return;
            }
            
            state = SUB;
            
            for (int i=0;i<split;i++) { 
                cohort.submit(new SubJob(identifier(), getContext(), tmpDir, 
                        before, after, 1000, i, split));
            }
        }
    }
    
    private void handleSubResult(ScriptResult r) { 
        
        sub.add(r);
        
        if (sub.size() == split) { 
            // all results are in
            
            for (ScriptResult s : sub) { 
                if (s.exit != 0) { 
                    state = ERROR;
                    return;
                }
            }
            
            state = POSTPROCESS;
            
            postProcess();
            
            if (post.exit != 0) { 
                state = ERROR;
                return;
            }
            
            state = DONE;
        }
    }
    
    @Override
    public void process(Event e) throws Exception {

        ScriptResult r = (ScriptResult) ((MessageEvent) e).message;
        
        switch (state) { 
        case DETECT:
            handleDetectResult(r); 
            break;     
        case SUB:
            handleSubResult(r); 
            break;     
        } 
        
        if (state == DONE || state == ERROR) { 
            finish();
        } else { 
            suspend();
        }
    }
    
    @Override
    public void cleanup() throws Exception {
        
        CompareResult result = new CompareResult(input, LocalConfig.cluster(), 
                split, detect, match, sub, post, state == DONE);
        
        cohort.send(identifier(), parent, result);
    }
}
