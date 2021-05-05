//Laney Ching and Katherine Zhou

package hw3;

import java.util.ArrayList;
import java.util.Arrays;

import hw1.RelationalOperator;

public class LeafNode implements Node {

	public Entry[] entries;
	public int num;
	public Node parent;

	public LeafNode(int degree, Node parent) {
		entries = new Entry[degree];
		num = 0;
		this.parent = parent;
	}
	
	public int numEntries() {
		return num;
	}

	public ArrayList<Entry> getEntries() {
		ArrayList<Entry> e = new ArrayList<>();

		for (int i = 0; i < num; i++) {
			e.add(entries[i]);
		}
		return e;
	}

	public int getDegree() {
		return entries.length;
	}

	public boolean isLeafNode() {
		return true;
	}
	
	public Node getParent() {
		return parent;
	}
	
	public void setParent(Node parent) {
		this.parent = parent;
	}

	public boolean isFull() {
		return num == entries.length;
	}
	
	public void addEntry(Entry entry) {
		int i;
		for (i = 0; i < num; i++) {
			if (entries[i].getField().compare(RelationalOperator.GT, entry.getField())) {
				break;
			}
		}
		for (int j = num; j > i; j--) {
			entries[j] = entries[j - 1];
		}
		entries[i] = entry;
		num++;
	}

}