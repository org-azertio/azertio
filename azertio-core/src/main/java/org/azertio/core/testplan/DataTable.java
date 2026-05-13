package org.azertio.core.testplan;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import lombok.*;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Setter
@Getter
@EqualsAndHashCode
public final class DataTable implements NodeArgument {

	private List<List<String>> values;
	private int rows;
	private int columns;

	public DataTable() {
		this.values = List.of();
		this.rows  = 0;
		this.columns = 0;
	}


	public DataTable(List<List<String>> values) {
		if (values.stream().mapToInt(List::size).distinct().count() > 1) {
			throw new IllegalArgumentException("All rows must have the same size. Values were:\n" + values);
		}
		this.values = values;
		this.rows = values.size();
		this.columns = values.getFirst().size();
	}




	@Override
	public NodeArgument copy(UnaryOperator<String> replacingVariablesMethod) {
		return new DataTable(values.stream().map( row ->
				row.stream().map(replacingVariablesMethod).toList()
		).toList());
	}



	@Override
	public String toString() {
		return values.stream().map(row -> String.join("|", row)).collect(Collectors.joining("||"));
	}


	public static DataTable fromString(String representation) {
		if (representation == null) {
			return null;
		}
		List<List<String>> values = new LinkedList<>();
		for (String row : representation.split("\\|\\|")) {
			values.add(Arrays.asList(row.split("\\|")));
		}
		return new DataTable(values);
	}



}

