DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
    personOpplysninger JSON_OBJECT_T;
    medfolgendeFamilie JSON_ARRAY_T;
    familiemedlem JSON_OBJECT_T;
    uuid VARCHAR2(36);
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs LOOP
            jsonData := TREAT (JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            personOpplysninger := TREAT (jsonData.get('personOpplysninger') AS JSON_OBJECT_T);
            medfolgendeFamilie := TREAT (personOpplysninger.get('medfolgendeFamilie') AS JSON_ARRAY_T);
            FOR i IN 0 .. medfolgendeFamilie.get_size - 1 LOOP
                    familiemedlem := TREAT (medfolgendeFamilie.get(i) AS JSON_OBJECT_T);
                    SELECT regexp_replace(rawtohex(sys_guid()), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5') INTO uuid FROM dual;
                    familiemedlem.put('uuid', uuid);
                END LOOP;
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;