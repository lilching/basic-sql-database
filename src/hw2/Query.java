//Laney Ching & Katherine Zhou
package hw2;

import java.util.ArrayList;
import java.util.List;

import hw1.Catalog;
import hw1.Database;
import hw1.HeapFile;
import hw1.StringField;
import hw1.Tuple;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.ValuesList;

public class Query {

	private String q;
	
	public Query(String q) {
		this.q = q;
	}
	
	public Relation execute()  {
		Statement statement = null;
		try {
			statement = CCJSqlParserUtil.parse(q);
		} catch (JSQLParserException e) {
			System.out.println("Unable to parse query");
			e.printStackTrace();
		}
		Select selectStatement = (Select) statement;
		PlainSelect sb = (PlainSelect)selectStatement.getSelectBody();
		
		Catalog c = Database.getCatalog();
		Table t = (Table) sb.getFromItem();
		String tableName = t.getName();
		
		int tableId = c.getTableId(tableName);
		HeapFile f = c.getDbFile(tableId);
		Relation r = new Relation(f.getAllTuples(), f.getTupleDesc());
		
		if (sb.getJoins() != null) {
			for (Join j : sb.getJoins()) {
				WhereExpressionVisitor wev = new WhereExpressionVisitor();
				j.getOnExpression().accept(wev);
				
				Table rtable = (Table) j.getRightItem();
				HeapFile g = c.getDbFile(c.getTableId(rtable.getName()));
				Relation other = new Relation(g.getAllTuples(), g.getTupleDesc());

				String rex = wev.getRight().toString();
				String rName = rex.substring(0, rex.lastIndexOf('.'));
				String lCol, rCol;
				if (rName.equalsIgnoreCase(rtable.getName())) {
					rCol = rex.substring(rex.lastIndexOf('.') + 1);
					lCol = wev.getLeft();
				}
				else {
					lCol = rex.substring(rex.lastIndexOf('.') + 1);
					rCol = wev.getLeft();
				}
				
				r = r.join(other, r.getDesc().nameToId(lCol), other.getDesc().nameToId(rCol));
			}
		}
		
		if (sb.getWhere() != null) {
			WhereExpressionVisitor wev = new WhereExpressionVisitor();
			sb.getWhere().accept(wev);
			r = r.select(r.getDesc().nameToId(wev.getLeft()), wev.getOp(), wev.getRight());
		}
		
		ArrayList<Integer> cols = new ArrayList<>();
		boolean doAggregate = false;
		// bogus val to bypass compiler warning
		AggregateOperator op = AggregateOperator.SUM;
		for (SelectItem item : sb.getSelectItems()) {
			ColumnVisitor cv = new ColumnVisitor();
			item.accept(cv);
			if (cv.isAggregate()) {
				doAggregate = true;
				op = cv.getOp();
				if (cv.getColumn() == "*") {
					cols.add(0);
				}
				else {
					cols.add(r.getDesc().nameToId(cv.getColumn()));
				}
			}
			else {
				if (cv.getColumn() != "*") {
					cols.add(r.getDesc().nameToId(cv.getColumn()));
				}
			}
		}
		
		if (cols.size() > 0) {
			r = r.project(cols);
		}
		if (doAggregate) {
			r = r.aggregate(op, r.getDesc().numFields() > 1);
		}
		
		return r;
		
	}
}
