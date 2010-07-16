package datachallenge.simple;

import ibis.cohort.ActivityIdentifier;
import ibis.cohort.Context;
import ibis.cohort.SimpleActivity;

public class CompareJob extends SimpleActivity {

    private static final long serialVersionUID = -653442064273941414L;
    
    private final String input;
    
    public CompareJob(ActivityIdentifier parent, Context c, String input) {
        super(parent, c);
        this.input = input;
    }

    @Override
    public void simpleActivity() throws Exception {
        
        System.out.println("Processing " + input);
        
        try { 
            Thread.sleep(1000);
        } catch (Exception e) {
            // ignored
        }
        
        cohort.send(identifier(), parent, LocalConfig.compare(input));
    }
}
