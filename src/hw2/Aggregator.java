//Laney Ching & Katherine Zhou
package hw2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

import hw1.Field;
import hw1.IntField;
import hw1.RelationalOperator;
import hw1.Tuple;
import hw1.TupleDesc;
import hw1.Type;

/**
 * A class to perform various aggregations, by accepting one tuple at a time
 * @author Doug Shook
 *
 */
public class Aggregator {
	private AggregateOperator o;
	private boolean groupBy;
	private TupleDesc td;
	private TreeMap<Field, Accum> tm;
	private Accum importantVal;
	
	private class Accum {
		Field f;
		int count;
		AggregateOperator o;
		
		Accum(AggregateOperator o) {
			count = 0;
			this.o = o;
		}
		
		void add(Field g) {
			if (count == 0) {
				if (o == AggregateOperator.COUNT) {
					f = new IntField(1);
				}
				else {
					f = g;
				}
			}
			else {
				IntField n;
				switch(o) {
				case MAX:
					if (g.compare(RelationalOperator.GT, f)) {
						f = g;
					}
					break;
				case MIN:
					if (g.compare(RelationalOperator.LT, f)) {
						f = g;
					}
					break;
				case AVG:
				case SUM:
					assert(g.getType() == Type.INT);
					IntField i = (IntField) g;
					n = new IntField(i.getValue() + ((IntField) f).getValue());
					f = n;
					break;
				case COUNT:
					n = new IntField(1 + ((IntField) f).getValue());
					f = n;
					break;
				}
			}
			count++;
		}
		
		Field res() {
			switch (o){
			case COUNT:
			case MAX:
			case MIN:
			case SUM:
				return f;
			case AVG:
				return new IntField(((IntField) f).getValue()/count);
			}
			return null;
		}
	}

	public Aggregator(AggregateOperator o, boolean groupBy, TupleDesc td) {
		this.o = o;
		this.groupBy = groupBy;
		this.td = td;
		if (groupBy) {
			Comparator<Field> cmp = (a,b) -> {
				if (a.compare(RelationalOperator.GT, b)) {
					return 1;
				}
				else if (a.compare(RelationalOperator.LT, b)) {
					return -1;
				}
				else {
					return 0;
				}
			};
			tm = new TreeMap<>(cmp);
		}
		else {
			importantVal = new Accum(o);
		}
	}

	/**
	 * Merges the given tuple into the current aggregation
	 * @param t the tuple to be aggregated
	 */
	public void merge(Tuple t) {
		if (groupBy) {
			Accum acc = tm.getOrDefault(t.getField(0), new Accum(o));
			acc.add(t.getField(1));
			tm.put(t.getField(0), acc);
		}
		else {
			importantVal.add(t.getField(0));
		}
	}
	
	
	public TupleDesc getTupleDesc() {
		if (groupBy) {
			if (o == AggregateOperator.COUNT) {
				Type[] t = new Type[] {this.td.getType(0), Type.INT};
				String[] n = new String[] {this.td.getFieldName(0), this.td.getFieldName(1)};
				return new TupleDesc(t,n);
			}
			else {
				return this.td;
			}
		}
		else {
			if (o == AggregateOperator.COUNT) {
				Type[] t = new Type[] {Type.INT};
				String[] n = new String[] {this.td.getFieldName(0)};
				return new TupleDesc(t,n);
			}
			else {
				return this.td;
			}
		}
	}
	/**
	 * Returns the result of the aggregation
	 * @return a list containing the tuples after aggregation
	 */
	public ArrayList<Tuple> getResults() {
		TupleDesc td = getTupleDesc();
		if (groupBy) {
			ArrayList<Tuple> al = new ArrayList<>();
			tm.forEach((group, acc) -> {
				Tuple tup = new Tuple(td);
				tup.setField(0, group);
				tup.setField(1, acc.res());
				al.add(tup);
			});
			return al;
		}
		else {
			Tuple tup = new Tuple(td);
			tup.setField(0, importantVal.res());
			ArrayList<Tuple> l = new ArrayList<>();
			l.add(tup);
			return l;
		}
	}

}
