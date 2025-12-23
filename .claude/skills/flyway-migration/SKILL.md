---
name: flyway-migration
description: |
  Expert knowledge of Flyway database migrations in melosys-api (Oracle DB).
  Use when: (1) Creating new database migrations,
  (2) Understanding migration versioning and naming,
  (3) Debugging migration failures,
  (4) Understanding Oracle-specific SQL patterns,
  (5) Adding tables, columns, or indexes.
---

# Flyway Migration Skill

Expert knowledge of Flyway database migrations for melosys-api's Oracle database.

## Quick Reference

### Migration Location

```
app/src/main/resources/db/migration/melosysDB/
```

### Versioning Convention

| Pattern | Example | Use Case |
|---------|---------|----------|
| `V{N}__` | `V99__name.sql` | Major feature |
| `V{N}.{M}__` | `V4.4_01__name.sql` | Grouped related changes |
| `V{N}.{M}_{NN}__` | `V5.1_12__name.sql` | Sub-feature in group |

**Rules**:
- Use double underscore `__` after version
- Use descriptive snake_case names
- Never modify existing migrations
- Find latest version: `ls -la app/src/main/resources/db/migration/melosysDB/ | tail -5`

### Current Version Range

Versions span V1.0_01 through V99+. Check existing files before creating.

## Common Patterns

### Create Table

```sql
CREATE TABLE my_table (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    fagsak_id       VARCHAR2(99) NOT NULL,
    status          VARCHAR2(50) NOT NULL,
    registrert_dato TIMESTAMP NOT NULL,
    endret_dato     TIMESTAMP NOT NULL,
    CONSTRAINT pk_my_table PRIMARY KEY (id),
    CONSTRAINT fk_my_table_fagsak FOREIGN KEY (fagsak_id) REFERENCES fagsak
);

CREATE INDEX idx_my_table_fagsak ON my_table(fagsak_id);
```

### Create Lookup Table (Kodeverk)

```sql
CREATE TABLE my_type (
    kode VARCHAR2(50) NOT NULL,
    navn VARCHAR2(100) NOT NULL,
    CONSTRAINT pk_my_type PRIMARY KEY (kode)
);

INSERT INTO my_type(kode, navn) VALUES ('VALUE1', 'Description 1');
INSERT INTO my_type(kode, navn) VALUES ('VALUE2', 'Description 2');
```

### Add Column

```sql
ALTER TABLE behandling ADD (
    my_column VARCHAR2(100) NULL
);
```

### Add Column with Default

```sql
ALTER TABLE behandling ADD (
    my_flag NUMBER(1) DEFAULT 0 NOT NULL
);
```

### Add Foreign Key

```sql
ALTER TABLE my_table ADD CONSTRAINT fk_my_table_ref
    FOREIGN KEY (ref_id) REFERENCES other_table;
```

### Add Index

```sql
CREATE INDEX idx_my_table_column ON my_table(column_name);
CREATE UNIQUE INDEX idx_my_table_unique ON my_table(column1, column2);
```

### Insert Process Type/Step

```sql
INSERT INTO PROSESS_TYPE(KODE, NAVN)
VALUES ('MY_PROCESS', 'Description of process');

INSERT INTO PROSESS_STEG(kode, navn)
VALUES ('MY_STEP', 'Description of step');
```

### JSON Column (CLOB with constraint)

```sql
CREATE TABLE my_json_table (
    id   NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    data CLOB NOT NULL,
    CONSTRAINT pk_my_json PRIMARY KEY (id),
    CONSTRAINT json_my_data CHECK (data IS JSON) ENABLE
);
```

## Oracle-Specific Syntax

### Data Types

| Type | Usage |
|------|-------|
| `NUMBER(19)` | IDs, large integers |
| `NUMBER(1)` | Boolean flags |
| `VARCHAR2(N)` | Variable text |
| `CLOB` | Large text/JSON |
| `TIMESTAMP` | Date/time |
| `DATE` | Date only |

### Identity Columns

```sql
-- Auto-increment ID
id NUMBER(19) GENERATED ALWAYS AS IDENTITY
```

### Sequences

```sql
CREATE SEQUENCE my_seq
MINVALUE 1
NOMAXVALUE
INCREMENT BY 1;
```

## Configuration

### Maven Plugin (app/pom.xml)

```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <version>${flyway.version}</version>
</plugin>
```

### Profile Settings

| Profile | Flyway Enabled |
|---------|----------------|
| `local-mock` | Yes (default) |
| `local-q1` | No |
| `local-q2` | No |
| `nais` | Yes |

## Commands

### Check Migration Status

```bash
# Via Makefile
make db-info

# Via Maven
mvn flyway:info -pl app
```

### Run Migrations

```bash
# Via Makefile
make db-migrate

# Via Maven
mvn flyway:migrate -pl app
```

### Application Startup

Flyway runs automatically on application startup (unless disabled).

## Best Practices

1. **One logical change per migration**
2. **Test locally first** with `USE_LOCAL_DB=true`
3. **Never modify** existing migrations in production
4. **Use descriptive names** that explain the change
5. **Include indexes** for foreign keys and query columns
6. **Use NOT NULL** where possible for data integrity
7. **Add constraints** with meaningful names (pk_, fk_, idx_, chk_)

## Related Skills

- **database**: Schema structure and tables
- **kodeverk**: Lookup tables and enums
- **behandling**: Treatment tables
- **fagsak**: Case tables
