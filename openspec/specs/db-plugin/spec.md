# Database Plugin

Provides steps to execute SQL queries and assert database state against any JDBC datasource.
Supports multiple named datasources configured in `azertio.yaml`. Uses jOOQ internally
for query construction and result comparison.

---

## Datasource Setup

### Requirement: Named datasource alias selection
The plugin SHALL allow selecting a configured datasource alias for subsequent database steps.
The alias refers to an entry under `db.datasources` in the configuration.

#### Scenario: Alias selects active datasource
- **WHEN** `db.define.alias` is called with alias `"main"`
- **THEN** all subsequent database steps operate against the `main` datasource

### Requirement: Teardown SQL registration
The plugin SHALL allow registering one or more SQL statements to be executed at the end of
the scenario (teardown), regardless of whether the scenario passed or failed.
Multiple registrations SHALL be executed in registration order.

#### Scenario: Teardown SQL runs after a passing scenario
- **WHEN** `db.teardown.execute` registers `DELETE FROM orders WHERE test=true`
- **AND** the scenario passes
- **THEN** the DELETE is executed after the scenario completes

#### Scenario: Teardown SQL runs after a failing scenario
- **WHEN** `db.teardown.execute` is registered and the scenario fails mid-way
- **THEN** the teardown SQL is still executed

#### Scenario: Multiple teardown statements execute in order
- **WHEN** two teardown statements are registered
- **THEN** they execute sequentially in registration order

---

## Query Execution

### Requirement: Arbitrary SQL execution
The plugin SHALL execute any SQL statement provided as a docstring block and store the result
for subsequent assertion or extraction steps. For SELECT queries, the result set is stored.
For INSERT / UPDATE / DELETE, the number of affected rows is stored.

#### Scenario: SELECT result stored for assertion
- **WHEN** `db.execute.query` runs `SELECT * FROM users WHERE active=true`
- **THEN** the result set is available for `db.assert.query.*` steps

#### Scenario: DML affected row count stored
- **WHEN** `db.execute.query` runs `UPDATE users SET active=false WHERE id=1`
- **THEN** the affected row count (e.g. 1) is stored for `db.assert.query.count`

### Requirement: Store query result in variable
The plugin SHALL store the result of the last executed SQL statement in a named variable.
For SELECT, the value of the first field of the first row is stored.
For DML, the affected row count is stored.

#### Scenario: First cell of SELECT stored in variable
- **WHEN** `db.store.query.result` is called with variable name `"count"` after a `SELECT COUNT(*) FROM users`
- **THEN** `${count}` holds the count value as a string

---

## Assertions

### Requirement: Assert query result count
The plugin SHALL assert that the count resulting from the last executed SQL statement satisfies
an integer condition expression (e.g. `= 3`, `> 0`, `<= 10`).

#### Scenario: Row count matches condition
- **WHEN** a SELECT returns 5 rows and `db.assert.query.count` asserts `= 5`
- **THEN** the assertion passes

#### Scenario: Row count does not match condition
- **WHEN** a SELECT returns 3 rows and `db.assert.query.count` asserts `> 5`
- **THEN** the assertion fails with actual vs expected count

### Requirement: Assert table row count
The plugin SHALL assert that the total number of rows in a named table satisfies an integer
condition expression, without requiring an explicit query step beforehand.

#### Scenario: Table row count satisfies condition
- **WHEN** the `orders` table contains 10 rows and `db.assert.count` asserts `orders > 0`
- **THEN** the assertion passes

### Requirement: Assert table contains rows (subset)
The plugin SHALL assert that a database table contains all rows from a given data table (inline).
Additional rows in the database beyond those listed SHALL be accepted.
First row of the data table is the header (column names).

#### Scenario: Table contains expected subset
- **WHEN** the `users` table has rows for Alice and Bob and the assertion lists only Alice
- **THEN** `db.assert.table.contains` passes

#### Scenario: Expected row missing fails assertion
- **WHEN** the assertion lists a row not present in the table
- **THEN** the assertion fails identifying the missing row

### Requirement: Assert table matches exactly (full match)
The plugin SHALL assert that a database table's contents match the given data table exactly —
same rows, no extras. Both row count and individual row values MUST match.

#### Scenario: Exact table match passes
- **WHEN** the table has exactly the rows listed in the data table
- **THEN** `db.assert.table.is` passes

#### Scenario: Extra row in database fails exact match
- **WHEN** the table has one more row than listed in the data table
- **THEN** `db.assert.table.is` fails

### Requirement: Assert table contains rows from CSV (subset)
The plugin SHALL assert that a database table contains all rows from a CSV file.
The first row of the CSV is treated as the header. Additional rows in the database SHALL be accepted.

#### Scenario: CSV subset present in table
- **WHEN** the referenced CSV contains 3 rows and all 3 exist in the table
- **THEN** `db.assert.table.contains.csv` passes

### Requirement: Assert table matches CSV exactly (full match)
The plugin SHALL assert that a database table's contents match a CSV file exactly.
Both row count and individual values MUST match.

#### Scenario: Exact CSV match passes
- **WHEN** the table has exactly the rows in the CSV file
- **THEN** `db.assert.table.is.csv` passes

### Requirement: Assert multiple tables from Excel file
The plugin SHALL assert that the database contains the data from an Excel file where each
sheet represents a table (sheet name = table name) and the first row of each sheet is the header.
The assertion is a subset match per table (additional rows are accepted).

#### Scenario: All sheets match their respective tables
- **WHEN** an Excel file has sheets `users` and `orders` and all rows are present in the database
- **THEN** `db.assert.contains.xls` passes

#### Scenario: Missing row in one sheet fails assertion
- **WHEN** one row from any sheet is absent in the database
- **THEN** the assertion fails identifying the sheet and missing row

### Requirement: NULL value handling in assertions
The plugin SHALL treat cells matching the configured `db.nullValue` string as SQL NULL
when comparing rows, both in inline tables and in CSV/Excel files.

#### Scenario: Empty string cell treated as NULL by default
- **WHEN** `db.nullValue` is `""` (default) and a cell is empty in the expected data
- **THEN** the assertion compares against SQL NULL for that column

---

## Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `db.maxAssertRows` | integer | 100 | Maximum rows evaluated in table assertions |
| `db.nullValue` | text | `""` | String in expected data interpreted as SQL NULL |
| `db.datasources.<alias>.url` | text | required | JDBC connection URL |
| `db.datasources.<alias>.username` | text | required | Database username |
| `db.datasources.<alias>.password` | text | required | Database password |
| `db.datasources.<alias>.driver` | text | required | Fully-qualified JDBC driver class name |
| `db.datasources.<alias>.dialect` | text | required | jOOQ SQL dialect (e.g. `POSTGRES`, `MYSQL`, `H2`) |
| `db.datasources.<alias>.schema` | text | — | Default schema |
| `db.datasources.<alias>.catalog` | text | — | Default catalog |
| `db.datasources.<alias>.quoteIdentifiers` | boolean | `true` | Quote table/column names in generated SQL |