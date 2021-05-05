//Laney Ching & Katherine Zhou
package hw1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A heap file stores a collection of tuples. It is also responsible for managing pages.
 * It needs to be able to manage page creation as well as correctly manipulating pages
 * when tuples are added or deleted.
 * @author Sam Madden modified by Doug Shook
 *
 */
public class HeapFile {
	
	public static final int PAGE_SIZE = 4096;
	File file;
	TupleDesc td;
	
	/**
	 * Creates a new heap file in the given location that can accept tuples of the given type
	 * @param f location of the heap file
	 * @param types type of tuples contained in the file
	 */
	public HeapFile(File f, TupleDesc type) {
		file = f;
		td = type;
	}
	
	public File getFile() {
		return file;
	}
	
	public TupleDesc getTupleDesc() {
		return td;
	}
	
	/**
	 * Creates a HeapPage object representing the page at the given page number.
	 * Because it will be necessary to arbitrarily move around the file, a RandomAccessFile object
	 * should be used here.
	 * @param id the page number to be retrieved
	 * @return a HeapPage at the given page number
	 */
	@SuppressWarnings("resource")
	public HeapPage readPage(int id) {
		RandomAccessFile fi;
		try {
			fi = new RandomAccessFile(file, "rw");
			fi.seek(id * PAGE_SIZE);
			byte[] buf = new byte[PAGE_SIZE];
			fi.read(buf);
			return new HeapPage(id, buf, getId());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns a unique id number for this heap file. Consider using
	 * the hash of the File itself.
	 * @return
	 */
	public int getId() {
		return file.hashCode();
	}
	
	/**
	 * Writes the given HeapPage to disk. Because of the need to seek through the file,
	 * a RandomAccessFile object should be used in this method.
	 * @param p the page to write to disk
	 */
	public void writePage(HeapPage p) {
		RandomAccessFile fi;
		try {
			fi = new RandomAccessFile(file, "rw");
			fi.seek(p.getId() * PAGE_SIZE);
			fi.write(p.getPageData());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a tuple. This method must first find a page with an open slot, creating a new page
	 * if all others are full. It then passes the tuple to this page to be stored. It then writes
	 * the page to disk (see writePage)
	 * @param t The tuple to be stored
	 * @return The HeapPage that contains the tuple
	 */
	@Deprecated
	public HeapPage addTuple(Tuple t) {
		for (int i = 0; i < getNumPages(); i++) {
			try {
				HeapPage page = readPage(i);
				if(page.addTuple(t)) {
					return page;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		HeapPage hpage = readPage(getNumPages());
		try {
			hpage.addTuple(t);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return hpage;
		
	}
	
	/**
	 * This method will examine the tuple to find out where it is stored, then delete it
	 * from the proper HeapPage. It then writes the modified page to disk.
	 * @param t the Tuple to be deleted
	 */
	@Deprecated
	public void deleteTuple(Tuple t){
		HeapPage p = readPage(t.getPid());
		p.setSlotOccupied(t.getId(), false);
		writePage(p);
	}
	
	/**
	 * Returns an ArrayList containing all of the tuples in this HeapFile. It must
	 * access each HeapPage to do this (see iterator() in HeapPage)
	 * @return
	 */
	@Deprecated
	public ArrayList<Tuple> getAllTuples() {
		ArrayList<Tuple> ret = new ArrayList<>();
		for (int i = 0; i < getNumPages(); i++) {
			HeapPage page = readPage(i);
			page.iterator().forEachRemaining(t -> ret.add(t));
		}
		return ret;
	}
	
	/**
	 * Computes and returns the total number of pages contained in this HeapFile
	 * @return the number of pages
	 */
	public int getNumPages() {
		return (int) (file.length() / PAGE_SIZE);
	}
}
