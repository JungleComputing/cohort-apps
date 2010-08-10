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

        if (success) { 
            return input  + " succesfully compared at " + cluster;
        } else { 
            StringBuilder tmp = new StringBuilder(input);
            tmp.append(" failed to compared at ");
            tmp.append(cluster);
            tmp.append("\n");
            
            for (ScriptResult r : detect) { 
                tmp.append("Detect stdout:\n");
                tmp.append(new String(r.out));
                tmp.append("Detect stderr:\n");
                tmp.append(new String(r.err));
            }

            tmp.append("Match stdout:\n");
            tmp.append(new String(match.out));
            tmp.append("Match stderr:\n");
            tmp.append(new String(match.err));
            
            for (ScriptResult r : sub) { 
                tmp.append("Sub stdout:\n");
                tmp.append(new String(r.out));
                tmp.append("Sub stderr:\n");
                tmp.append(new String(r.err));
            }
            
            tmp.append("Post stdout:\n");
            tmp.append(new String(match.out));
            tmp.append("Post stderr:\n");
            tmp.append(new String(match.err));
            
            tmp.append("\n");
            
            return tmp.toString();
        }
    }
}
