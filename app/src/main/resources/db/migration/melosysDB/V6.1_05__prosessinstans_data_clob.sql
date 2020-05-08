ALTER TABLE prosessinstans ADD (tmp_data CLOB);
UPDATE prosessinstans SET tmp_data = data;
UPDATE prosessinstans SET data = null;
ALTER TABLE prosessinstans MODIFY data LONG;
ALTER TABLE prosessinstans MODIFY data CLOB;
UPDATE prosessinstans SET data=tmp_data;
ALTER TABLE prosessinstans DROP COLUMN tmp_data;

-- Indexer må bygges på nytt
alter index pk_prosessinstans rebuild;
