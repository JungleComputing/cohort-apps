package barnes;

import ibis.constellation.Activity;
import ibis.constellation.ActivityContext;
import ibis.constellation.ActivityIdentifier;
import ibis.constellation.Event;
import ibis.constellation.context.UnitActivityContext;

public class BarnesJob extends Activity {

    private static final long serialVersionUID = 4702819888780508196L;

    private ActivityIdentifier parent;
    
    private final BodyTreeNode me;
    private final BodyTreeNode tree;
    private final RunParameters params;
    
    private BodyUpdates result;    

    private BodyUpdates [] subResults;
    private int count;
    
    protected BarnesJob(ActivityContext context, ActivityIdentifier parent, 
            BodyTreeNode me, BodyTreeNode tree, RunParameters params) {
        super(context, true);
        
        this.parent = parent;
        this.me = me;
        this.tree = tree;
        this.params = params;
    }

    // Just a complicated way of doing a 'new' ;-)
    private BodyUpdates getBodyUpdates(int n, RunParameters params) {
        if (params.USE_DOUBLE_UPDATES) {
            return new BodyUpdatesDouble(n);
        }
        return new BodyUpdatesFloat(n);
    }
    
    private void sequential() { 
        // leaf node, let barnesSequential handle this
        result = getBodyUpdates(me.bodyCount, params);
        me.barnesSequential(tree, result, params);
        finish();
    }
    
    private void parallel() { 
     
        int childcount = 0;

        for (int i = 0; i < 8; i++) {
            if (me.children[i] != null) {
                childcount++;
            }
        }
        
        result = getBodyUpdates(0, params);
        
        subResults = new BodyUpdates[childcount];
        
        childcount = 0;

        for (int i = 0; i < 8; i++) {
            BodyTreeNode ch = me.children[i];
            
            if (ch != null) {
                //necessaryTree creation
                BodyTreeNode necessaryTree = 
                        (params.IMPLEMENTATION == RunParameters.IMPL_FULLTREE 
                                || ch == tree) 
                                ? tree : new BodyTreeNode(tree, ch);
               
                // FIXME: maybe use ch.bodyCount * necessaryTree.bodyCount as rank ?
                executor.submit(new BarnesJob(new UnitActivityContext("DEFAULT", ch.bodyCount), 
                		identifier(), ch, necessaryTree, params));
            }
        }

        suspend();        
    }
    
    @Override
    public void initialize() throws Exception {
        
        if (me.children == null || me.bodyCount < params.THRESHOLD) {
            sequential();
        } else { 
            parallel();
        }
    }

    @Override
    public void process(Event e) throws Exception {
       
       subResults[count++] = (BodyUpdates) e.data;
        
       if (count == subResults.length) { 
           result = result.combineResults(subResults);
           finish();
       } else { 
           suspend();
       }
    }
    
    @Override
    public void cleanup() throws Exception {
        executor.send(new Event(identifier(), parent, result));
    }

    @Override
    public void cancel() throws Exception {
        // TODO Auto-generated method stub
    }
}
