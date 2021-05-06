package hw4;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import hw1.Database;
import hw1.HeapFile;
import hw1.HeapPage;
import hw1.Tuple;

/**
 * BufferPool manages the reading and writing of pages into memory from disk.
 * Access methods call into it to retrieve pages, and it fetches pages from the
 * appropriate location.
 * <p>
 * The BufferPool is also responsible for locking; when a transaction fetches a
 * page, BufferPool which check that the transaction has the appropriate locks
 * to read/write the page.
 */
public class BufferPool {
	/** Bytes per page, including header. */
	public static final int PAGE_SIZE = 4096;

	/**
	 * Default number of pages passed to the constructor. This is used by other
	 * classes. BufferPool should use the numPages argument to the constructor
	 * instead.
	 */
	public static final int DEFAULT_PAGES = 50;
	
	public static final int READ_LOCK_TIMEOUT_MS = 1000;
	public static final int WRITE_LOCK_TIMEOUT_MS = 1000;

	private HeapPage cache[];
	private ReentrantReadWriteLock locks[];
	private ArrayList<LinkedList<Integer>> tids;
	private boolean dirty[];
	// offset in cache that we should start trying to evict from
	private int global_offset;

	/**
	 * Creates a BufferPool that caches up to numPages pages.
	 *
	 * @param numPages maximum number of pages in this buffer pool.
	 */
	public BufferPool(int numPages) {
		// your code here
		cache = new HeapPage[numPages];
		locks = new ReentrantReadWriteLock[numPages];
		for (int i = 0; i < numPages; i++) {
			locks[i] = new ReentrantReadWriteLock();
		}
		tids = new ArrayList<>(numPages);
		for (int i = 0; i < numPages; i++) {
			tids.add(new LinkedList<>());
		}
		dirty = new boolean[numPages];
		global_offset = 0;
	}
	
	private void get_lock(int cache_idx, Permissions perm) throws Exception {
		if (perm == Permissions.READ_WRITE) {
			if (locks[cache_idx].writeLock().isHeldByCurrentThread() ||
					!locks[cache_idx].writeLock().tryLock(WRITE_LOCK_TIMEOUT_MS, TimeUnit.MICROSECONDS)) {
				throw new Exception("Unable to acquire write lock");
			}
		}
		else {
			if (locks[cache_idx].writeLock().isHeldByCurrentThread() ||
					!locks[cache_idx].readLock().tryLock(READ_LOCK_TIMEOUT_MS, TimeUnit.MICROSECONDS)) {
				throw new Exception("Unable to acquire read lock");
			}
		}
	}
	
	private void release_lock(int cache_idx) {
		if (locks[cache_idx].isWriteLocked()) {
			locks[cache_idx].writeLock().unlock();
		}
		else {
			locks[cache_idx].readLock().unlock();
		}
	}
	
	private boolean is_locked(int cache_idx, int tid) {
		if (tids.get(cache_idx).size() == 1 && tids.get(cache_idx).contains((Integer) tid) && !dirty[cache_idx]) {
			return false;
		}
		return locks[cache_idx].isWriteLocked() || locks[cache_idx].getReadLockCount() > 0;
	}

