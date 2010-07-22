package datachallenge.workflow;

import java.io.Serializable;

public class ScriptResult implements Serializable {

    private static final long serialVersionUID = -5076870361794190850L;
    
    public final String [] command; 
    public final byte [] out; 
    public final byte [] err;
    public final int exit;

    public final long time;
    
    public ScriptResult(String [] command, byte[] out, byte[] err, int exit, 
            long time) {

        this.command = command;
        this.out = out;
        this.err = err;
        this.exit = exit;
        this.time = time;
    }
    
}
