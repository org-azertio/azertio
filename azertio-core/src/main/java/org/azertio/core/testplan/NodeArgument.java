package org.azertio.core.testplan;


import java.util.function.UnaryOperator;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public sealed interface NodeArgument permits DataTable, Document {

	NodeArgument copy(UnaryOperator<String> replacingVariablesMethod);

}