	/**
	 * Retrieve the specified page with the associated permissions. Will acquire a
	 * lock and may block if that lock is held by another transaction.
	 * <p>
	 * The retrieved page should be looked up in the buffer pool. If it is present,
	 * it should be returned. If it is not present, it should be added to the buffer
	 * pool and returned. If there is insufficient space in the buffer pool, a page
	 * should be evicted and the new page should be added in its place.
	 *
	 * @param tid     the ID of the transaction requesting the page
	 * @param tableId the ID of the table with the requested page
	 * @param pid     the ID of the requested page
	 * @param perm    the requested permissions on the page
	 */
	public HeapPage getPage(int tid, int tableId, int pid, Permissions perm) throws Exception {
		// your code here
		for (int i = 0; i < cache.length; i++) {
			if (cache[i] != null && (cache[i].getTableId() == tableId && cache[i].getId() == pid)) {
				if (tids.get(i).contains((Integer) tid)) {
					// we already have a lock on this page
//					if (locks[i].isWriteLocked() && perm == Permissions.READ_WRITE) {
//						return cache[i];
//					}
//					else if (locks[i].getReadLockCount() > 0 && perm == Permissions.READ_ONLY) {
//						return cache[i];
//					}
					if (locks[i].isWriteLocked() && perm == Permissions.READ_ONLY) {
						release_lock(i);
						// fall through to downgrade to read lock
					}
					if (locks[i].getReadLockCount() > 0 && perm == Permissions.READ_WRITE) {
						release_lock(i);
						// fall through to acquire a write lock on the page
					}
					else {
						return cache[i];
					}
				}
				try {
					get_lock(i, perm);
				}
				catch (Exception e) {
					transactionComplete(tid, false);
					throw e;
				}
				tids.get(i).add(tid);
				return cache[i];
			}
		}
		// search for null page
		for (int i = 0; i < cache.length; i++) {
			if (cache[i] == null) {
				cache[i] = Database.getCatalog().getDbFile(tableId).readPage(pid);
				try {
					get_lock(i, perm);
				}
				catch (Exception e) {
					transactionComplete(tid, false);
					throw e;
				}
				tids.get(i).add(tid);
				return cache[i];
			}
		}
		// this page was not in the cache, so try to evict another page
		try {
			evictPage(tid);
		}
		catch (Exception e) {
			transactionComplete(tid, false);
			throw e;
		}
		for (int i = 0; i < cache.length; i++) {
			if (cache[i] == null) {
				cache[i] = Database.getCatalog().getDbFile(tableId).readPage(pid);
				try {
					get_lock(i, perm);
				}
				catch (Exception e) {
					transactionComplete(tid, false);
					throw e;
				}
				tids.get(i).add(tid);
				return cache[i];
			}
		}
		transactionComplete(tid, false);
		throw new Exception("No available cache slots for new page");
	}
	
	public HeapPage getPageInCache(int tid, int tableId, int pid, Permissions perm) throws Exception {
		for (int i = 0; i < cache.length; i++) {
			if (cache[i] != null && (cache[i].getTableId() == tableId && cache[i].getId() == pid)) {
				if (tids.get(i).contains((Integer) tid)) {
					// we already have a lock on this page
					if (locks[i].isWriteLocked() || perm == Permissions.READ_ONLY) {
						return cache[i];
					}
					else {
						throw new Exception("Trying to write with a read lock");
					}
				}
				throw new Exception("Trying to get page without lock");
			}
		}
		throw new Exception("Trying to get page that isn't in the cache");
	}

	/**
	 * Releases the lock on a page. Calling this is very risky, and may result in
	 * wrong behavior. Think hard about who needs to call this and why, and why they
	 * can run the risk of calling it.
	 *
	 * @param tid     the ID of the transaction requesting the unlock
	 * @param tableID the ID of the table containing the page to unlock
	 * @param pid     the ID of the page to unlock
	 */
	public void releasePage(int tid, int tableId, int pid) {
		// your code here
		for (int i = 0; i < cache.length; i++) {
			if (cache[i].getTableId() == tableId && cache[i].getId() == pid) {
				release_lock(i);
				tids.get(i).remove((Integer) tid);
				return;
			}
		}
		// can't get here
		assert(false);
	}

	/** Return true if the specified transaction has a lock on the specified page */
	public boolean holdsLock(int tid, int tableId, int pid) {
		// your code here
		for (int i = 0; i < cache.length; i++) {
			if (cache[i].getTableId() == tableId && cache[i].getId() == pid) {
				return tids.get(i).contains((Integer) tid);
			}
		}
		return false;
	}

