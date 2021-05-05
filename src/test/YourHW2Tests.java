//Laney Ching & Katherine Zhou
package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import hw1.Catalog;
import hw1.Database;
import hw1.HeapFile;
import hw1.IntField;
import hw1.StringField;
import hw1.Tuple;
import hw1.TupleDesc;
import hw2.Query;
import hw2.Relation;

public class YourHW2Tests {

	private HeapFile testhf;
	private TupleDesc testtd;
	private HeapFile ahf;
	private TupleDesc atd;
	private Catalog c;

	@Before
	public void setup() {
		
		try {
			Files.copy(new File("testfiles/test.dat.bak").toPath(), new File("testfiles/test.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(new File("testfiles/A.dat.bak").toPath(), new File("testfiles/A.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("unable to copy files");
			e.printStackTrace();
		}
		
		c = Database.getCatalog();
		c.loadSchema("testfiles/test.txt");
		
		int tableId = c.getTableId("test");
		testtd = c.getTupleDesc(tableId);
		testhf = c.getDbFile(tableId);
		
		c = Database.getCatalog();
		c.loadSchema("testfiles/A.txt");
		
		tableId = c.getTableId("A");
		atd = c.getTupleDesc(tableId);
		ahf = c.getDbFile(tableId);
	}

	/**
	 * This test adds a bunch of tuples, each of which belong to one of 3 groups
	 * based on the first column. The second column is of strings, and we test
	 * the MAX group by aggregator on these strings. This should return the
	 * alphabetically largest string in each of the three groups
	 */
	@Test
	public void StringAggregationGroupByTest() {
		testhf = c.getDbFile(c.getTableId("test"));
		// remove all tuples currently in the table
		for (Tuple t : testhf.getAllTuples()) {
			testhf.deleteTuple(t);
		}
		
		// add the test tuples
		int[] ints = new int[] { 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4 };
		String[] strs = new String[] {
				// 1's:
				"a",
				"b",
				"c",
				"d",
				"e",
				// 2's:
				"laney",
				"kathy",
				"remy",
				"zzzzz",
				// 4's:
				"1",
				"2",
				"3",
				"4",
				"5",
				"6",
				"7"
		};
		
		for (int i = 0; i < ints.length; i++) {
			Tuple t = new Tuple(testhf.getTupleDesc());
			t.setField(0, new IntField(ints[i]));
			t.setField(1, new StringField(strs[i]));
			testhf.addTuple(t);
		}
		
		// construct the query that performs the group-by aggregation
		Query q = new Query("SELECT c1, MAX(c2) FROM test GROUP BY c1");
		Relation r = q.execute();
		
		// make sure the 3 tuples returned match what we expect
		ArrayList<Tuple> tps = r.getTuples();
		assertTrue(tps.size() == 3);
		
		Tuple t1 = new Tuple(testhf.getTupleDesc());
		t1.setField(0, new IntField(1));
		t1.setField(1, new StringField("e"));
		assertTrue(tps.contains(t1));

		Tuple t2 = new Tuple(testhf.getTupleDesc());
		t2.setField(0, new IntField(2));
		t2.setField(1, new StringField("zzzzz"));
		assertTrue(tps.contains(t2));

		Tuple t3 = new Tuple(testhf.getTupleDesc());
		t3.setField(0, new IntField(4));
		t3.setField(1, new StringField("7"));
		assertTrue(tps.contains(t3));
	}
	
	/**
	 * In this test, we test both joining and having a WHERE clause.
	 */
	@Test
	public void queryTest() {
		testhf = c.getDbFile(c.getTableId("test"));
		HeapFile testhf2 = c.getDbFile(c.getTableId("A"));
		// remove all tuples currently in the tables
		for (Tuple t : testhf.getAllTuples()) {
			testhf.deleteTuple(t);
		}
		for (Tuple t : testhf2.getAllTuples()) {
			testhf2.deleteTuple(t);
		}
		
		// add the test tuples
		int[] ints = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
		String[] strs = new String[] {
				// 1's:
				"a",
				"b",
				"c",
				"d",
				"e",
				// 2's:
				"laney",
				"kathy",
				"remy",
				"zzzzz",
				// 4's:
				"1",
				"2",
				"3",
				"4",
				"5",
				"6",
				"7"
		};
		
		for (int i = 0; i < ints.length; i++) {
			Tuple t = new Tuple(testhf.getTupleDesc());
			t.setField(0, new IntField(ints[i]));
			t.setField(1, new StringField(strs[i]));
			testhf.addTuple(t);
		}

		int[] ints1 = new int[] { 1,  2,  3,  5,  7, 11, 13,  8,  4,  9, 16,  6, 10, 12, 14, 15 };
		int[] ints2 = new int[] { 6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, 84, 90, 96 };

		for (int i = 0; i < ints1.length; i++) {
			Tuple t = new Tuple(testhf2.getTupleDesc());
			t.setField(0, new IntField(ints1[i]));
			t.setField(1, new IntField(ints2[i]));
			testhf2.addTuple(t);
		}

		// construct the query that performs the group-by aggregation
		Query q = new Query("SELECT * FROM test JOIN A ON a.a1 = test.c1 WHERE c2 >= laney");
		Relation r = q.execute();
		
		/*
		 * the three tuples that pass this condition are the ones containing "laney",
		 * "remy", and "zzzzz". Make sure the resulting tuples are just these 3
		 */
		ArrayList<Tuple> tps = r.getTuples();
		assertTrue(tps.size() == 3);
		
		Tuple t1 = new Tuple(r.getDesc());
		t1.setField(0, new IntField(6));
		t1.setField(1, new StringField("laney"));
		t1.setField(2, new IntField(6));
		t1.setField(3, new IntField(72));
		assertTrue(tps.contains(t1));

		Tuple t2 = new Tuple(r.getDesc());
		t2.setField(0, new IntField(8));
		t2.setField(1, new StringField("remy"));
		t2.setField(2, new IntField(8));
		t2.setField(3, new IntField(48));
		assertTrue(tps.contains(t2));

		Tuple t3 = new Tuple(r.getDesc());
		t3.setField(0, new IntField(9));
		t3.setField(1, new StringField("zzzzz"));
		t3.setField(2, new IntField(9));
		t3.setField(3, new IntField(60));
		assertTrue(tps.contains(t3));
	}

}
