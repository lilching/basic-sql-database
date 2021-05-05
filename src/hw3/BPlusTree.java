//Laney Ching and Katherine Zhou
package hw3;

import hw1.Field;
import hw1.IntField;
import hw1.RelationalOperator;

public class BPlusTree {

	private int pInner;
	private int pLeaf;
	private Node root;

	public BPlusTree(int pInner, int pLeaf) {
		this.pInner = pInner;
		this.pLeaf = pLeaf;
		root = new LeafNode(pLeaf, null);
	}
	
	public int pLeafLower() {
		return (pLeaf + 1) / 2;
	}
	
	public int pInnerLower() {
		return (pInner + 1) / 2;
	}
	
	public int pInner() {
		return pInner;
	}
	
	public int pLeaf() {
		return pLeaf;
	}

	private LeafNode findLeaf(Node node, Field f) {
		if (node instanceof InnerNode) {
			InnerNode n = (InnerNode) node;
			for (int i = 0; i < n.num - 1; i++) {
				if (n.keys[i].compare(RelationalOperator.GTE, f)) {
					return findLeaf(n.nodes[i], f);
				}
			}
			return findLeaf(n.nodes[n.num - 1], f);
		}
		else {
			return (LeafNode) node;
		}
	}
	
	public LeafNode search(Field f) {
		LeafNode n = findLeaf(root, f);
		for (int i = 0; i < n.num; i++) {
			if (n.entries[i].getField().compare(RelationalOperator.EQ, f)) {
//				System.out.print(f + " in ");
//				print(n);
//				System.out.println();
				return n;
			}
		}
		return null;
	}

	public void insert(Entry e) {
		LeafNode toPut = findLeaf(root, e.getField());
		
		if (toPut.numEntries() < pLeaf) {
			toPut.addEntry(e);
		}
		else {
			// the leaf is full, so we have to split!
			Entry entries[] = new Entry[pLeaf + 1];
			int idx;
			for (idx = 0; idx < pLeaf && toPut.entries[idx].getField().compare(RelationalOperator.LT, e.getField()); idx++) {
				entries[idx] = toPut.entries[idx];
			}
			entries[idx] = e;
			for (; idx < pLeaf; idx++) {
				entries[idx + 1] = toPut.entries[idx];
			}
			
			LeafNode leftLeaf = new LeafNode(pLeaf, toPut.getParent());
			LeafNode rightLeaf = new LeafNode(pLeaf, toPut.getParent());
			
			int mid = (pLeaf + 2) / 2;
			for (int i = 0; i < mid; i++) {
				leftLeaf.addEntry(entries[i]);
			}
			for (int i = mid; i < entries.length; i++) {
				rightLeaf.addEntry(entries[i]);
			}
			// send the first element of the right node up to the parent
			Field sendUp = leftLeaf.entries[mid - 1].getField();
			// node is the original node before it was split
			Node node = toPut;
			// the left half of the split node 一 二 三 四 五 六 七 八 九 十 十一 十二 十三 十四 十五 十六 十八 十九 二十 
			Node left = leftLeaf;
			// the right half of the split node
			Node right = rightLeaf;
			
			while (true) {
				Node parentP = node.getParent();
				if (parentP == null) {
					// we have reached the root, split and make a new root
					InnerNode newRoot = new InnerNode(pInner, null);
					newRoot.nodes[0] = left;
					newRoot.nodes[1] = right;
					newRoot.keys[0] = sendUp;
					newRoot.num = 2;
					
					left.setParent(newRoot);
					right.setParent(newRoot);

					root = newRoot;
					break;
				}
				else {
					InnerNode parent = (InnerNode) parentP;
					// find the index this node is of the parent
					int index;
					for (index = 0; parent.nodes[index] != node; index++);
					
					if (parent.isFull()) {
						// create a new array of nodes + keys and insert the propagated nodes/keys
						// in sorted order
						Node nodes[] = new Node[pInner + 1];
						Field keys[] = new Field[pInner];
						for (int i = 0; i < index; i++) {
							nodes[i] = parent.nodes[i];
							keys[i] = parent.keys[i];
						}
						nodes[index] = left;
						nodes[index + 1] = right;
						keys[index] = sendUp;
						for (int i = index + 1; i < pInner; i++) {
							nodes[i + 1] = parent.nodes[i];
							keys[i] = parent.keys[i - 1];
						}

						// make new inner nodes for the split parent
						InnerNode leftP = new InnerNode(pInner, parent.getParent());
						InnerNode rightP = new InnerNode(pInner, parent.getParent());
						
						mid = nodes.length / 2;
						for (int i = 0; i < mid; i++) {
							leftP.nodes[i] = nodes[i];
							nodes[i].setParent(leftP);
							if (i < mid - 1) {
								leftP.keys[i] = keys[i];
							}
						}
						leftP.num = mid;
						
						for (int i = mid; i < nodes.length; i++) {
							rightP.nodes[i - mid] = nodes[i];
							nodes[i].setParent(rightP);
							if (i < nodes.length - 1) {
								rightP.keys[i - mid] = keys[i];
							}
						}
						rightP.num = nodes.length - mid;
						
						sendUp = keys[mid - 1];
						left = leftP;
						right = rightP;
						node = parent;
						continue;
					}
					else {
						// there is room in the parent for the new key, just put it in there
						for (int j = parent.num; j > index + 1; j--) {
							parent.nodes[j] = parent.nodes[j - 1];
							parent.keys[j - 1] = parent.keys[j - 2];
						}
						parent.keys[index] = sendUp;
						parent.nodes[index] = left;
						parent.nodes[index + 1] = right;
						parent.num++;
						left.setParent(parent);
						right.setParent(parent);
						break;
					}
				}
			}
		}
	}
	