	/**
	 * Commit or abort a given transaction; release all locks associated to the
	 * transaction. If the transaction wishes to commit, write
	 *
	 * @param tid    the ID of the transaction requesting the unlock
	 * @param commit a flag indicating whether we should commit or abort
	 */
	public void transactionComplete(int tid, boolean commit) throws IOException {
		// your code here
		for (int i = 0; i < cache.length; i++) {
			if (tids.get(i).contains((Integer) tid)) {
				// this page is held by tid
				if (dirty[i]) {
					if (commit) {
						// flush the page to disk and erase the dirty bit
						flushPage(cache[i].getTableId(), cache[i].getId());
					}
					else {
						// reload the page from disk
						cache[i] = Database.getCatalog().getDbFile(cache[i].getTableId()).readPage(cache[i].getId());
						dirty[i] = false;
					}
				}
				tids.get(i).remove((Integer) tid);
				release_lock(i);
			}
		}
	}

	/**
	 * Add a tuple to the specified table on behalf of transaction tid. Will acquire a
	 * write lock on the page the tuple is added to. May block if the lock cannot be
	 * acquired.
	 * 
	 * Marks any pages that were dirtied by the operation as dirty
	 *
	 * @param tid     the transaction adding the tuple
	 * @param tableId the table to add the tuple to
	 * @param t       the tuple to add
	 */
	public void insertTuple(int tid, int tableId, Tuple t) throws Exception {
		// your code here
		try {
			HeapFile f = Database.getCatalog().getDbFile(tableId);
			for (int i = 0; i < f.getNumPages(); i++) {
				HeapPage page = getPageInCache(tid, tableId, i, Permissions.READ_WRITE);
				if (!locks[i].isWriteLockedByCurrentThread()) {
					// we don't have write permission
					transactionComplete(tid, false);
					throw new Exception("We didn't have the write lock before?");
				}
				if(page.addTuple(t)) {
					dirty[i] = true;
					return;
				}
				releasePage(tid, tableId, i);
			}
			HeapPage hpage;
			hpage = getPageInCache(tid, tableId, f.getNumPages(), Permissions.READ_WRITE);
			hpage.addTuple(t);
			for (int i = 0; i < cache.length; i++) {
				if (cache[i] == hpage) {
					dirty[i] = true;
					break;
				}
			}
		}
		catch (Exception e) {
			transactionComplete(tid, false);
			throw e;
		}
	}

	/**
	 * Remove the specified tuple from the buffer pool. Will acquire a write lock on
	 * the page the tuple is removed from. May block if the lock cannot be acquired.
	 *
	 * Marks any pages that were dirtied by the operation as dirty.
	 *
	 * @param tid     the transaction adding the tuple.
	 * @param tableId the ID of the table that contains the tuple to be deleted
	 * @param t       the tuple to add
	 */
	public void deleteTuple(int tid, int tableId, Tuple t) throws Exception {
		// your code here
		HeapPage p = getPageInCache(tid, tableId, t.getPid(), Permissions.READ_WRITE);
		for (int i = 0; i < cache.length; i++) {
			if (cache[i] != p) {
				continue;
			}
			if (!locks[i].isWriteLockedByCurrentThread()) {
				// we don't have write permission
				transactionComplete(tid, false);
				throw new Exception("We didn't have the write lock before?");
			}
			dirty[i] = true;
		}
		p.deleteTuple(t);
	}

	private synchronized void flushPage(int tableId, int pid) throws IOException {
		// your code here
		for (int i = 0; i < cache.length; i++) {
			if (cache[i].getTableId() == tableId && cache[i].getId() == pid) {
				Database.getCatalog().getDbFile(cache[i].getTableId()).writePage(cache[i]);
				dirty[i] = false;
				return;
			}
		}
	}

	/**
	 * Discards a page from the buffer pool. Flushes the page to disk to ensure
	 * dirty pages are updated on disk.
	 */
	private synchronized void evictPage(int tid) throws Exception {
		// your code here
		for (int i = 0; i < cache.length; i++) {
			int idx = (i + global_offset) % cache.length;
			if (!is_locked(idx, tid)) {
				if (tids.get(idx).size() == 1) {
					release_lock(idx);
				}
				// we can evict this one
				flushPage(cache[idx].getTableId(), cache[idx].getId());
				cache[idx] = null;
				global_offset = (idx + 1) % cache.length;
				return;
			}
		}
		throw new Exception("No available cache slots for new page");
	}

}
