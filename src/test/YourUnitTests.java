//Laney Ching & Katherine Zhou
package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import hw1.Catalog;
import hw1.Database;
import hw1.HeapFile;
import hw1.HeapPage;
import hw1.IntField;
import hw1.StringField;
import hw1.Tuple;
import hw1.TupleDesc;

public class YourUnitTests {
	
	private HeapFile hf;
	private TupleDesc td;
	private Catalog c;
	private HeapPage hp;

	@Before
	public void setup() {
		
		try {
			Files.copy(new File("testfiles/test.dat.bak").toPath(), new File("testfiles/test.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("unable to copy files");
			e.printStackTrace();
		}
		
		c = Database.getCatalog();
		c.loadSchema("testfiles/test.txt");
		
		int tableId = c.getTableId("test");
		td = c.getTupleDesc(tableId);
		hf = c.getDbFile(tableId);
		hp = hf.readPage(0);
	}
	
	@Test
	public void testInsertAgain() {
		// first let's empty the database
		ArrayList<Tuple> ts = new ArrayList<>();
		hp.iterator().forEachRemaining(t -> ts.add(t));
		for (Tuple t : ts) {
			hp.deleteTuple(t);
		}
		
		Tuple t = new Tuple(td);
		t.setField(0, new IntField(new byte[] {0, 0, (byte) 2, (byte) 71}));
		byte[] s = new byte[129];
		s[0] = 23;
		s[1] = 't';
		s[2] = 'h';
		s[3] = 'i';
		s[4] = 's';
		s[5] = ' ';
		s[6] = 'i';
		s[7] = 's';
		s[8] = ' ';
		s[9] = 'a';
		s[10] = ' ';
		s[11] = 's';
		s[12] = 'e';
		s[13] = 'c';
		s[14] = 'r';
		s[15] = 'e';
		s[16] = 't';
		s[17] = ',';
		s[18] = ' ';
		s[19] = 's';
		s[20] = 'h';
		s[21] = 'h';
		s[22] = 'h';
		s[23] = '!';

		t.setField(1, new StringField(s));
		try {
			hp.addTuple(t);
		} catch (Exception e) {
			fail("Exception thrown when adding tuple to heap page");
		}

		Iterator<Tuple> tuples = hp.iterator();
		assertTrue("No tuples found in the database", tuples.hasNext());
		Tuple a = tuples.next();
		assertTrue("Tuple does not match what was inserted", a.equals(t));
		assertTrue("Extra tuples in database", !tuples.hasNext());
	}
	
	@Test
	public void test2() {
		// first let's empty the database
		ArrayList<Tuple> ts = new ArrayList<>();
		for (int i = 0; i < hf.getNumPages(); i++) {
			hf.readPage(i).iterator().forEachRemaining(t -> ts.add(t));
		}
		for (Tuple t : ts) {
			hf.deleteTuple(t);
		}
		System.out.println("how many: " + hp.getNumSlots());
		
		for (int i = 0; i < 256; i++) {
			Tuple t = new Tuple(td);
			t.setField(0, new IntField(new byte[] {0, 0, 0, (byte) i}));
			byte[] s = new byte[129];
			s[0] = 0;
			t.setField(1, new StringField(s));
			
			try {
				hf.addTuple(t);
			} catch (Exception e) {
				fail("Exception thrown when adding tuple to heap page");
			}
		}
		
		int total = 0;
		for (int i = 0; i < hf.getNumPages(); i++) {
			Iterator<Tuple> tuples = hf.readPage(i).iterator();
			while (tuples.hasNext()) {
				Tuple a = tuples.next();
				assertTrue("Tuple does not match what was inserted", a.getField(0).toByteArray()[3] == (total > 127 ? -256 + total : total));
				total++;
			}
		}
		assertTrue("Expected 256 tuples", total == 256);
	}

}