	public static void print(InnerNode n) {
		System.out.print("[");
		for (int i = 0; i < n.num; i++) {
			print(n.nodes[i]);
			if (i < n.num - 1) {
				System.out.print(" <" + ((IntField) n.keys[i]).getValue() + "> ");
			}
		}
		System.out.print("]");
	}
	
	public static void print(LeafNode n) {
		System.out.print("{");
		for (int i = 0; i < n.num; i++) {
			System.out.print(((IntField) n.entries[i].getField()).getValue());
			if (i < n.num - 1) {
				System.out.print(", ");
			}
		}
		System.out.print("}");
	}
	
	public static void print(Node n) {
		if (n instanceof LeafNode) {
			print((LeafNode) n);
		}
		else {
			print((InnerNode) n);
		}
	}
	
	public void print() {
		if (root == null) {
			System.out.println("Empty");
		}
		else {
			print(root);
			System.out.println();
		}
	}
	
	private void printYml(InnerNode n, int lvl) {
		String spaces = "";
		for (int i = 0; i < lvl; i++) {
			spaces += "  ";
		}
	    System.out.print(spaces + (lvl == 0 ? "  keys: [" : "- keys: ["));
		for (int i = 0; i < n.num - 1; i++) {
			System.out.print(((IntField) n.keys[i]).getValue());
			if (i < n.num - 2) {
				System.out.print(", ");
			}
		}
		System.out.println("]\n" + spaces + "  children:");
		for (int i = 0; i < n.num; i++) {
			printYml(n.nodes[i], lvl+1);
		}
	}
	
	private void printYml(LeafNode n, int lvl) {
		String spaces = "";
		for (int i = 0; i < lvl; i++) {
			spaces += "  ";
		}
	    System.out.print(spaces + (lvl == 0 ? "  [" : "- ["));
		for (int i = 0; i < n.num; i++) {
			System.out.print(((IntField) n.entries[i].getField()).getValue());
			if (i < n.num - 1) {
				System.out.print(", ");
			}
		}
		System.out.println("]");
	}
	
	private void printYml(Node n, int lvl) {
		if (n instanceof LeafNode) {
			printYml((LeafNode) n, lvl);
		}
		else {
			printYml((InnerNode) n, lvl);
		}
	}
	
	public void printYml() {
		if (root == null) {
			System.out.println("Empty");
		}
		else {
			System.out.println("keys_per_block: " + Math.max(pInner, pLeaf));
			System.out.println("tree:");
			printYml(root, 0);
			System.out.println();
		}
	}
	
	private int findIdx(InnerNode p, Node c) {
		for (int i = 0; i < p.num; i++) {
			if (p.nodes[i] == c) {
				return i;
			}
		}
		// shouldn't ever happen
		return -1;
	}

