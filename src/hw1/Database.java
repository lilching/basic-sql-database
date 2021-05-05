//Laney Ching & Katherine Zhou
package hw1;

import hw4.BufferPool;

/*
 * Student 1 name:
 * Student 2 name:
 * Date: 
 */

/** Database is a class that initializes a static
    variable used by the database system (the catalog)

    Provides a set of methods that can be used to access these variables
    from anywhere.
 */

public class Database {
	private static Database _instance = new Database();
	private final Catalog _catalog;
	private static BufferPool _pool = new BufferPool(BufferPool.DEFAULT_PAGES);

	private Database() {
		_catalog = new Catalog();
	}

	/** Return the catalog of the static Database instance*/
	public static Catalog getCatalog() {
		return _instance._catalog;
	}


	//reset the database, used for unit tests only.
	public static void reset() {
		_instance = new Database();
	}
	
	public static BufferPool getBufferPool() {
		return _pool;
	}
	
	public static BufferPool resetBufferPool(int nPages) {
		_pool = new BufferPool(nPages);
		return _pool;
	}

}