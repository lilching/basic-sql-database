//Laney Ching and Katherine Zhou

package hw3;

import java.util.ArrayList;

import hw1.Field;

public class InnerNode implements Node {

	public Node[] nodes;
	public Field[] keys;
	public int num;
	public Node parent;

	public InnerNode(int degree, Node parent) {
		nodes = new Node[degree];
		keys = new Field[degree - 1];
		num = 0;
		this.parent = parent;
	}

	public ArrayList<Field> getKeys() {
		ArrayList<Field> k = new ArrayList<>();

		for (int i = 0; i < num - 1; i++) {
			k.add(keys[i]);
		}
		return k;
	}

	public ArrayList<Node> getChildren() {
		ArrayList<Node> children = new ArrayList<>();

		for (int i = 0; i < num; i++) {
			children.add(nodes[i]);
		}
		return children;
	}

	public int getDegree() {
		return nodes.length;
	}

	public boolean isLeafNode() {
		return false;
	}
	
	public Node getParent() {
		return parent;
	}
	
	public void setParent(Node parent) {
		this.parent = parent;
	}

	public boolean isFull() {
		return num == nodes.length;
	}

}