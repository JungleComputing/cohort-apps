package datachallenge.wf;

import java.io.Serializable;

public class ScriptResult implements Serializable {

    private static final long serialVersionUID = -5076870361794190850L;
    
    public final String message; 
    
    public final String [] command; 
    public final byte [] out; 
    public final byte [] err;
    public final int exit;
    
    public final int id;
    
    public final long time;
    
    public ScriptResult(String message, int id, long time) {
    	this.message = message;
    	this.id = id;
    	this.time = time;
    	
    	this.command = null;
    	this.out = null;
    	this.err = null;
    	this.exit = -1;    	
    }

    public ScriptResult(String message, int id, long time, int exit) {
    	this.message = message;
    	this.id = id;
    	this.time = time;
    	
    	this.command = null;
    	this.out = null;
    	this.err = null;
    	this.exit = exit;    	
    }

    
    public ScriptResult(String message, String [] command, byte[] out, byte[] err, int exit, 
            long time, int id) {

    	this.message = message;
    	
        this.command = command;
        this.out = out;
        this.err = err;
        this.exit = exit;
        this.time = time;
        this.id = id;
    }
    
    public String toString() { 
    	
    	StringBuilder tmp = new StringBuilder();
    	
    	tmp.append("SCRIPT " + id + " exit " + exit + " time " + time);
    	
    	if (message != null) { 
    		tmp.append(message); 
    		tmp.append(" ");
    	} 
    	
    	if (command != null) { 
    		for (String s: command) { 
    			tmp.append(s);
    			tmp.append(" ");
    		}
    	}
    			
    	if (out != null) { 
    		tmp.append("STDOUT: ");
    		tmp.append(new String(out));
    		tmp.append(" ");
    	}
    	
    	if (err != null) { 
    		tmp.append("STDERR: ");
    		tmp.append(new String(err));
    		tmp.append(" ");
    	}
    	
    	return tmp.toString();
    }
    
}
