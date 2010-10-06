package datachallenge.wf;

import java.io.Serializable;

public class Problem implements Serializable {

	private static final long serialVersionUID = 1148924333869274090L;

	public final String name;
	
	public final String beforeFileName;
	public final long beforeFileSize;
	
	public final String afterFileName;
	public final long afterFileSize;
	
	public Problem(String name, 
			String beforeFileName, long beforeFileSize,
			String afterFileName, long afterFileSize) { 
		
		super();
		this.name = name;
		this.beforeFileName = beforeFileName;
		this.beforeFileSize = beforeFileSize;
			
		this.afterFileName = afterFileName;
		this.afterFileSize = afterFileSize;
	}	
}
