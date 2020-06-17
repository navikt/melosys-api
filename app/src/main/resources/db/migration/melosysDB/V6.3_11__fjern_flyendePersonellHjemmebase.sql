DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG WHERE TYPE = 'SØKNAD' FOR UPDATE;
    jsonData JSON_OBJECT_T;
    arbeidNorge JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
    LOOP
        jsonData := TREAT (JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
        arbeidNorge := TREAT (jsonData.get('arbeidNorge') AS JSON_OBJECT_T);
        IF arbeidNorge IS NOT NULL THEN
            arbeidNorge.remove('flyendePersonellHjemmebase');
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END IF;
    END LOOP;
END;
