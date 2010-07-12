package nbia;

import java.io.Serializable;

public class Result implements Serializable {

    public static final int STATISTICS = 1;
    public static final int LBP        = 2;
    public static final int REDUCE     = 3;
    public static final int CLASSIFIER = 4;
    public static final int JOB        = 5;
    
    private final MetaData imageData;
    private final Object result;
    private final int type;
    
    public Result(MetaData imageData, Object result, int type) {
        this.imageData = imageData;
        this.result= result;
        this.type = type;
    }
    
    public final MetaData getMetaData() { 
        return imageData;
    }
    
    public final Object getResult() { 
        return result;
    }
    
    public final int getType() { 
        return type;
    }
}
