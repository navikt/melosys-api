DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
    arbeidUtland JSON_ARRAY_T;
    arbeidPaaLand JSON_OBJECT_T;
    fysiskeArbeidssteder JSON_ARRAY_T;
    fysiskArbeidssted JSON_OBJECT_T;
    arbeidUtlandHjemmekontor BOOLEAN;
    erHjemmekontor BOOLEAN;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            jsonData.put('arbeidPaaLand', NEW JSON_OBJECT_T('{}'));
            arbeidUtland := TREAT(jsonData.get('arbeidUtland') AS JSON_ARRAY_T);
            arbeidPaaLand := TREAT(jsonData.get('arbeidPaaLand') AS JSON_OBJECT_T);
            arbeidPaaLand.put('fysiskeArbeidssteder', arbeidUtland);
            jsonData.remove('arbeidUtland');
            erHjemmekontor := false;
            fysiskeArbeidssteder := TREAT(arbeidPaaLand.get('fysiskeArbeidssteder') AS JSON_ARRAY_T);
            FOR i IN 0 .. fysiskeArbeidssteder.get_size - 1
                LOOP
                    fysiskArbeidssted := TREAT(fysiskeArbeidssteder.get(i) AS JSON_OBJECT_T);
                    fysiskArbeidssted.rename_key('foretakNavn', 'virksomhetNavn');
                    fysiskArbeidssted.remove('foretakOrgnr');
                    arbeidUtlandHjemmekontor := fysiskArbeidssted.get_Boolean('arbeidUtlandHjemmekontor');
                    IF arbeidUtlandHjemmekontor IS NOT NULL AND arbeidUtlandHjemmekontor THEN
                        erHjemmekontor := true;
                    END IF;
                    fysiskArbeidssted.remove('arbeidUtlandHjemmekontor');
                END LOOP;
            arbeidPaaland.put_Null('erFastArbeidssted');
            arbeidPaaland.put('erHjemmekontor', erHjemmekontor);
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
