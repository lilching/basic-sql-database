//Laney Ching and Katherine Zhou

package hw3;

public interface Node {

	public int getDegree();
	public boolean isLeafNode();
	public Node getParent();
	public void setParent(Node parent);
	public boolean isFull();
	
}
