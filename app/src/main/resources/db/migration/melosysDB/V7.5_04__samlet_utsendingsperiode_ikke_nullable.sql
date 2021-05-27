DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG WHERE "TYPE" = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS' FOR UPDATE;
    jsonData                JSON_OBJECT_T;
    utenlandsoppdraget      JSON_OBJECT_T;
    samletUtsendingsperiode JSON_OBJECT_T;
    oppdatertJsonData       BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            utenlandsoppdraget := TREAT(jsonData.get('utenlandsoppdraget') AS JSON_OBJECT_T);
            IF (utenlandsoppdraget IS NULL) THEN
                CONTINUE;
            END IF;
            samletUtsendingsperiode := TREAT(utenlandsoppdraget.get('samletUtsendingsperiode') AS JSON_OBJECT_T);
            IF (samletUtsendingsperiode.IS_NULL) THEN
                utenlandsoppdraget.put('samletUtsendingsperiode', NEW JSON_OBJECT_T('{"fom":null,"tom":null}'));
            END IF;
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
