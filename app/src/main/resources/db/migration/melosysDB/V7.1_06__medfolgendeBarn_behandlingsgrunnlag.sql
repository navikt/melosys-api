DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
    personOpplysninger JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT (JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            personOpplysninger := TREAT (jsonData.get('personOpplysninger') AS JSON_OBJECT_T);
            personOpplysninger.remove('medfolgendeFamilie');
            personOpplysninger.remove('medfolgendeAndre');
            personOpplysninger.put('medfolgendeBarn', json_array_t());
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
