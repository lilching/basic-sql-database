//Laney Ching & Katherine Zhou
package hw1;

import java.sql.Types;
import java.util.HashMap;

/**
 * This class represents a tuple that will contain a single row's worth of information
 * from a table. It also includes information about where it is stored
 * @author Sam Madden modified by Doug Shook
 *
 */
public class Tuple {
	
	Field fields[];
	TupleDesc td;
	int pid;
	int id;
	
	/**
	 * Creates a new tuple with the given description
	 * @param t the schema for this tuple
	 */
	public Tuple(TupleDesc t) {
		td = t;
		fields = new Field[t.numFields()];
	}
	
	public TupleDesc getDesc() {
		return td;
	}
	
	/**
	 * retrieves the page id where this tuple is stored
	 * @return the page id of this tuple
	 */
	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	/**
	 * retrieves the tuple (slot) id of this tuple
	 * @return the slot where this tuple is stored
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void setDesc(TupleDesc td) {
		this.td = td;
	}
	
	/**
	 * Stores the given data at the i-th field
	 * @param i the field number to store the data
	 * @param v the data
	 */
	public void setField(int i, Field v) {
		fields[i] = v;
	}
	
	public Field getField(int i) {
		return fields[i];
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Tuple)) {
			return false;
		}
		Tuple t = (Tuple) o;
		if (!t.td.equals(td)) {
			return false;
		}
		for (int i = 0; i < td.numFields(); i++) {
			if (!fields[i].compare(RelationalOperator.EQ, t.fields[i])) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Creates a string representation of this tuple that displays its contents.
	 * You should convert the binary data into a readable format (i.e. display the ints in base-10 and convert
	 * the String columns to readable text).
	 */
	public String toString() {
		String s = "";
    	int n = td.numFields();
    	for (int i = 0; i < n; i++) {
    		switch (td.getType(i)) {
    			case INT: {
    				IntField f = (IntField) fields[i];
    				s += f;
    				break;
    			}
    			case STRING: {
    				StringField f = (StringField) fields[i];
    				s += f;
    				break;
    			}
    		}
    		if (i != n - 1) {
    			s += ", ";
    		}
    	}
    	return s;
	}
}
	