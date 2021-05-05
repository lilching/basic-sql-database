//Laney Ching & Katherine Zhou
package hw2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hw1.Field;
import hw1.RelationalOperator;
import hw1.Tuple;
import hw1.TupleDesc;
import hw1.Type;

/**
 * This class provides methods to perform relational algebra operations. It will be used
 * to implement SQL queries.
 * @author Doug Shook
 *
 */
public class Relation {

	private ArrayList<Tuple> tuples;
	private TupleDesc td;
	
	public Relation(ArrayList<Tuple> l, TupleDesc td) {
		tuples = l;
		this.td = td;
	}
	
	/**
	 * This method performs a select operation on a relation
	 * @param field number (refer to TupleDesc) of the field to be compared, left side of comparison
	 * @param op the comparison operator
	 * @param operand a constant to be compared against the given column
	 * @return
	 */
	public Relation select(int field, RelationalOperator op, Field operand) {
		ArrayList<Tuple> ans = new ArrayList<>();
		for (Tuple t : tuples) {
			if(t.getField(field).compare(op, operand)) {
				ans.add(t);
			}
		}
		return new Relation(ans, td);
	}

	/**
	 * This method performs a rename operation on a relation
	 * @param fields the field numbers (refer to TupleDesc) of the fields to be renamed
	 * @param names a list of new names. The order of these names is the same as the order of field numbers in the field list
	 * @return
	 */
	public Relation rename(ArrayList<Integer> fields, ArrayList<String> names) {
		String[] names2 = new String[td.numFields()];
		Type[] types = new Type[td.numFields()];
		for (int i = 0; i < td.numFields(); i++) {
			names2[i] = td.getFieldName(i);
			types[i] = td.getType(i);
		}
		for (int i = 0; i < fields.size(); i++) {
			for (String name : names2) {
				if (names.get(i).equalsIgnoreCase(name)) {
					throw new IllegalArgumentException("No duplicate names allowed");
				}
			}
			if (names.get(i) != null && names.get(i).length() > 0) {
				names2[fields.get(i)] = names.get(i);
			}
		}
		TupleDesc tdsc = new TupleDesc(types, names2);
		td = tdsc;
		return this;
	}
	
	/**
	 * This method performs a project operation on a relation
	 * @param fields a list of field numbers (refer to TupleDesc) that should be in the result
	 * @return
	 */
	public Relation project(ArrayList<Integer> fields) {
		String[] names2 = new String[fields.size()];
		Type[] types = new Type[fields.size()];
		for (int i = 0; i < fields.size(); i++) {
			if (fields.get(i) >= td.numFields() || fields.get(i) < 0) {
				throw new IllegalArgumentException("no such field " + fields.get(i));
			}
			names2[i] = td.getFieldName(fields.get(i));
			types[i] = td.getType(fields.get(i));
		}
		TupleDesc tdsc = new TupleDesc(types, names2);
		for (int i = 0; i < tuples.size(); i++) {
			Tuple tup = tuples.get(i);
			Tuple tupl = new Tuple(tdsc);
			int k = 0;
			for (Integer j: fields){
				tupl.setField(k,tup.getField(j));
				k++;
			}
			tuples.set(i, tupl);
		}
		td = tdsc;
		if (td.numFields() == 0) {
			tuples.clear();
		}
		return this;
	}
	
	private int bSearch(Tuple[] tuples, Tuple n, int field1, int field2) {
		int lo = 0;
		int hi = tuples.length;
		Field cmp = n.getField(field1);
		while (lo + 1 < hi) {
			int mid = (lo + hi - 1) / 2;
			if (tuples[mid].getField(field2).compare(RelationalOperator.LT, cmp)) {
				lo = mid + 1;
			}
			else {
				hi = mid + 1;
			}
		}
		if (tuples[lo].getField(field2).compare(RelationalOperator.EQ, cmp)) {
			return lo;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * This method performs a join between this relation and a second relation.
	 * The resulting relation will contain all of the columns from both of the given relations,
	 * joined using the equality operator (=)
	 * @param other the relation to be joined
	 * @param field1 the field number (refer to TupleDesc) from this relation to be used in the join condition
	 * @param field2 the field number (refer to TupleDesc) from other to be used in the join condition
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Relation join(Relation other, int field1, int field2) {
		Comparator<Tuple> cmp = (a,b) -> {
			if (a.getField(field2).compare(RelationalOperator.GT, b.getField(field2))) {
				return 1;
			}
			else if (a.getField(field2).compare(RelationalOperator.LT, b.getField(field2))) {
				return -1;
			}
			else {
				return 0;
			}
		};
		
		ArrayList<Tuple> copy = (ArrayList<Tuple>)(other.tuples.clone());
		copy.sort(cmp);
		Tuple[] c = new Tuple[copy.size()];
		c = copy.toArray(c);
		
		ArrayList<Tuple> res = new ArrayList<>();
		Type [] types = new Type [td.numFields()+other.td.numFields()];
		String [] names = new String [td.numFields()+other.td.numFields()];
		for (int i = 0; i < td.numFields(); i++) {
			names[i] = td.getFieldName(i);
			types[i] = td.getType(i);
		}
		for (int i = 0; i < other.td.numFields(); i++) {
			names[td.numFields()+i] = other.td.getFieldName(i);
			types[td.numFields()+i] = other.td.getType(i);
		}

		TupleDesc tdsc = new TupleDesc(types, names);
		
		for (int i = 0; i < tuples.size(); i++) {
			Tuple t = tuples.get(i);
			int pos = bSearch(c, t, field1, field2);
			if (pos != -1) {
				do {
					System.out.println("on " + pos);
					Tuple tup = c[pos];
					Tuple tpl = new Tuple(tdsc);
					for (int j = 0; j < td.numFields(); j++) {
						tpl.setField(j, t.getField(j));
					}
					for (int j = 0; j < other.td.numFields(); j++) {
						tpl.setField(td.numFields()+j, tup.getField(j));
					}
					res.add(tpl);
					
					pos++;
				} while (pos < c.length &&
						c[pos].getField(field2).compare(RelationalOperator.EQ,
								t.getField(field1)));
			}
		}
		return new Relation(res, tdsc);
	}
	
	/**
	 * Performs an aggregation operation on a relation. See the lab write up for details.
	 * @param op the aggregation operation to be performed
	 * @param groupBy whether or not a grouping should be performed
	 * @return
	 */
	public Relation aggregate(AggregateOperator op, boolean groupBy) {
		Aggregator ag = new Aggregator(op, groupBy, td);
		for(Tuple t: tuples) {
			ag.merge(t);
		}
		return new Relation(ag.getResults(), ag.getTupleDesc());
	}
	
	public TupleDesc getDesc() {
		return td;
	}
	
	public ArrayList<Tuple> getTuples() {
		return tuples;
	}
	
	/**
	 * Returns a string representation of this relation. The string representation should
	 * first contain the TupleDesc, followed by each of the tuples in this relation
	 */
	public String toString() {
		return td + ": " + tuples;
	}
}
