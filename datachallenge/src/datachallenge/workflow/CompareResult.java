package datachallenge.workflow;

import java.io.Serializable;
import java.util.ArrayList;

public class CompareResult implements Serializable {

    private static final long serialVersionUID = 1098802414650670042L;
    
    public final String cluster;
    public final String input;
    public final int split;
    
    public ArrayList<ScriptResult> detect;
    public ScriptResult match;
    public ArrayList<ScriptResult> sub;
    public ScriptResult post;
    
    public final boolean success;
    
    public CompareResult(String input, String cluster, int split, 
            ArrayList<ScriptResult> detect, ScriptResult match, 
            ArrayList<ScriptResult> sub, ScriptResult post,
            boolean success) { 
        this.cluster = cluster;
        this.input = input;
        this.split = split;
        this.detect = detect;
        this.match = match;
        this.sub = sub;
        this.post = post;
        this.success = success;
    }
    
    public String toString() { 

        // TODO: improve this print!!!!
        if (success) { 
            return input  + " succesfully compared at " + cluster;
        } else { 
            return input  + " failed to compared at " + cluster;
        }
    }
}
