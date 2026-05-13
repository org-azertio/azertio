package org.myjtools.openbbt.persistence.execution;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.execution.ExecutionNodeStats;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestExecutionNode;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.util.UUIDGenerator;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JooqExecutionRepository implements TestExecutionRepository, AutoCloseable {

	private static final Table<Record> TABLE_EXECUTION = DSL.table("execution");
	private static final Table<Record> TABLE_EXECUTION_NODE = DSL.table("execution_node");
	private static final Table<Record> TABLE_EXECUTION_ATTACHMENT = DSL.table("execution_attachment");
	private static final Table<Record> TABLE_EXECUTION_NODE_STATS = DSL.table("execution_node_stats");

	private static final Field<UUID> FIELD_EXECUTION_ID = DSL.field("execution_id", UUID.class);
	private static final Field<UUID> FIELD_PLAN_ID = DSL.field("plan_id", UUID.class);
	private static final Field<LocalDateTime> FIELD_EXECUTED_AT = DSL.field("executed_at", LocalDateTime.class);

	private static final Field<UUID> FIELD_EXECUTION_NODE_ID = DSL.field("execution_node_id", UUID.class);
	private static final Field<UUID> FIELD_PLAN_NODE_ID = DSL.field("plan_node_id", UUID.class);
	private static final Field<LocalDateTime> FIELD_STARTED_AT = DSL.field("started_at", LocalDateTime.class);
	private static final Field<LocalDateTime> FIELD_FINISHED_AT = DSL.field("finished_at", LocalDateTime.class);
	private static final Field<Integer> FIELD_RESULT = DSL.field("result", Integer.class);
	private static final Field<String> FIELD_MESSAGE = DSL.field("message", String.class);
	private static final Field<Integer> FIELD_TEST_PASSED_COUNT = DSL.field("test_passed_count", Integer.class);
	private static final Field<Integer> FIELD_TEST_ERROR_COUNT = DSL.field("test_error_count", Integer.class);
	private static final Field<Integer> FIELD_TEST_FAILED_COUNT = DSL.field("test_failed_count", Integer.class);

	private static final Field<UUID> FIELD_ATTACHMENT_ID = DSL.field("attachment_id", UUID.class);
	private static final Field<String> FIELD_PROFILE = DSL.field("profile", String.class);

	private static final Field<Integer> FIELD_STATS_NUM_EXECUTIONS = DSL.field("num_executions", Integer.class);
	private static final Field<Integer> FIELD_STATS_NUM_THREADS    = DSL.field("num_threads",    Integer.class);
	private static final Field<Integer> FIELD_STATS_MIN_MS         = DSL.field("min_ms",         Integer.class);
	private static final Field<Integer> FIELD_STATS_MAX_MS         = DSL.field("max_ms",         Integer.class);
	private static final Field<Integer> FIELD_STATS_MEAN_MS        = DSL.field("mean_ms",        Integer.class);
	private static final Field<Integer> FIELD_STATS_P50_MS         = DSL.field("p50_ms",         Integer.class);
	private static final Field<Integer> FIELD_STATS_P95_MS         = DSL.field("p95_ms",         Integer.class);
	private static final Field<Integer> FIELD_STATS_P99_MS         = DSL.field("p99_ms",         Integer.class);
	private static final Field<Double>  FIELD_STATS_THROUGHPUT     = DSL.field("throughput",     Double.class);
	private static final Field<Double>  FIELD_STATS_ERROR_RATE     = DSL.field("error_rate",     Double.class);

	private final DSLContext dsl;
	private final Connection directConnection;


	public JooqExecutionRepository(DataSourceProvider dataSourceProvider) {
		this(dataSourceProvider.obtainDataSource(), dataSourceProvider.dialect());
	}

	public JooqExecutionRepository(DataSource dataSource, SQLDialect dialect) {
		this.dsl = DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
		this.directConnection = null;
	}

	public JooqExecutionRepository(Connection connection, SQLDialect dialect) {
		this.dsl = DSL.using(connection, dialect);
		this.directConnection = connection;
	}

	@Override
	public void close() {
		if (directConnection != null) {
			try { directConnection.close(); } catch (SQLException ignored) {}
		}
	}


	@Override
	public TestExecution newExecution(UUID planID, Instant executedAt, String profile) {
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_EXECUTION)
		   .set(FIELD_EXECUTION_ID, id)
		   .set(FIELD_PLAN_ID, planID)
		   .set(FIELD_EXECUTED_AT, LocalDateTime.ofInstant(executedAt, ZoneOffset.UTC))
		   .set(FIELD_PROFILE, profile)
		   .execute();
		TestExecution execution = new TestExecution();
		execution.executionID(id);
		execution.planID(planID);
		execution.executedAt(executedAt);
		execution.profile(profile);
		return execution;
	}


	@Override
	public UUID newExecutionNode(UUID executionID, UUID testPlanNodeID) {
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_EXECUTION_NODE)
		   .set(FIELD_EXECUTION_NODE_ID, id)
		   .set(FIELD_EXECUTION_ID, executionID)
		   .set(FIELD_PLAN_NODE_ID, testPlanNodeID)
		   .execute();
		return id;
	}


	@Override
	public Optional<UUID> getExecutionNodeByPlanNode(UUID executionID, UUID testPlanNodeID) {
		return dsl.select(FIELD_EXECUTION_NODE_ID)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_ID.eq(executionID))
			.and(FIELD_PLAN_NODE_ID.eq(testPlanNodeID))
			.fetchOptional(FIELD_EXECUTION_NODE_ID);
	}


	@Override
	public void updateExecutionNodeStart(UUID executionNodeID, Instant startedAt) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_STARTED_AT, LocalDateTime.ofInstant(startedAt, ZoneOffset.UTC))
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	@Override
	public void updateExecutionNodeFinish(UUID executionNodeID, ExecutionResult result, Instant finishedAt) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_RESULT, result.value())
		   .set(FIELD_FINISHED_AT, LocalDateTime.ofInstant(finishedAt, ZoneOffset.UTC))
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	@Override
	public void updateExecutionNodeTestCounts(UUID executionNodeID, int passed, int error, int failed) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_TEST_PASSED_COUNT, passed)
		   .set(FIELD_TEST_ERROR_COUNT, error)
		   .set(FIELD_TEST_FAILED_COUNT, failed)
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	@Override
	public void updateExecutionTestCounts(UUID executionID, int passed, int error, int failed) {
		dsl.update(TABLE_EXECUTION)
		   .set(FIELD_TEST_PASSED_COUNT, passed)
		   .set(FIELD_TEST_ERROR_COUNT, error)
		   .set(FIELD_TEST_FAILED_COUNT, failed)
		   .where(FIELD_EXECUTION_ID.eq(executionID))
		   .execute();
	}


	@Override
	public void updateExecutionNodeMessage(UUID executionNodeID, String message) {
		dsl.update(TABLE_EXECUTION_NODE)
		   .set(FIELD_MESSAGE, message)
		   .where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
		   .execute();
	}


	@Override
	public Optional<TestExecution> getExecution(UUID executionId) {
		return dsl.select(FIELD_EXECUTION_ID, FIELD_PLAN_ID, FIELD_EXECUTED_AT, FIELD_PROFILE,
				FIELD_TEST_PASSED_COUNT, FIELD_TEST_ERROR_COUNT, FIELD_TEST_FAILED_COUNT)
			.from(TABLE_EXECUTION)
			.where(FIELD_EXECUTION_ID.eq(executionId))
			.fetchOptional(rec -> {
				TestExecution ex = new TestExecution();
				ex.executionID(rec.value1());
				ex.planID(rec.value2());
				ex.executedAt(rec.value3().toInstant(ZoneOffset.UTC));
				ex.profile(rec.value4());
				ex.testPassedCount(rec.value5());
				ex.testErrorCount(rec.value6());
				ex.testFailedCount(rec.value7());
				return ex;
			});
	}


	@Override
	public List<TestExecution> listExecutions(UUID planID, UUID planNodeRoot, int offset, int max) {
		// Two-table JOIN: execution LEFT JOIN execution_node.
		// planNodeRoot is a parameter, so no cross-domain join to the plan table is needed.
		// Table-qualified column names avoid ambiguity on execution_id without aliases.
		var fExecId    = DSL.field("execution.execution_id",            UUID.class);
		var fEnExecId  = DSL.field("execution_node.execution_id",       UUID.class);
		var fEnNodeId  = DSL.field("execution_node.execution_node_id",  UUID.class);

		var fPassedCount = DSL.field("execution.test_passed_count", Integer.class);
		var fErrorCount  = DSL.field("execution.test_error_count",  Integer.class);
		var fFailedCount = DSL.field("execution.test_failed_count", Integer.class);
		var fProfile     = DSL.field("execution.profile",           String.class);

		var query = dsl
			.select(fExecId, FIELD_PLAN_ID, FIELD_EXECUTED_AT, fEnNodeId, fPassedCount, fErrorCount, fFailedCount, fProfile)
			.from(TABLE_EXECUTION)
			.leftJoin(TABLE_EXECUTION_NODE)
				.on(fEnExecId.eq(fExecId)
					.and(FIELD_PLAN_NODE_ID.eq(planNodeRoot)))
			.where(FIELD_PLAN_ID.eq(planID))
			.orderBy(FIELD_EXECUTED_AT.desc())
			.offset(offset);

		return (max > 0 ? query.limit(max) : query).fetch().map(rec -> {
			TestExecution ex = new TestExecution();
			ex.executionID(rec.value1());
			ex.planID(rec.value2());
			ex.executedAt(rec.value3().toInstant(ZoneOffset.UTC));
			ex.executionRootNodeID(rec.value4());
			ex.testPassedCount(rec.value5());
			ex.testErrorCount(rec.value6());
			ex.testFailedCount(rec.value7());
			ex.profile(rec.value8());
			return ex;
		});
	}

	@Override
	public Optional<TestExecutionNode> getExecutionNode(UUID executionID, UUID planNodeID) {
		return dsl.select(
				FIELD_EXECUTION_NODE_ID, FIELD_PLAN_NODE_ID,
				FIELD_STARTED_AT, FIELD_FINISHED_AT, FIELD_RESULT, FIELD_MESSAGE,
				FIELD_TEST_PASSED_COUNT, FIELD_TEST_ERROR_COUNT, FIELD_TEST_FAILED_COUNT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_ID.eq(executionID))
			.and(FIELD_PLAN_NODE_ID.eq(planNodeID))
			.fetchOptional(rec -> {
				TestExecutionNode node = new TestExecutionNode();
				node.executionID(executionID);
				node.executionNodeID(rec.value1());
				node.planNodeID(rec.value2());
				node.startTime(rec.value3() != null ? rec.value3().toInstant(java.time.ZoneOffset.UTC) : null);
				node.endTime(rec.value4() != null ? rec.value4().toInstant(java.time.ZoneOffset.UTC) : null);
				node.result(rec.value5() != null ? ExecutionResult.of(rec.value5()) : null);
				node.message(rec.value6());
				node.testPassedCount(rec.value7());
				node.testErrorCount(rec.value8());
				node.testFailedCount(rec.value9());
				return node;
			});
	}

	@Override
	public Optional<ExecutionResult> getExecutionNodeResult(UUID executionNodeID) {
		return dsl.select(FIELD_RESULT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_RESULT)
			.map(ExecutionResult::of);
	}

	public void clearAllData() {
		dsl.deleteFrom(TABLE_EXECUTION_ATTACHMENT).execute();
		dsl.deleteFrom(TABLE_EXECUTION_NODE).execute();
		dsl.deleteFrom(TABLE_EXECUTION).execute();
	}

	@Override
	public void deleteExecution(UUID executionId) {
		// EXECUTION_NODE_EXECUTION_FK and EXECUTION_ATTACHMENT_EXECUTION_FK are
		// ON DELETE CASCADE, so deleting the execution row is enough for the DB.
		dsl.deleteFrom(TABLE_EXECUTION)
		   .where(FIELD_EXECUTION_ID.eq(executionId))
		   .execute();
	}

	@Override
	public void deleteExecutionsByPlan(UUID planId) {
		// Same as deleteExecution: cascades handle nodes and attachment records.
		dsl.deleteFrom(TABLE_EXECUTION)
		   .where(FIELD_PLAN_ID.eq(planId))
		   .execute();
	}


	public Optional<Instant> getExecutionNodeStartedAt(UUID executionNodeID) {
		return dsl.select(FIELD_STARTED_AT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_STARTED_AT)
			.map(ldt -> ldt.toInstant(ZoneOffset.UTC));
	}


	public Optional<Instant> getExecutionNodeFinishedAt(UUID executionNodeID) {
		return dsl.select(FIELD_FINISHED_AT)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_FINISHED_AT)
			.map(ldt -> ldt.toInstant(ZoneOffset.UTC));
	}




	public Optional<String> getExecutionNodeMessage(UUID executionNodeID) {
		return dsl.select(FIELD_MESSAGE)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(FIELD_MESSAGE);
	}


	@Override
	public List<UUID> listAttachmentIds(UUID executionNodeID) {
		return dsl.select(FIELD_ATTACHMENT_ID)
			.from(TABLE_EXECUTION_ATTACHMENT)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetch(FIELD_ATTACHMENT_ID);
	}

	public boolean existsAttachment(UUID attachmentID) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_EXECUTION_ATTACHMENT)
			   .where(FIELD_ATTACHMENT_ID.eq(attachmentID))
		);
	}


	@Override
	public Optional<ExecutionNodeStats> getExecutionNodeStats(UUID executionNodeID) {
		return dsl.select(
				FIELD_STATS_NUM_EXECUTIONS, FIELD_STATS_NUM_THREADS,
				FIELD_STATS_MIN_MS, FIELD_STATS_MAX_MS, FIELD_STATS_MEAN_MS,
				FIELD_STATS_P50_MS, FIELD_STATS_P95_MS, FIELD_STATS_P99_MS,
				FIELD_STATS_THROUGHPUT, FIELD_STATS_ERROR_RATE)
			.from(TABLE_EXECUTION_NODE_STATS)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOptional(rec -> {
				ExecutionNodeStats stats = new ExecutionNodeStats();
				stats.numExecutions(rec.value1());
				stats.numThreads(rec.value2());
				stats.min(rec.value3());
				stats.max(rec.value4());
				stats.mean(rec.value5());
				stats.p50(rec.value6());
				stats.p95(rec.value7());
				stats.p99(rec.value8());
				stats.throughput(rec.value9());
				stats.errorRate(rec.value10());
				return stats;
			});
	}

	@Override
	public void storeExecutionNodeStats(UUID executionNodeID, ExecutionNodeStats stats) {
		dsl.insertInto(TABLE_EXECUTION_NODE_STATS)
		   .set(FIELD_EXECUTION_NODE_ID, executionNodeID)
		   .set(FIELD_STATS_NUM_EXECUTIONS, stats.numExecutions())
		   .set(FIELD_STATS_NUM_THREADS,    stats.numThreads())
		   .set(FIELD_STATS_MIN_MS,         stats.min())
		   .set(FIELD_STATS_MAX_MS,         stats.max())
		   .set(FIELD_STATS_MEAN_MS,        stats.mean())
		   .set(FIELD_STATS_P50_MS,         stats.p50())
		   .set(FIELD_STATS_P95_MS,         stats.p95())
		   .set(FIELD_STATS_P99_MS,         stats.p99())
		   .set(FIELD_STATS_THROUGHPUT,     stats.throughput())
		   .set(FIELD_STATS_ERROR_RATE,     stats.errorRate())
		   .onConflict(FIELD_EXECUTION_NODE_ID)
		   .doUpdate()
		   .set(FIELD_STATS_NUM_EXECUTIONS, stats.numExecutions())
		   .set(FIELD_STATS_NUM_THREADS,    stats.numThreads())
		   .set(FIELD_STATS_MIN_MS,         stats.min())
		   .set(FIELD_STATS_MAX_MS,         stats.max())
		   .set(FIELD_STATS_MEAN_MS,        stats.mean())
		   .set(FIELD_STATS_P50_MS,         stats.p50())
		   .set(FIELD_STATS_P95_MS,         stats.p95())
		   .set(FIELD_STATS_P99_MS,         stats.p99())
		   .set(FIELD_STATS_THROUGHPUT,     stats.throughput())
		   .set(FIELD_STATS_ERROR_RATE,     stats.errorRate())
		   .execute();
	}

	@Override
	public UUID newAttachment(UUID executionNodeID) {
		UUID executionID = dsl.select(FIELD_EXECUTION_ID)
			.from(TABLE_EXECUTION_NODE)
			.where(FIELD_EXECUTION_NODE_ID.eq(executionNodeID))
			.fetchOne(FIELD_EXECUTION_ID);
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_EXECUTION_ATTACHMENT)
		   .set(FIELD_ATTACHMENT_ID, id)
		   .set(FIELD_EXECUTION_NODE_ID, executionNodeID)
		   .set(FIELD_EXECUTION_ID, executionID)
		   .execute();
		return id;
	}

}