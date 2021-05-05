//Laney Ching & Katherine Zhou
package hw1;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc {

	private Type[] types;
	private String[] fields;
	
    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr array specifying the number of and types of fields in
     *        this TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
    	assert(typeAr.length == fieldAr.length);
    	types = typeAr;
    	fields = fieldAr;
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
    	return fields.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
    	if (i >= 0 && i < numFields()) {
    		return fields[i];
    	}
    	throw new NoSuchElementException("Index " + i + " out of bounds");
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int nameToId(String name) throws NoSuchElementException {
    	int n = numFields();
    	for (int i = 0; i < n; i++) {
    		if (name.equals(getFieldName(i))) {
    			return i;
    		}
    	}
    	throw new NoSuchElementException("No such field \"" + name + "\"");
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid index.
     * @return the type of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getType(int i) throws NoSuchElementException {
    	if (i >= 0 && i < numFields()) {
    		return types[i];
    	}
    	throw new NoSuchElementException("Index " + i + " out of bounds");
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     * Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
    	int totalSize = 0;
    	int n = numFields();
    	for (int i = 0; i < n; i++) {
    		switch (getType(i)) {
    			case INT:
    				totalSize += 4;
    				break;
    			case STRING:
    				totalSize += 129;
    				break;
    		}
    	}
    	return totalSize;
    }

    /**
     * Compares the specified object with this TupleDesc for equality.
     * Two TupleDescs are considered equal if they are the same size and if the
     * n-th type in this TupleDesc is equal to the n-th type in td.
     *
     * @param o the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */
    public boolean equals(Object o) {
    	if (!(o instanceof TupleDesc)) {
    		return false;
    	}
    	TupleDesc td = (TupleDesc) o;
    	int n = numFields();
    	if (n != td.numFields()) {
    		return false;
    	}
    	for (int i = 0; i < n; i++) {
    		if (getType(i) != td.getType(i)) {
    			return false;
    		}
    	}
    	return true;
    }
    

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     * @return String describing this descriptor.
     */
    public String toString() {
    	String s = "";
    	int n = numFields();
    	for (int i = 0; i < n; i++) {
    		switch (getType(i)) {
    			case INT:
    				s += "INT";
    				break;
    			case STRING:
    				s += "STRING";
    				break;
    		}
    		s += "(" + getFieldName(i) + ")";
    		if (i != n - 1) {
    			s += ", ";
    		}
    	}
    	return s;
    }
}
