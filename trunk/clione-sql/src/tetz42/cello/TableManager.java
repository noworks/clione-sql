package tetz42.cello;

import static tetz42.cello.CelloUtil.*;

import java.util.ArrayList;
import java.util.List;

import tetz42.cello.contents.Cell;
import tetz42.cello.contents.Row;
import tetz42.cello.header.Header;

public class TableManager<T> implements ITableManager {

	public static <T> TableManager<T> create(Class<T> clazz) {
		return new TableManager<T>(clazz);
	}

	private final Header<T> header;
	private final Context<T> context;
	private final RowHolder<T> rowHolder;
	private final RowHolder<T> tmpHolder;
	private final RowHolder<T> tailHolder;

	private TableManager(Class<T> clazz) {
		this.header = new Header<T>(clazz);
		this.context = header.getContext();
		this.rowHolder = new RowHolder<T>(clazz, context, true);
		this.tmpHolder = new RowHolder<T>(clazz, context);
		this.tailHolder = new RowHolder<T>(clazz, context);
	}

	public RowHolder<T> body() {
		return this.rowHolder;
	}

	public RowHolder<T> tmps() {
		return this.tmpHolder;
	}

	public RowHolder<T> tails() {
		return this.tailHolder;
	}

	public Row<T> newRow() {
		return body().newRow();
	}

	public Row<T> newRow(String key) {
		return body().newRow(key);
	}

	public Row<T> getRow() {
		return body().getRow();
	}

	public Row<T> getRow(String key) {
		return body().getRow(key);
	}

	public Row<T> row() {
		return body().row();
	}

	public Row<T> row(String key) {
		return body().row(key);
	}

	public Row<T> tmp() {
		return tmps().row();
	}

	public Row<T> tmp(String key) {
		return tmps().row(key);
	}

	public Row<T> tail() {
		return tails().row();
	}

	public Row<T> tail(String key) {
		return tails().row(key);
	}

	public void setCurrentRowAs(String key) {
		body().setCurrentRowAs(key);
	}

	public void appendAllTmpsToBody() {
		body().addRows(tmps().getRowList());
		tmps().clear();
	}

	public void appendTmpToBody(String key) {
		body().addRow(tmps().remove(key));
	}

	public <E> List<Cell<E>> getByQuery(
			@SuppressWarnings("unused") Class<E> clazz, String query) {
		return getByQuery(query);
	}

	public <E> List<Cell<E>> getByQuery(String query) {
		return getByQuery(Query.parse(query));
	}

	public <E> List<Cell<E>> getByQuery(
			@SuppressWarnings("unused") Class<E> clazz, Query query) {
		return getByQuery(query);
	}

	public <E> List<Cell<E>> getByQuery(Query query) {
		List<Cell<E>> list = rowHolder.getByQuery(query);
		List<Cell<E>> tmpList = tmpHolder.getByQuery(query);
		List<Cell<E>> tailList = tailHolder.getByQuery(query);
		list.addAll(tmpList);
		list.addAll(tailList);
		return list;
	}

	@Override
	public Header<T> header() {
		return this.header;
	}

	@Override
	public List<IRow> eachRow() {
		List<IRow> resultRows = new ArrayList<IRow>();
		addToRows(resultRows, rowHolder.getRowList());
		addToRows(resultRows, tmpHolder.getRowList());
		addToRows(resultRows, tailHolder.getRowList());
		return resultRows;
	}

	private void addToRows(List<IRow> resultRows, List<Row<T>> rows) {
		for (Row<T> row : rows) {
			if (!row.isRemoved())
				resultRows.add(row);
		}
	}

	public Row<T> def() {
		return this.context.getRowDef();
	}

	public void putConversion(String convertFrom, String convertTo) {
		putConversion("", convertFrom, convertTo);
	}

	public void putConversion(String schema, String convertFrom,
			String convertTo) {
		this.context.putConversion(schema, convertFrom, convertTo);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Iterable<ICell> it : header().each()) {
			appendCells(sb, it);
		}
		for (IRow row : eachRow()) {
			appendCells(sb, row.each());
		}
		return sb.toString();
	}

	private void appendCells(StringBuilder sb, Iterable<ICell> it) {
		for (ICell cell : it) {
			if (!cell.isSkipped()) {
				sb.append(cell.getValue()).append("(Style:").append(
						cell.getStyle()).append(")");
			}
			sb.append("\t");
		}
		sb.deleteCharAt(sb.length() - 1).append(CRLF);
	}

}
