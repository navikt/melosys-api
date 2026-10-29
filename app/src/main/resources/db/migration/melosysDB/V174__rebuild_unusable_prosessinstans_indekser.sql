-- Bygger kun indekser som fortsatt er UNUSABLE
BEGIN
    FOR i IN (SELECT index_name
              FROM user_indexes
              WHERE table_name IN ('PROSESSINSTANS', 'PROSESSINSTANS_HENDELSER')
                AND index_type <> 'LOB'
                AND status = 'UNUSABLE')
    LOOP
        EXECUTE IMMEDIATE 'ALTER INDEX ' || i.index_name || ' REBUILD ONLINE';
    END LOOP;
END;
/
