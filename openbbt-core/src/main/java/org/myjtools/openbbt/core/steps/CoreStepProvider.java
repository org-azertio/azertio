package org.myjtools.openbbt.core.steps;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.backend.ExecutionContext;
import org.myjtools.openbbt.core.contributors.StepExpression;
import org.myjtools.openbbt.core.contributors.StepProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Extension(
name = "Core Step Provider",
extensionPointVersion = "1.0",
scope = Scope.TRANSIENT // A new instance of this class will be created for each execution
)
public class CoreStepProvider implements StepProvider {

	@Inject
	DataTypes dataTypes;

	@Override
	public void init(Config config) {

	}


	@StepExpression(value = "assert.variable.integer", args = {"variable:id"})
	public void assertVariableInteger(String variable, Assertion assertion) {
		String varValue = ExecutionContext.current().getVariable(variable);
		Integer value = (Integer) dataTypes.byJavaType(Integer.class).parse(varValue);
		Assertion.assertThat(value, assertion);
	}

	@StepExpression(value = "assert.variable.decimal", args = {"variable:id"})
	public void assertVariableDecimal(String variable, Assertion assertion) {
		String varValue = ExecutionContext.current().getVariable(variable);
		BigDecimal value = (BigDecimal) dataTypes.byJavaType(BigDecimal.class).parse(varValue);
		Assertion.assertThat(value, assertion);
	}

	@StepExpression(value = "assert.variable.date", args = {"variable:id"})
	public void assertVariableDate(String variable, Assertion assertion) {
		String varValue = ExecutionContext.current().getVariable(variable);
		LocalDate value = (LocalDate) dataTypes.byJavaType(LocalDate.class).parse(varValue);
		Assertion.assertThat(value, assertion);
	}

	@StepExpression(value = "assert.variable.time", args = {"variable:id"})
	public void assertVariableTime(String variable, Assertion assertion) {
		String varValue = ExecutionContext.current().getVariable(variable);
		LocalTime value = (LocalTime) dataTypes.byJavaType(LocalTime.class).parse(varValue);
		Assertion.assertThat(value, assertion);
	}

	@StepExpression(value = "assert.variable.datetime", args = {"variable:id"})
	public void assertVariableDatetime(String variable, Assertion assertion) {
		String varValue = ExecutionContext.current().getVariable(variable);
		LocalDateTime value = (LocalDateTime) dataTypes.byJavaType(LocalDateTime.class).parse(varValue);
		Assertion.assertThat(value, assertion);
	}

	@StepExpression(value = "assert.variable.text", args = {"variable:id"})
	public void assertVariableText(String variable, Assertion assertion) {
		String varValue = ExecutionContext.current().getVariable(variable);
		String value = (String) dataTypes.byJavaType(String.class).parse(varValue);
		Assertion.assertThat(value, assertion);
	}

	@StepExpression(value = "enable.benchmark.mode", args = {"executions:integer","threads:integer"})
	public void enableBenchmarkMode(Integer executions, Integer threads) {
		ExecutionContext.current().enableBenchmarkMode(executions, threads);
	}

	@StepExpression(value = "assert.benchmark.statistics.mean")
	public void assertBenchmarkStatisticsMean(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().mean(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.min")
	public void assertBenchmarkStatisticsMin(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().min(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.max")
	public void assertBenchmarkStatisticsMax(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().max(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.p50")
	public void assertBenchmarkStatisticsP50(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().p50(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.p95")
	public void assertBenchmarkStatisticsP95(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().p95(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.p99")
	public void assertBenchmarkStatisticsP99(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().p99(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.throughput")
	public void assertBenchmarkStatisticsThroughput(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().throughput(), assertion);
	}

	@StepExpression(value = "assert.benchmark.statistics.errorRate")
	public void assertBenchmarkStatisticsErrorRate(Assertion assertion) {
		ExecutionContext ctx = ExecutionContext.current();
		Assertion.assertThat(ctx.lastBenchmarkStatistics().errorRate(), assertion);
	}

}
