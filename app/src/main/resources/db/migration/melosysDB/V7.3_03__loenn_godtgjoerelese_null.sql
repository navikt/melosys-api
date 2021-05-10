DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG WHERE "TYPE" != 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS' FOR UPDATE;
    jsonData JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            jsonData.remove('arbeidsinntekt');
            jsonData.put_Null('loennOgGodtgjoerelse');
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
/