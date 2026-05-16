package org.azertio.persistence.plan;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.azertio.core.AzertioException;
import org.azertio.core.persistence.TestPlanNodeCriteria;
import org.azertio.core.persistence.TestPlanRepository;
import org.azertio.core.testplan.*;
import org.azertio.core.util.UUIDGenerator;
import org.azertio.persistence.DataSourceProvider;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class JooqPlanRepository implements TestPlanRepository, AutoCloseable {

	private static final Table<Record> TABLE_PLAN_NODE = DSL.table("plan_node");
	private static final Table<Record> TABLE_PLAN_NODE_TAG = DSL.table("plan_node_tag");
	private static final Table<Record> TABLE_PLAN_NODE_PROPERTY = DSL.table("plan_node_property");
	private static final Table<Record> TABLE_PLAN = DSL.table("plan");
	private static final Table<Record> TABLE_PROJECT = DSL.table("project");

	private static final Field<UUID> FIELD_NODE_ID = DSL.field("node_id", UUID.class);
	private static final Field<UUID> FIELD_PARENT_NODE = DSL.field("parent_node", UUID.class);
	private static final Field<UUID> FIELD_PLAN_NODE = DSL.field("plan_node", UUID.class);
	private static final Field<String> FIELD_TAG = DSL.field("tag", String.class);
	private static final Field<String> FIELD_KEY = DSL.field("key", String.class);
	private static final Field<String> FIELD_VALUE = DSL.field("value", String.class);
	private static final Field<Integer> FIELD_NODE_POSITION = DSL.field("node_position", Integer.class);
	private static final Field<Integer> FIELD_TYPE = DSL.field("type", Integer.class);
	private static final Field<String> FIELD_NAME = DSL.field("name", String.class);
	private static final Field<String> FIELD_IDENTIFIER = DSL.field("identifier", String.class);
	private static final Field<String> FIELD_LANGUAGE = DSL.field("language", String.class);
	private static final Field<String> FIELD_SOURCE = DSL.field("source", String.class);
	private static final Field<String> FIELD_DISPLAY = DSL.field("display", String.class);
	private static final Field<String> FIELD_KEYWORD = DSL.field("keyword", String.class);
	private static final Field<String> FIELD_DESCRIPTION = DSL.field("description", String.class);
	private static final Field<String> FIELD_DATA_TABLE = DSL.field("data_table", String.class);
	private static final Field<String> FIELD_DOCUMENT = DSL.field("document", String.class);
	private static final Field<String> FIELD_DOCUMENT_MIME_TYPE = DSL.field("document_mime_type", String.class);

	private static final Field<UUID> FIELD_PLAN_ID = DSL.field("plan_id", UUID.class);
	private static final Field<Integer> FIELD_VALIDATION_STATUS = DSL.field("validation_status", Integer.class);
	private static final Field<String> FIELD_VALIDATION_MESSAGE = DSL.field("validation_message", String.class);
	private static final Field<Boolean> FIELD_HAS_ISSUES = DSL.field("has_issues", Boolean.class);
	private static final Field<UUID> FIELD_PROJECT_ID = DSL.field("project_id", UUID.class);
	private static final Field<String> FIELD_ORGANIZATION_NAME = DSL.field("organization_name", String.class);
	private static final Field<String> FIELD_PROJECT_NAME = DSL.field("project_name", String.class);
	private static final Field<LocalDateTime> FIELD_CREATED_AT = DSL.field("created_at", LocalDateTime.class);
	private static final Field<String> FIELD_RESOURCE_SET_HASH = DSL.field("resource_set_hash", String.class);
	private static final Field<String> FIELD_CONFIGURATION_HASH = DSL.field("configuration_hash", String.class);
	private static final Field<UUID> FIELD_PLAN_NODE_ROOT = DSL.field("plan_node_root", UUID.class);
	private static final Field<Integer> FIELD_TEST_CASE_COUNT = DSL.field("test_case_count", Integer.class);
	private static final Field<String> FIELD_SUITES = DSL.field("suites", String.class);

	private final DSLContext dsl;
	private final Connection directConnection;

	public JooqPlanRepository(DataSourceProvider dataSourceProvider) {
		this(dataSourceProvider.obtainDataSource(), dataSourceProvider.dialect());
	}

	public JooqPlanRepository(DataSource dataSource, SQLDialect dialect) {
		this.dsl = DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
		this.directConnection = null;
	}

	public JooqPlanRepository(Connection connection, SQLDialect dialect) {
		this.dsl = DSL.using(connection, dialect);
		this.directConnection = connection;
	}

	@Override
	public void close() {
		if (directConnection != null) {
			try { directConnection.close(); } catch (SQLException ignored) {}
		}
	}

	public void clearAllData() {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG).execute();
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY).execute();
		dsl.deleteFrom(TABLE_PLAN_NODE).execute();
	}


	public Optional<TestPlanNode> getNodeData(UUID id) {
		return dsl.select(
				FIELD_NODE_ID, FIELD_PARENT_NODE, FIELD_NODE_POSITION,
				FIELD_TYPE, FIELD_NAME, FIELD_IDENTIFIER, FIELD_LANGUAGE, FIELD_SOURCE,
				FIELD_KEYWORD, FIELD_DESCRIPTION, FIELD_DISPLAY, FIELD_DATA_TABLE,
				FIELD_DOCUMENT, FIELD_DOCUMENT_MIME_TYPE,
				FIELD_VALIDATION_STATUS, FIELD_VALIDATION_MESSAGE, FIELD_HAS_ISSUES,
				FIELD_TEST_CASE_COUNT
			)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id))
			.fetchOptional()
			.map(this::mapPlanNode);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void updateNodeField(UUID id, String fieldName, T fieldValue) {
		Field<T> field = (Field<T>) resolveField(fieldName);
		dsl.update(TABLE_PLAN_NODE)
		   .set(field, fieldValue)
		   .where(FIELD_NODE_ID.eq(id))
		   .execute();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getNodeField(UUID id, String fieldName) {
		Field<T> field = (Field<T>) resolveField(fieldName);
		return dsl.select(field)
		   .from(TABLE_PLAN_NODE)
		   .where(FIELD_NODE_ID.eq(id))
		   .fetchOptional(field);
	}



	public boolean existsNode(UUID id) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE)
			   .where(FIELD_NODE_ID.eq(id))
		);
	}


	public Optional<UUID> getParentNode(UUID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_PARENT_NODE)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_NODE_ID.eq(id))
			.fetchOptional()
			.map(record1 -> record1.get(FIELD_PARENT_NODE))
			;
	}


	public void deleteNode(UUID id) {
		// first detach from parent if exists
		getParentNode(id).ifPresent(parent -> detachChildNode(parent, id));
		// delete node (cascade should handle hierarchy, tags, properties)
		dsl.deleteFrom(TABLE_PLAN_NODE)
		   .where(FIELD_NODE_ID.eq(id))
		   .execute();
	}


	public void attachChildNodeLast(UUID parent, UUID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_PARENT_NODE, parent)
		   .set(FIELD_NODE_POSITION, maxNodePosition(parent) + 1)
		   .where(FIELD_NODE_ID.eq(child))
		   .execute();
	}


	public void attachChildNodeFirst(UUID parent, UUID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		// increment position of existing child nodes
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_NODE_POSITION, FIELD_NODE_POSITION.add(1))
			.where(FIELD_PARENT_NODE.eq(parent))
			.execute();
		// set child node as first
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_PARENT_NODE, parent)
			.set(FIELD_NODE_POSITION, 1)
			.where(FIELD_NODE_ID.eq(child))
			.execute();
	}


	public void detachChildNode(UUID parent, UUID child) {
		assertExistsNode(parent);
		assertExistsNode(child);
		dsl.update(TABLE_PLAN_NODE)
			.set(FIELD_PARENT_NODE, (UUID) null)
			.where(FIELD_NODE_ID.eq(child))
			.execute();
	}




	public Stream<UUID> getNodeChildren(UUID id) {
		assertExistsNode(id);
		return dsl.select(FIELD_NODE_ID)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(id))
			.orderBy(FIELD_NODE_POSITION)
			.fetch().stream()
			.map(record1 -> mapUUID(record1, FIELD_NODE_ID));
	}


	public int countNodeChildren(UUID id) {
		assertExistsNode(id);
		Integer count = dsl.selectCount()
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(id))
			.fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	private static final Table<?> CTE_DESC = DSL.table(DSL.unquotedName("descendants"));
	private static final Table<?> CTE_ANCS = DSL.table(DSL.unquotedName("ancestors"));
	private static final Field<UUID> CTE_NID = DSL.field(DSL.unquotedName("nid"), UUID.class);
	private static final Field<UUID> CTE_PID = DSL.field(DSL.unquotedName("pid"), UUID.class);

	public Stream<UUID> getNodeDescendants(UUID id) {
		assertExistsNode(id);
		return dsl.withRecursive(DSL.unquotedName("descendants"), DSL.unquotedName("nid")).as(
			   DSL.select(FIELD_NODE_ID)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_PARENT_NODE.eq(id))
			   .unionAll(
				   DSL.select(FIELD_NODE_ID)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_DESC)
					   .on(FIELD_PARENT_NODE.eq(CTE_NID))
			   )
		   )
		   .select(CTE_NID)
		   .from(CTE_DESC)
		   .fetch().stream()
		   .map(rec -> rec.get(CTE_NID));
	}


	@Override
	public Stream<UUID> getNodeDescendantsWithIssues(UUID rootNodeId) {
		var allNodes = DSL.unquotedName("all_nodes");
		var allNodesTable = DSL.table(allNodes);
		var nid = DSL.field(DSL.unquotedName("nid"), UUID.class);
		return dsl.withRecursive(allNodes, DSL.unquotedName("nid")).as(
			   DSL.select(FIELD_NODE_ID)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_NODE_ID.eq(rootNodeId))
			   .unionAll(
				   DSL.select(FIELD_NODE_ID)
					   .from(TABLE_PLAN_NODE)
					   .join(allNodesTable)
					   .on(FIELD_PARENT_NODE.eq(nid))
			   )
		   )
		   .select(FIELD_NODE_ID)
		   .from(TABLE_PLAN_NODE)
		   .where(FIELD_NODE_ID.in(DSL.select(nid).from(allNodesTable)))
		   .and(FIELD_VALIDATION_STATUS.isNotNull())
		   .and(FIELD_VALIDATION_STATUS.gt(ValidationStatus.OK.value))
		   .fetch().stream()
		   .map(rec -> rec.get(FIELD_NODE_ID));
	}


	public int countNodeDescendants(UUID id) {
		assertExistsNode(id);
		Integer count = dsl.withRecursive(DSL.unquotedName("descendants"), DSL.unquotedName("nid")).as(
			   DSL.select(FIELD_NODE_ID)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_PARENT_NODE.eq(id))
			   .unionAll(
				   DSL.select(FIELD_NODE_ID)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_DESC)
					   .on(FIELD_PARENT_NODE.eq(CTE_NID))
			   )
		   )
		   .selectCount()
		   .from(CTE_DESC)
		   .fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	public int countNodeAncestors(UUID id) {
		assertExistsNode(id);
		Integer count = dsl.withRecursive(DSL.unquotedName("ancestors"), DSL.unquotedName("pid")).as(
			   DSL.select(FIELD_PARENT_NODE)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_NODE_ID.eq(id))
			   .unionAll(
				   DSL.select(FIELD_PARENT_NODE)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_ANCS)
					   .on(FIELD_NODE_ID.eq(CTE_PID))
			   )
		   )
		   .selectCount()
		   .from(CTE_ANCS)
		   .where(CTE_PID.isNotNull())
		   .fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	public Stream<UUID> getNodeAncestors(UUID id) {
		assertExistsNode(id);
		return dsl.withRecursive(DSL.unquotedName("ancestors"), DSL.unquotedName("pid")).as(
			   DSL.select(FIELD_PARENT_NODE)
				   .from(TABLE_PLAN_NODE)
				   .where(FIELD_NODE_ID.eq(id))
			   .unionAll(
				   DSL.select(FIELD_PARENT_NODE)
					   .from(TABLE_PLAN_NODE)
					   .join(CTE_ANCS)
					   .on(FIELD_NODE_ID.eq(CTE_PID))
			   )
		   )
		   .select(CTE_PID)
		   .from(CTE_ANCS)
		   .where(CTE_PID.isNotNull())
		   .fetch().stream()
		   .map(rec -> rec.get(CTE_PID));
	}


	public UUID persistNode(TestPlanNode node) {
		boolean isUpdate = node.nodeID() != null;
		UUID id;
		if (isUpdate) {
			id = node.nodeID();
			assertExistsNode(id);
			updateNode(node);
		} else {
			id = UUIDGenerator.generateUUID();
			node.nodeID(id);
			insertNode(node);
		}
		syncTags(id, node.tags());
		syncProperties(id, node.properties());
		return id;
	}


	private void insertNode(TestPlanNode node) {
		dsl.insertInto(TABLE_PLAN_NODE)
		   .set(FIELD_NODE_ID, node.nodeID())
		   .set(FIELD_PARENT_NODE, (UUID) null)
		   .set(FIELD_NODE_POSITION, 1)
		   .set(FIELD_TYPE, node.nodeType() != null ? node.nodeType().value : null)
		   .set(FIELD_NAME, node.name())
		   .set(FIELD_IDENTIFIER, node.identifier())
		   .set(FIELD_LANGUAGE, node.language())
		   .set(FIELD_SOURCE, node.source())
		   .set(FIELD_KEYWORD, node.keyword())
		   .set(FIELD_DESCRIPTION, node.description())
		   .set(FIELD_DISPLAY, node.display())
		   .set(FIELD_DATA_TABLE, node.dataTable() != null ? node.dataTable().toString() : null)
		   .set(FIELD_DOCUMENT, node.document() != null ? node.document().content() : null)
		   .set(FIELD_DOCUMENT_MIME_TYPE, node.document() != null ? node.document().mimeType() : null)
		   .set(FIELD_TEST_CASE_COUNT, (Integer) null)
		   .execute();
	}


	private void updateNode(TestPlanNode node) {
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_TYPE, node.nodeType() != null ? node.nodeType().value : null)
		   .set(FIELD_NAME, node.name())
		   .set(FIELD_IDENTIFIER, node.identifier())
		   .set(FIELD_LANGUAGE, node.language())
		   .set(FIELD_SOURCE, node.source())
		   .set(FIELD_KEYWORD, node.keyword())
		   .set(FIELD_DESCRIPTION, node.description())
		   .set(FIELD_DISPLAY, node.display())
		   .set(FIELD_DATA_TABLE, node.dataTable() != null ? node.dataTable().toString() : null)
		   .set(FIELD_DOCUMENT, node.document() != null ? node.document().content() : null)
		   .set(FIELD_DOCUMENT_MIME_TYPE, node.document() != null ? node.document().mimeType() : null)
		   .set(FIELD_TEST_CASE_COUNT, node.testCaseCount())
		   .where(FIELD_NODE_ID.eq(node.nodeID()))
		   .execute();
	}


	private void syncTags(UUID id, Set<String> tags) {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(id))
		   .execute();

		if (tags == null || tags.isEmpty()) {
			return;
		}
		var batch = dsl.batch(
			dsl.insertInto(TABLE_PLAN_NODE_TAG, FIELD_PLAN_NODE, FIELD_TAG).values((UUID) null, (String) null)
		);
		tags.forEach(tag -> batch.bind(id, tag));
		batch.execute();
	}


	private void syncProperties(UUID id, SortedMap<String, String> properties) {
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY)
		   .where(FIELD_PLAN_NODE.eq(id))
		   .execute();
		if (properties == null || properties.isEmpty()) {
			return;
		}
		var batch = dsl.batch(
			dsl.insertInto(TABLE_PLAN_NODE_PROPERTY, FIELD_PLAN_NODE, FIELD_KEY, FIELD_VALUE)
			   .values((UUID) null, (String) null, (String) null)
		);
		for (var entry : properties.entrySet()) {
			batch.bind(id, entry.getKey(), entry.getValue());
		}
		batch.execute();
	}


	@Override
	public Stream<UUID> searchNodes(TestPlanNodeCriteria criteria) {
		Condition condition = buildCondition(criteria);
		return dsl.select(FIELD_NODE_ID).from(TABLE_PLAN_NODE)
			.where(condition)
			.orderBy(FIELD_NODE_ID)
			.fetch().stream()
			.map(rec1 -> mapUUID(rec1, FIELD_NODE_ID));
	}



	public int countNodes(TestPlanNodeCriteria criteria) {
		Condition condition = buildCondition(criteria);
		Integer count = dsl.selectCount().from(TABLE_PLAN_NODE)
				.where(condition)
				.fetchOne(0, int.class);
		return count != null ? count : 0;
	}


	@Override
	public boolean existsNodeTag(UUID nodeID, String tag) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE_TAG)
			   .where(FIELD_PLAN_NODE.eq(nodeID))
			   .and(FIELD_TAG.eq(tag))
		);
	}

	@Override
	public void addNodeTag(UUID nodeID, String tag) {
		dsl.insertInto(TABLE_PLAN_NODE_TAG)
		   .set(FIELD_PLAN_NODE, nodeID)
		   .set(FIELD_TAG, tag)
		   .execute();
	}

	@Override
	public void removeNodeTag(UUID nodeID, String tag) {
		dsl.deleteFrom(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(nodeID))
		   .and(FIELD_TAG.eq(tag))
		   .execute();
	}

	public List<String> getNodeTags(UUID nodeID) {
		return dsl.select(FIELD_TAG)
		   .from(TABLE_PLAN_NODE_TAG)
		   .where(FIELD_PLAN_NODE.eq(nodeID))
		   .fetch().stream()
		   .map(rec -> rec.get(FIELD_TAG)).toList();
	}

	@Override
	public boolean existsNodeProperty(UUID nodeID, String propertyKey, String propertyValue) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE_PROPERTY)
			   .where(FIELD_PLAN_NODE.eq(nodeID))
			   .and(FIELD_KEY.eq(propertyKey))
			   .and(propertyValue != null ? FIELD_VALUE.eq(propertyValue) : DSL.trueCondition())
		);
	}


	@Override
	public void addNodeProperty(UUID nodeID, String propertyKey, String propertyValue) {
		dsl.insertInto(TABLE_PLAN_NODE_PROPERTY)
		   .set(FIELD_PLAN_NODE, nodeID)
		   .set(FIELD_KEY, propertyKey)
		   .set(FIELD_VALUE, propertyValue)
		   .execute();
	}

	@Override
	public void removeNodeProperty(UUID nodeID, String propertyKey) {
		dsl.deleteFrom(TABLE_PLAN_NODE_PROPERTY)
		   .where(FIELD_PLAN_NODE.eq(nodeID))
		   .and(FIELD_KEY.eq(propertyKey))
		   .execute();
	}

	@Override
	public Optional<String> getNodeProperty(UUID nodeID, String propertyKey) {
		return dsl.select(FIELD_VALUE)
			.from(TABLE_PLAN_NODE_PROPERTY)
			.where(FIELD_PLAN_NODE.eq(nodeID))
			.and(FIELD_KEY.eq(propertyKey))
			.fetchOptional(FIELD_VALUE);
	}


	public Map<String, String> getNodeProperties(UUID nodeID) {
		return dsl.select(FIELD_KEY, FIELD_VALUE)
			.from(TABLE_PLAN_NODE_PROPERTY)
			.where(FIELD_PLAN_NODE.eq(nodeID))
			.fetch().stream()
			.collect(Collectors.toMap(
				rec -> rec.get(FIELD_KEY),
				rec -> rec.get(FIELD_VALUE)
			));
	}


	private Condition buildCondition(TestPlanNodeCriteria criteria) {
		return switch (criteria) {
			case TestPlanNodeCriteria.AllCriteria() -> DSL.trueCondition();

			case TestPlanNodeCriteria.HasTagCriteria(String tag) -> DSL.exists(
				DSL.selectOne()
					.from(TABLE_PLAN_NODE_TAG)
					.where(FIELD_PLAN_NODE.eq(FIELD_NODE_ID))
					.and(FIELD_TAG.eq(tag))
			);

			case TestPlanNodeCriteria.HasPropertyCriteria(String property, String value) -> DSL.exists(
				DSL.selectOne()
					.from(TABLE_PLAN_NODE_PROPERTY)
					.where(FIELD_PLAN_NODE.eq(FIELD_NODE_ID))
					.and(FIELD_KEY.eq(property))
					.and(value != null ? FIELD_VALUE.eq(value) : DSL.trueCondition())
			);

			case TestPlanNodeCriteria.HasNodeTypeCriteria(NodeType nodeType) ->
				FIELD_TYPE.eq(nodeType.value);

			case TestPlanNodeCriteria.HasFieldCriteria(String field, Object value) ->
				buildFieldCondition(field, value);

			case TestPlanNodeCriteria.HasValuedFieldCriteria(String field) ->
				resolveField(field).isNotNull();

			case TestPlanNodeCriteria.IsDescendantCriteria(UUID parent, int depth) ->
				buildDescendantCondition(parent, depth);

			case TestPlanNodeCriteria.IsAscendantCriteria(UUID child, int depth) ->
				buildAscendantCondition(child, depth);

			case TestPlanNodeCriteria.AndCriteria(TestPlanNodeCriteria[] conditions) -> {
				Condition result = DSL.trueCondition();
				for (TestPlanNodeCriteria c : conditions) {
					result = result.and(buildCondition(c));
				}
				yield result;
			}

			case TestPlanNodeCriteria.OrCriteria(TestPlanNodeCriteria[] conditions) -> {
				Condition result = DSL.falseCondition();
				for (TestPlanNodeCriteria c : conditions) {
					result = result.or(buildCondition(c));
				}
				yield result;
			}

			case TestPlanNodeCriteria.NotCriteria(TestPlanNodeCriteria condition) ->
				DSL.not(buildCondition(condition));
		};
	}


	private Field<?> resolveField(String fieldName) {
		return switch (fieldName.toLowerCase()) {
			case "name" -> FIELD_NAME;
			case "type", "nodetype" -> FIELD_TYPE;
			case "language" -> FIELD_LANGUAGE;
			case "source" -> FIELD_SOURCE;
			case "keyword" -> FIELD_KEYWORD;
			case "description" -> FIELD_DESCRIPTION;
			case "display" -> FIELD_DISPLAY;
			case "identifier" -> FIELD_IDENTIFIER;
			default -> throw new AzertioException("Unknown field: {}", fieldName);
		};
	}


	@SuppressWarnings("unchecked")
	private <T> Condition buildFieldCondition(String fieldName, T value) {
		Field<T> field = (Field<T>) resolveField(fieldName);
		return field.eq(value);
	}


	private Condition buildDescendantCondition(UUID parent, int depth) {
		if (depth == 1) {
			// Direct children only
			return FIELD_PARENT_NODE.eq(parent);
		} else {
			// All descendants (depth == -1) or up to certain depth
			var descendants = DSL.unquotedName("descendants");
			var descendantsTable = DSL.table(descendants);
			var dNodeId = DSL.field(DSL.unquotedName("descendants", "node_id"), UUID.class);
			var dDepth = DSL.field(DSL.unquotedName("descendants", "depth"), Integer.class);
			return FIELD_NODE_ID.in(
				DSL.withRecursive(descendants, DSL.unquotedName("node_id"), DSL.unquotedName("depth")).as(
					DSL.select(FIELD_NODE_ID, DSL.inline(1))
						.from(TABLE_PLAN_NODE)
						.where(FIELD_PARENT_NODE.eq(parent))
					.unionAll(
						DSL.select(FIELD_NODE_ID, dDepth.add(1))
							.from(TABLE_PLAN_NODE)
							.join(descendantsTable)
							.on(FIELD_PARENT_NODE.eq(dNodeId))
							.where(depth < 0 ? DSL.trueCondition() : dDepth.lt(depth))
					)
				)
				.select(dNodeId)
				.from(descendantsTable)
			);
		}
	}


	private Condition buildAscendantCondition(UUID child, int depth) {
		if (depth == 1) {
			// Direct parent only
			return FIELD_NODE_ID.in(
				DSL.select(FIELD_PARENT_NODE)
					.from(TABLE_PLAN_NODE)
					.where(FIELD_NODE_ID.eq(child))
			);
		} else {
			// All ancestors (depth == -1) or up to certain depth
			return FIELD_NODE_ID.in(
				DSL.withRecursive("ancestors", "node_id", "depth").as(
					DSL.select(FIELD_PARENT_NODE, DSL.inline(1))
						.from(TABLE_PLAN_NODE)
						.where(FIELD_NODE_ID.eq(child))
						.and(FIELD_PARENT_NODE.isNotNull())
					.unionAll(
						DSL.select(TABLE_PLAN_NODE.field(FIELD_PARENT_NODE), DSL.field("ancestors.depth", Integer.class).add(1))
							.from(TABLE_PLAN_NODE)
							.join(DSL.table("ancestors"))
							.on(TABLE_PLAN_NODE.field(FIELD_NODE_ID).eq(DSL.field("ancestors.node_id", UUID.class)))
							.where(TABLE_PLAN_NODE.field(FIELD_PARENT_NODE).isNotNull())
							.and(depth < 0 ? DSL.trueCondition() : DSL.field("ancestors.depth", Integer.class).lt(depth))
					)
				)
				.select(DSL.field("node_id", UUID.class))
				.from(DSL.table("ancestors"))
			);
		}
	}


	private UUID mapUUID(Record1<UUID> record1, Field<UUID> field) {
		return record1.get(field);
	}


	private TestPlanNode mapPlanNode(Record rec) {
		TestPlanNode node = new TestPlanNode();
		node.nodeID(rec.get(FIELD_NODE_ID));
		Integer typeValue = rec.get(FIELD_TYPE);
		if (typeValue != null) {
			node.nodeType(NodeType.of(typeValue));
		}
		node.name(rec.get(FIELD_NAME));
		node.language(rec.get(FIELD_LANGUAGE));
		node.identifier(rec.get(FIELD_IDENTIFIER));
		node.source(rec.get(FIELD_SOURCE));
		node.keyword(rec.get(FIELD_KEYWORD));
		node.description(rec.get(FIELD_DESCRIPTION));
		node.display(rec.get(FIELD_DISPLAY));
		String dataTableStr = rec.get(FIELD_DATA_TABLE);
		if (dataTableStr != null) {
			node.dataTable(DataTable.fromString(dataTableStr));
		}
		String documentContent = rec.get(FIELD_DOCUMENT);
		String documentMimeType = rec.get(FIELD_DOCUMENT_MIME_TYPE);
		if (documentContent != null) {
			node.document(Document.of(documentMimeType, documentContent));
		}
		Integer validationStatusValue = rec.get(FIELD_VALIDATION_STATUS);
		if (validationStatusValue != null) {
			node.validationStatus(ValidationStatus.of(validationStatusValue));
		}
		node.validationMessage(rec.get(FIELD_VALIDATION_MESSAGE));
		Boolean hasIssues = rec.get(FIELD_HAS_ISSUES);
		node.hasIssues(Boolean.TRUE.equals(hasIssues));
		node.testCaseCount(rec.get(FIELD_TEST_CASE_COUNT));
		fillTagsAndProperties(node);
		return node;
	}

	private void fillTagsAndProperties(TestPlanNode testPlanNode) {
		// fill tags
		Set<String> tags = new HashSet<>(dsl.select(FIELD_TAG)
            .from(TABLE_PLAN_NODE_TAG)
            .where(FIELD_PLAN_NODE.eq(testPlanNode.nodeID()))
            .fetch(FIELD_TAG));
		testPlanNode.tags(tags);
		// fill properties
		SortedMap<String, String> props = new TreeMap<>();
		dsl.select(FIELD_KEY, FIELD_VALUE)
            .from(TABLE_PLAN_NODE_PROPERTY)
            .where(FIELD_PLAN_NODE.eq(testPlanNode.nodeID()))
            .fetch()
            .forEach(rec -> props.put(rec.get(FIELD_KEY), rec.get(FIELD_VALUE)));
		testPlanNode.properties(props);
	}




	private Integer maxNodePosition(UUID planNodeID) {
		Integer maxPosition = dsl.select(DSL.max(FIELD_NODE_POSITION))
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PARENT_NODE.eq(planNodeID))
			.fetchOne(DSL.max(FIELD_NODE_POSITION));
		return maxPosition != null ? maxPosition : 0;
	}


	@Override
	public void setNodeValidation(UUID nodeId, ValidationStatus status, String message) {
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_VALIDATION_STATUS, status != null ? status.value : null)
		   .set(FIELD_VALIDATION_MESSAGE, message)
		   .where(FIELD_NODE_ID.eq(nodeId))
		   .execute();
	}


	@Override
	public void propagatePlanIssues(UUID planId) {
		// Step 1: initialize HAS_ISSUES from own VALIDATION_STATUS
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_HAS_ISSUES, DSL.inline(false))
		   .where(FIELD_PLAN_ID.eq(planId))
		   .execute();
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_HAS_ISSUES, DSL.inline(true))
		   .where(FIELD_PLAN_ID.eq(planId))
		   .and(FIELD_VALIDATION_STATUS.isNotNull())
		   .and(FIELD_VALIDATION_STATUS.gt(ValidationStatus.OK.value))
		   .execute();
		// Step 2: propagate upward — mark all ancestors of problematic nodes
		var anc = DSL.unquotedName("anc");
		var ancTable = DSL.table(anc);
		var pid = DSL.field(DSL.unquotedName("pid"), UUID.class);
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_HAS_ISSUES, DSL.inline(true))
		   .where(FIELD_PLAN_ID.eq(planId))
		   .and(FIELD_NODE_ID.in(
			   DSL.withRecursive(anc, DSL.unquotedName("pid")).as(
				   DSL.select(FIELD_PARENT_NODE)
					   .from(TABLE_PLAN_NODE)
					   .where(FIELD_PLAN_ID.eq(planId))
					   .and(FIELD_HAS_ISSUES.eq(DSL.inline(true)))
					   .and(FIELD_PARENT_NODE.isNotNull())
				   .unionAll(
					   DSL.select(FIELD_PARENT_NODE)
						   .from(TABLE_PLAN_NODE)
						   .join(ancTable)
						   .on(FIELD_NODE_ID.eq(pid))
						   .where(FIELD_PARENT_NODE.isNotNull())
				   )
			   )
			   .select(pid)
			   .from(ancTable)
			   .where(pid.isNotNull())
		   ))
		   .execute();
		// Force HSQLDB to flush committed changes to disk so a subsequent
		// JVM (e.g. 'azertio serve') sees the updated HAS_ISSUES values.
		if (dsl.dialect() == SQLDialect.HSQLDB) {
			dsl.execute("CHECKPOINT");
		}
	}


	@Override
	public boolean planHasIssues(UUID planId) {
		return dsl.fetchExists(
			dsl.selectOne()
			   .from(TABLE_PLAN_NODE)
			   .where(FIELD_PLAN_ID.eq(planId))
			   .and(FIELD_HAS_ISSUES.eq(DSL.inline(true)))
		);
	}


	@Override
	public void assignPlanToNodes(UUID planId, UUID rootNodeId) {
		var allNodes = DSL.unquotedName("all_nodes");
		var allNodesTable = DSL.table(allNodes);
		var nid = DSL.field(DSL.unquotedName("nid"), UUID.class);
		dsl.update(TABLE_PLAN_NODE)
		   .set(FIELD_PLAN_ID, planId)
		   .where(FIELD_NODE_ID.in(
			   DSL.withRecursive(allNodes, DSL.unquotedName("nid")).as(
				   DSL.select(FIELD_NODE_ID)
					   .from(TABLE_PLAN_NODE)
					   .where(FIELD_NODE_ID.eq(rootNodeId))
				   .unionAll(
					   DSL.select(FIELD_NODE_ID)
						   .from(TABLE_PLAN_NODE)
						   .join(allNodesTable)
						   .on(FIELD_PARENT_NODE.eq(nid))
				   )
			   )
			   .select(nid)
			   .from(allNodesTable)
		   ))
		   .execute();
	}


	@Override
	public void deletePlan(UUID planId) {
		// Deleting the plan record is enough: the schema cascades handle everything.
		// - EXECUTION_PLAN_FK ON DELETE CASCADE → removes executions, execution_nodes, execution_attachments
		// - PLAN_NODE_PLAN_FK ON DELETE SET NULL → clears plan_id on plan_nodes
		// - PLAN_PLAN_NODE_FK ON DELETE CASCADE → removes root plan_node, which cascades
		//   via fk_plan_node_parent to all children, tags and properties.
		dsl.deleteFrom(TABLE_PLAN)
		   .where(FIELD_PLAN_ID.eq(planId))
		   .execute();
	}


	private void assertExistsNode(UUID id) {
		if (id == null) {
			throw new AzertioException("Plan node ID is null!");
		}
		if (!existsNode(id)) {
			throw new AzertioException("Plan node {} not present in repository", id);
		}
	}


	@Override
	public UUID persistProject(TestProject testProject) {
		String organization = Objects.requireNonNullElse(testProject.organization(), "");
		Optional<UUID> existing = dsl.select(FIELD_PROJECT_ID)
			.from(TABLE_PROJECT)
			.where(FIELD_ORGANIZATION_NAME.eq(organization))
			.and(FIELD_PROJECT_NAME.eq(testProject.name()))
			.fetchOptional()
			.map(r -> r.get(FIELD_PROJECT_ID));
		if (existing.isPresent()) {
			dsl.update(TABLE_PROJECT)
				.set(FIELD_DESCRIPTION, testProject.description())
				.where(FIELD_PROJECT_ID.eq(existing.get()))
				.execute();
			return existing.get();
		}
		UUID id = UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_PROJECT)
			.set(FIELD_PROJECT_ID, id)
			.set(FIELD_ORGANIZATION_NAME, organization)
			.set(FIELD_PROJECT_NAME, testProject.name())
			.set(FIELD_DESCRIPTION, testProject.description())
			.execute();
		return id;
	}


	@Override
	public TestPlan persistPlan(TestPlan testPlan) {
		UUID id = testPlan.planID() != null ? testPlan.planID() : UUIDGenerator.generateUUID();
		dsl.insertInto(TABLE_PLAN)
			.set(FIELD_PLAN_ID, id)
			.set(FIELD_PROJECT_ID, testPlan.projectID())
			.set(FIELD_CREATED_AT, testPlan.createdAt().atOffset(ZoneOffset.UTC).toLocalDateTime())
			.set(FIELD_RESOURCE_SET_HASH, testPlan.resourceSetHash())
			.set(FIELD_CONFIGURATION_HASH, testPlan.configurationHash())
			.set(FIELD_PLAN_NODE_ROOT, testPlan.planNodeRoot())
			.set(FIELD_TEST_CASE_COUNT, testPlan.testCaseCount())
			.set(FIELD_SUITES, testPlan.suites())
			.execute();
		return new TestPlan(
			id,
			testPlan.projectID(),
			testPlan.createdAt(),
			testPlan.resourceSetHash(),
			testPlan.configurationHash(),
			testPlan.planNodeRoot(),
			testPlan.testCaseCount(),
			testPlan.suites()
		);
	}


	@Override
	public void assignTestCaseCountsToNodes(UUID planId) {
		record NodeRow(UUID nodeId, UUID parentId, int type) {}

		List<NodeRow> rows = dsl.select(FIELD_NODE_ID, FIELD_PARENT_NODE, FIELD_TYPE)
			.from(TABLE_PLAN_NODE)
			.where(FIELD_PLAN_ID.eq(planId))
			.fetch(r -> new NodeRow(r.get(FIELD_NODE_ID), r.get(FIELD_PARENT_NODE), r.get(FIELD_TYPE)));

		Map<UUID, List<UUID>> childrenMap = new HashMap<>();
		Map<UUID, Integer> typeMap = new HashMap<>();
		for (var row : rows) {
			typeMap.put(row.nodeId(), row.type());
			childrenMap.computeIfAbsent(row.nodeId(), k -> new ArrayList<>());
			if (row.parentId() != null) {
				childrenMap.computeIfAbsent(row.parentId(), k -> new ArrayList<>()).add(row.nodeId());
			}
		}

		Map<UUID, Integer> countMap = new HashMap<>();
		for (var entry : typeMap.entrySet()) {
			if (entry.getValue() != NodeType.TEST_CASE.value) {
				countMap.put(entry.getKey(), countDescendantTestCases(entry.getKey(), childrenMap, typeMap));
			}
		}

		if (!countMap.isEmpty()) {
			var batch = dsl.batch(
				dsl.update(TABLE_PLAN_NODE)
				   .set(FIELD_TEST_CASE_COUNT, DSL.param("count", Integer.class))
				   .where(FIELD_NODE_ID.eq(DSL.param("id", UUID.class)))
			);
			countMap.forEach((id, count) -> batch.bind(count, id));
			batch.execute();
		}
	}

	private int countDescendantTestCases(UUID nodeId, Map<UUID, List<UUID>> childrenMap, Map<UUID, Integer> typeMap) {
		int count = 0;
		for (UUID child : childrenMap.getOrDefault(nodeId, List.of())) {
			Integer childType = typeMap.get(child);
			if (childType != null && childType == NodeType.TEST_CASE.value) {
				count++;
			} else {
				count += countDescendantTestCases(child, childrenMap, typeMap);
			}
		}
		return count;
	}


	@Override
	public List<TestPlan> listPlans() {
		return dsl.select(
				FIELD_PLAN_ID, FIELD_PROJECT_ID, FIELD_CREATED_AT,
				FIELD_RESOURCE_SET_HASH, FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT,
				FIELD_TEST_CASE_COUNT, FIELD_SUITES
			)
			.from(TABLE_PLAN)
			.orderBy(FIELD_CREATED_AT.desc())
			.fetch()
			.map(this::mapPlan);
	}

	@Override
	public List<TestPlan> listPlans(String organization, String project, int offset, int max) {
		var query = dsl.select(
				FIELD_PLAN_ID, FIELD_PROJECT_ID, FIELD_CREATED_AT,
				FIELD_RESOURCE_SET_HASH, FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT,
				FIELD_TEST_CASE_COUNT, FIELD_SUITES
			)
			.from(TABLE_PLAN)
			.join(TABLE_PROJECT).using(FIELD_PROJECT_ID)
			.where(FIELD_ORGANIZATION_NAME.eq(organization))
			.and(FIELD_PROJECT_NAME.eq(project))
			.orderBy(FIELD_CREATED_AT.desc())
			.offset(offset);
		return (max > 0 ? query.limit(max) : query).fetch().map(this::mapPlan);
	}

	@Override
	public List<TestPlan> listPlans(String organization, String project, int offset, int max, boolean withExecutions) {
		if (!withExecutions) {
			return listPlans(organization, project, offset, max);
		}
		var existsExecution = DSL.exists(
			DSL.selectOne()
				.from(DSL.table("execution"))
				.where(DSL.field("plan_id", UUID.class).eq(DSL.field("plan.plan_id", UUID.class)))
		);
		var query = dsl.select(
				FIELD_PLAN_ID, FIELD_PROJECT_ID, FIELD_CREATED_AT,
				FIELD_RESOURCE_SET_HASH, FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT,
				FIELD_TEST_CASE_COUNT, FIELD_SUITES
			)
			.from(TABLE_PLAN)
			.join(TABLE_PROJECT).using(FIELD_PROJECT_ID)
			.where(FIELD_ORGANIZATION_NAME.eq(organization))
			.and(FIELD_PROJECT_NAME.eq(project))
			.and(existsExecution)
			.orderBy(FIELD_CREATED_AT.desc())
			.offset(offset);
		return (max > 0 ? query.limit(max) : query).fetch().map(this::mapPlan);
	}

	@Override
	public Optional<TestPlan> getPlan(TestProject testProject, String resourceSetHash, String configurationHash) {
		return dsl.select(FIELD_PROJECT_ID)
			.from(TABLE_PROJECT)
			.where(FIELD_ORGANIZATION_NAME.eq(Objects.requireNonNullElse(testProject.organization(), "")))
			.and(FIELD_PROJECT_NAME.eq(testProject.name()))
			.fetchOptional()
			.map(r -> r.get(FIELD_PROJECT_ID))
			.flatMap(projectID -> dsl.select(
					FIELD_PLAN_ID, FIELD_PROJECT_ID, FIELD_CREATED_AT,
					FIELD_RESOURCE_SET_HASH, FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT,
					FIELD_TEST_CASE_COUNT, FIELD_SUITES
				)
				.from(TABLE_PLAN)
				.where(FIELD_PROJECT_ID.eq(projectID))
				.and(FIELD_RESOURCE_SET_HASH.eq(resourceSetHash))
				.and(FIELD_CONFIGURATION_HASH.eq(configurationHash))
				.fetchOptional()
				.map(this::mapPlan)
			);
	}



	@Override
	public Optional<org.azertio.core.testplan.TestProject> getProject(UUID projectID) {
		return dsl.select(FIELD_ORGANIZATION_NAME, FIELD_PROJECT_NAME, FIELD_DESCRIPTION)
			.from(TABLE_PROJECT)
			.where(FIELD_PROJECT_ID.eq(projectID))
			.fetchOptional()
			.map(rec -> new org.azertio.core.testplan.TestProject(
				rec.get(FIELD_PROJECT_NAME),
				rec.get(FIELD_DESCRIPTION),
				rec.get(FIELD_ORGANIZATION_NAME),
				java.util.List.of()
			));
	}

	public Optional<TestPlan> getPlan(UUID planID) {
		return dsl.select(
				FIELD_PLAN_ID, FIELD_PROJECT_ID, FIELD_CREATED_AT,
				FIELD_RESOURCE_SET_HASH, FIELD_CONFIGURATION_HASH, FIELD_PLAN_NODE_ROOT,
				FIELD_TEST_CASE_COUNT, FIELD_SUITES
			)
			.from(TABLE_PLAN)
			.where(FIELD_PLAN_ID.eq(planID))
			.fetchOptional()
			.map(this::mapPlan);
	}

	private TestPlan mapPlan(org.jooq.Record rec) {
		Integer testCaseCount = rec.get(FIELD_TEST_CASE_COUNT);
		return new TestPlan(
			rec.get(FIELD_PLAN_ID),
			rec.get(FIELD_PROJECT_ID),
			rec.get(FIELD_CREATED_AT).toInstant(ZoneOffset.UTC),
			rec.get(FIELD_RESOURCE_SET_HASH),
			rec.get(FIELD_CONFIGURATION_HASH),
			rec.get(FIELD_PLAN_NODE_ROOT),
			testCaseCount != null ? testCaseCount : 0,
			rec.get(FIELD_SUITES)
		);
	}

}