	public void delete(Entry e) {
		LeafNode l = search(e.getField());
		assert(l != null);
		
		// the index in the children list that e is
		int idx;
		for (idx = 0; l.entries[idx].getField().compare(RelationalOperator.NOTEQ, e.getField()); idx++);

		// first remove the key from this leaf
		for (int i = idx; i < l.num - 1; i++) {
			l.entries[i] = l.entries[i + 1];
		}
		l.entries[l.num - 1] = null;
		l.num--;
		
		if ((l == root) || l.num >= pLeafLower()) {
			return;
		}
		else {
			// we have to borrow from/merge with a neighbor
			InnerNode p = (InnerNode) l.getParent();
			int i = findIdx(p, l);
			
			if (i > 0 && ((LeafNode) p.nodes[i - 1]).num > pLeafLower()) {
				// we can borrow from the left neighbor
				LeafNode ln = (LeafNode) p.nodes[i - 1];
				
				// remove the largest guy from the left neighbor
				Entry toBorrow = ln.entries[ln.num - 1];
				ln.entries[ln.num - 1] = null;
				ln.num--;
				
				// insert it into the beginning of this leaf
				for (int j = l.num; j > 0; j--) {
					l.entries[j] = l.entries[j - 1];
				}
				l.entries[0] = toBorrow;
				l.num++;
				
				// fix the left neighbor's key in the parent
				p.keys[i - 1] = ln.entries[ln.num - 1].getField();
				// don't need to fix our key because the largest one didn't change
				return;
			}
			if (i < p.num - 1 && ((LeafNode) p.nodes[i + 1]).num > pLeafLower()) {
				// we can borrow from the right neighbor
				LeafNode rn = (LeafNode) p.nodes[i + 1];

				// remove the smallest guy from the right neighbor
				Entry toBorrow = rn.entries[0];
				for (int j = 0; j < rn.num - 1; j++) {
					rn.entries[j] = rn.entries[j + 1];
				}
				rn.entries[rn.num - 1] = null;
				rn.num--;
				
				// insert it into the end of this leaf
				l.entries[l.num] = toBorrow;
				l.num++;
				
				// fix our key in the parent
				p.keys[i] = toBorrow.getField();
				// don't need to fix the right neighbor's key because the largest one didn't change
				return;
			}
			
			// i is the index of the parent that this node is
			
			// if we can't borrow from either neighbor, we have to merge with a neighbor
			// we arbitrarily decide to merge with the left neighbor if we have a choice
			if (i == 0) {
				// merge with the right, it's our only choice
				LeafNode rn = (LeafNode) p.nodes[i + 1];
				for (int j = 0; j < rn.num; j++) {
					l.entries[j + l.num] = rn.entries[j];
				}
				l.num += rn.num;
				
				// don't need to update b/c l is already the first element
				// p.nodes[0] = l;
				for (int j = 1; j < p.num - 1; j++) {
					p.nodes[j] = p.nodes[j + 1];
					p.keys[j - 1] = p.keys[j];
				}
				p.nodes[p.num - 1] = null;
				p.keys[p.num - 2] = null;
				p.num--;
			}
			else {
				// merge with the left
				LeafNode ln = (LeafNode) p.nodes[i - 1];
				for (int j = 0; j < l.num; j++) {
					ln.entries[j + ln.num] = l.entries[j]; 
				}
				ln.num += l.num;
				
				for (int j = i; j < p.num - 1; j++) {
					p.nodes[j] = p.nodes[j + 1];
					p.keys[j - 1] = p.keys[j];
				}
				p.nodes[p.num - 1] = null;
				p.keys[p.num - 2] = null;
				p.num--;
				
				// TODO also comment
				l = ln;
			}
			
			// pc is the child of p that was just updated
			Node pc = l;
			
			// now that p is one element smaller, see if we have to do more corrections
			while (true) {
				
				if (p.getParent() == null) {
					// no need to correct any more
					if (p.num == 1) {
						// if we just emptied the root, make the last remaining child of the
						// root the new one
						root = pc;
						pc.setParent(null);
					}
					return;
				}
				if (p.num >= pInnerLower()) {
					// we don't need to correct any more
					return;
				}

				InnerNode c = p;
				pc = c;
				p = (InnerNode) c.getParent();
				
				i = findIdx(p, c);
				
				if (i > 0 && ((InnerNode) p.nodes[i - 1]).num > pInnerLower()) {
					// we can borrow from the left neighbor
					InnerNode ln = (InnerNode) p.nodes[i - 1];
					
					// remove the largest guy from the left neighbor
					Node toBorrow = ln.nodes[ln.num - 1];
					ln.nodes[ln.num - 1] = null;
					Field toBorrowKey = ln.keys[ln.num - 2];
					ln.keys[ln.num - 2] = null;
					ln.num--;
					
					// insert it into the beginning of this node
					for (int j = c.num; j > 0; j--) {
						c.nodes[j] = c.nodes[j - 1];
						if (j < c.num) {
							c.keys[j] = c.keys[j - 1];
						}
					}
					c.nodes[0] = toBorrow;
					toBorrow.setParent(c);
					Field newChildKey = p.keys[i - 1];
					c.num++;
					
					// fix the left neighbor's key in the parent
					p.keys[i - 1] = toBorrowKey;
					if (c.num > 1) {
						c.keys[0] = newChildKey;
					}
					return;
				}
				if (i < p.num - 1 && ((InnerNode) p.nodes[i + 1]).num > pInnerLower()) {
					// we can borrow from the right neighbor
					InnerNode rn = (InnerNode) p.nodes[i + 1];

					// remove the smallest guy from the right neighbor
					Node toBorrow = rn.nodes[0];
					Field toBorrowKey = rn.keys[0];
					for (int j = 0; j < rn.num - 1; j++) {
						rn.nodes[j] = rn.nodes[j + 1];
						if (j < rn.num - 2) {
							rn.keys[j] = rn.keys[j + 1];
						}
					}
					rn.nodes[rn.num - 1] = null;
					rn.keys[rn.num - 2] = null;
					rn.num--;
					
					// insert it into the end of this leaf
					c.nodes[c.num] = toBorrow;
					toBorrow.setParent(c);
					c.keys[c.num - 1] = p.keys[i];
					c.num++;
					
					// fix our key in the parent
					p.keys[i] = toBorrowKey;
					// don't need to fix the right neighbor's key because the largest one didn't change
					return;
				}
				
				// i is the index of the parent that this node is
				
				// if we can't borrow from either neighbor, we have to merge with a neighbor
				// we arbitrarily decide to merge with the left neighbor if we have a choice
				if (i == 0) {
					// merge with the right, it's our only choice
					InnerNode rn = (InnerNode) p.nodes[i + 1];
					for (int j = 0; j < rn.num; j++) {
						c.nodes[j + c.num] = rn.nodes[j];
						rn.nodes[j].setParent(c);
						if (j < rn.num - 1) {
							c.keys[j + c.num] = rn.keys[j];
						}
					}
					c.keys[c.num - 1] = p.keys[i];
					c.num += rn.num;
					// c is already the first element of p

					for (int j = 1; j < p.num - 1; j++) {
						p.nodes[j] = p.nodes[j + 1];
						p.keys[j - 1] = p.keys[j];
					}
					p.nodes[p.num - 1] = null;
					p.keys[p.num - 2] = null;
					p.num--;
				}
				else {
					// merge with the left
					InnerNode ln = (InnerNode) p.nodes[i - 1];
					for (int j = 0; j < c.num; j++) {
						ln.nodes[j + ln.num] = c.nodes[j];
						c.nodes[j].setParent(ln);
						if (j < c.num - 1) {
							ln.keys[j + ln.num] = c.keys[j];
						}
					}
					ln.keys[ln.num - 1] = p.keys[i - 1];
					ln.num += c.num;

					for (int j = i; j < p.num - 1; j++) {
						p.nodes[j] = p.nodes[j + 1];
						p.keys[j - 1] = p.keys[j];
					}
					p.nodes[p.num - 1] = null;
					p.keys[p.num - 2] = null;
					p.num--;

					// TODO comment
					pc = ln;
				}
			}
		}
	}

	public Node getRoot() {
		return root == null || ((root instanceof LeafNode) && ((LeafNode) root).num == 0) ? null : root;
	}

}
