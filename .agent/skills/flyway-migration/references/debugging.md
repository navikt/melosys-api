# Flyway Migration Debugging Guide

## Common Issues

### 1. Migration Checksum Mismatch

**Symptom**: `FlywayException: Validate failed: Migration checksum mismatch`

**Cause**: Existing migration was modified after being applied

**Solution**:
```sql
-- Check flyway history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

-- If local development only, repair:
mvn flyway:repair -pl app

-- NEVER repair in production - fix the issue properly
```

### 2. Migration Already Applied

**Symptom**: `FlywayException: Found non-empty schema without schema history table`

**Cause**: Database has data but no flyway_schema_history

**Solution**:
```bash
# Baseline existing database
mvn flyway:baseline -pl app -Dflyway.baselineVersion=N
```

### 3. Migration Failed Partway

**Symptom**: Partially applied migration, subsequent runs fail

**Cause**: DDL failed mid-migration

**Debug**:
```sql
-- Check what was applied
SELECT * FROM flyway_schema_history WHERE success = 0;

-- Check table state
SELECT * FROM user_tables WHERE table_name = 'MY_TABLE';

-- Manual cleanup if needed (local only!)
DELETE FROM flyway_schema_history WHERE version = 'X.X';
```

### 4. ORA-00955: name is already used by an existing object

**Cause**: Table/index/constraint already exists

**Solution**: Check if object exists before creating:
```sql
-- Or use EXECUTE IMMEDIATE with exception handling
BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE ...';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN NULL; -- Already exists
        ELSE RAISE;
        END IF;
END;
/
```

### 5. ORA-02292: integrity constraint violated - child record found

**Cause**: Trying to drop/modify parent table with existing child records

**Solution**: Remove child records first or drop constraint:
```sql
-- Option 1: Delete children first
DELETE FROM child_table WHERE parent_id IN (SELECT id FROM parent_table WHERE ...);

-- Option 2: Disable constraint temporarily
ALTER TABLE child_table DISABLE CONSTRAINT fk_child_parent;
-- Do changes
ALTER TABLE child_table ENABLE CONSTRAINT fk_child_parent;
```

### 6. ORA-01430: column being added already exists

**Cause**: Column already exists in table

**Solution**:
```sql
-- Check existing columns
SELECT column_name FROM user_tab_columns WHERE table_name = 'MY_TABLE';

-- Add only if not exists
DECLARE
    column_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO column_exists
    FROM user_tab_columns
    WHERE table_name = 'MY_TABLE' AND column_name = 'MY_COLUMN';

    IF column_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE my_table ADD (my_column VARCHAR2(100))';
    END IF;
END;
/
```

## Debugging Commands

### Check Flyway Status

```bash
# Maven
mvn flyway:info -pl app

# Show pending migrations
mvn flyway:info -pl app | grep Pending
```

### Validate Migrations

```bash
mvn flyway:validate -pl app
```

### View Migration History

```sql
SELECT version, description, script, checksum, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

### Check Table Structure

```sql
-- All columns in a table
SELECT column_name, data_type, nullable, data_length
FROM user_tab_columns
WHERE table_name = 'MY_TABLE';

-- All constraints
SELECT constraint_name, constraint_type, status
FROM user_constraints
WHERE table_name = 'MY_TABLE';

-- All indexes
SELECT index_name, uniqueness, status
FROM user_indexes
WHERE table_name = 'MY_TABLE';
```

## Environment Variables

```bash
# Use local Oracle (required for ARM Macs)
export USE_LOCAL_DB=true

# Oracle image for ARM
export ORACLE_IMAGE=ghcr.io/navikt/melosys-legacy-avhengigheter/oracle-arm:19.3.0-ee-slim-faststart
export MELOSYS_ORACLE_DB_NAME=FREEPDB1

# Intel Oracle
export MELOSYS_ORACLE_DB_NAME=XEPDB1
```

## Local Development Workflow

### Fresh Database

```bash
# 1. Start local Oracle (from melosys-docker-compose)
docker-compose up -d oracle

# 2. Run migrations
make db-migrate
# or
mvn flyway:migrate -pl app
```

### Testing Migration Locally

```bash
# 1. Create migration file
touch app/src/main/resources/db/migration/melosysDB/V100__my_change.sql

# 2. Add SQL content

# 3. Run migration
mvn flyway:migrate -pl app

# 4. Verify
mvn flyway:info -pl app
```

### Rollback (Local Only!)

Flyway doesn't support automatic rollback. For local development:

```bash
# 1. Manual cleanup in SQL
DROP TABLE my_new_table;

# 2. Remove from history
DELETE FROM flyway_schema_history WHERE version = '100';

# 3. Delete migration file
rm app/src/main/resources/db/migration/melosysDB/V100__my_change.sql
```

## Common SQL Patterns

### Safe Column Addition

```sql
-- Add nullable column (always safe)
ALTER TABLE my_table ADD (new_column VARCHAR2(100) NULL);

-- Then update existing rows
UPDATE my_table SET new_column = 'default_value' WHERE new_column IS NULL;

-- Then add NOT NULL if needed
ALTER TABLE my_table MODIFY new_column NOT NULL;
```

### Safe Column Removal

```sql
-- 1. Set to unused (fast, no locks)
ALTER TABLE my_table SET UNUSED (old_column);

-- 2. Drop unused columns later (can be slow)
ALTER TABLE my_table DROP UNUSED COLUMNS;
```

### Safe Table Rename

```sql
RENAME old_table_name TO new_table_name;
```

## Related Skills

- **database**: Full schema documentation
- **testing**: Integration test database setup
