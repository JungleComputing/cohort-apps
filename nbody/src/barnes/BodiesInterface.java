package barnes;

public interface BodiesInterface {
    public void updateBodies(BodyUpdates b, int iteration);

    // To be called after the last iteration, instead of updateBodies.
    public void updateBodiesLocally(BodyUpdates b, int iteration);

    public BodyTreeNode getRoot();
    
    // clean the body tree
    public void cleanup(int dummy); // TODO remove dummy
}
