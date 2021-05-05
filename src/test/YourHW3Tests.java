//Laney Ching and Katherine Zhou

package test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import hw1.Field;
import hw1.IntField;
import hw1.RelationalOperator;
import hw3.BPlusTree;
import hw3.Entry;
import hw3.InnerNode;
import hw3.LeafNode;
import hw3.Node;

public class YourHW3Tests {
	
	void assertValid(BPlusTree tree, LeafNode n, Field lb, Field ub) {
		if (n.getParent() != null) {
			assert(n.num >= tree.pLeafLower() && n.num <= tree.pLeaf());
		}

		if (lb != null) {
			assert(lb.compare(RelationalOperator.LTE, n.entries[0].getField()));
		}
		for (int i = 0; i < n.num - 1; i++) {
			assert(n.entries[i].getField().compare(RelationalOperator.LT, n.entries[i+1].getField()));
		}
		if (ub != null) {
			assert(n.entries[n.num - 1].getField().compare(RelationalOperator.LTE, ub));
		}
	}
	
	void assertValid(BPlusTree tree, InnerNode n, Field lb, Field ub) {
		if (n.getParent() != null) {
			assert(n.num >= tree.pInnerLower() && n.num <= tree.pInner());
		}

		if (lb != null) {
			assert(lb.compare(RelationalOperator.LTE, n.keys[0]));
		}
		assertValid(tree, n.nodes[0], lb, n.keys[0]);
		assert(n.nodes[0].getParent() == n);
		for (int i = 0; i < n.num - 2; i++) {
			assert(n.keys[i].compare(RelationalOperator.LT, n.keys[i+1]));
			assertValid(tree, n.nodes[i + 1], n.keys[i], n.keys[i + 1]);	
			assert(n.nodes[i + 1].getParent() == n);
		}
		if (n.num > 1) {
			assertValid(tree, n.nodes[n.num - 1], n.keys[n.num - 2], ub);
			assert(n.nodes[n.num - 1].getParent() == n);
		}
		if (ub != null) {
			assert(n.keys[n.num - 2].compare(RelationalOperator.LTE, ub));
		}
	}
	
	void assertValid(BPlusTree tree, Node n, Field lb, Field ub) {
		if (n instanceof InnerNode) {
			assertValid(tree, (InnerNode) n, lb, ub);
		}
		else {
			assertValid(tree, (LeafNode) n, lb, ub);
		}
	}
	
	void assertValid(BPlusTree tree) {
		if (tree.getRoot() != null) {
			assert(tree.getRoot().getParent() == null);
			
			if (tree.getRoot() instanceof InnerNode) {
				assert(((InnerNode) tree.getRoot()).num > 1);
			}
			assertValid(tree, tree.getRoot(), null, null);
		}
	}

	@Test
	public void testWithInsert() {
		final int n_recs = 2000;
		Field f[] = new Field[n_recs];
		for (int i = 0; i < n_recs; i++) {
			f[i] = new IntField(i);
		}
		// shuffle the records
		for (int i = 0; i < n_recs; i++) {
			int replace = (int) (Math.random() * (i + 1));
			Field tmp = f[i];
			f[i] = f[replace];
			f[replace] = tmp;
		}
		
		BPlusTree tree = new BPlusTree(7, 16);
		
		// insert all the records
		for (int i = 0; i < n_recs; i++) {
			Entry e = new Entry(f[i], 0);
			tree.insert(e);
			assertValid(tree);
			
			// make sure every record is still in there
			for (int j = 0; j <= i; j++) {
				assert(tree.search(f[i]) != null);
			}
		}
	}

	@Test
	public void testWithDelete() {
		final int n_recs = 4000;
		Field f[] = new Field[n_recs];
		Field d[] = new Field[n_recs];
		for (int i = 0; i < n_recs; i++) {
			f[i] = new IntField(i);
			d[i] = new IntField(i);
		}
		// shuffle the records
		for (int i = 0; i < n_recs; i++) {
			int replace = (int) (Math.random() * (i + 1));
			Field tmp = f[i];
			f[i] = f[replace];
			f[replace] = tmp;

			replace = (int) (Math.random() * (i + 1));
			tmp = d[i];
			d[i] = d[replace];
			d[replace] = tmp;
		}
		
		BPlusTree tree = new BPlusTree(66, 6);
		
		// insert all the records
		for (int i = 0; i < n_recs; i++) {
			Entry e = new Entry(f[i], 0);
			tree.insert(e);
			assertValid(tree);
			
			// make sure every record is still in there
			for (int j = 0; j <= i; j++) {
				assert(tree.search(f[i]) != null);
			}
		}

		// delete all the records in a different order
		for (int i = 0; i < n_recs; i++) {
			Entry e = new Entry(d[i], 0);
			tree.delete(e);
			assertValid(tree);

			// make sure every record is still in there
			for (int j = i + 1; j < n_recs; j++) {
				assert(tree.search(d[j]) != null);
			}
			// and none of the records that have been deleted are still there
			for (int j = 0; j <= i; j++) {
				assert(tree.search(d[j]) == null);
			}
		}
	}

}
