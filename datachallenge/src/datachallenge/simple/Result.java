package datachallenge.simple;

import java.io.Serializable;

public class Result implements Serializable {

    private static final long serialVersionUID = 2306611001688349402L;
 
    public final String data;

    public Result(final String data) {
        super();
        this.data = data;
    }
}
