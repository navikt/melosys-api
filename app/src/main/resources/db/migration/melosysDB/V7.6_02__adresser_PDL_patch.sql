DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
    arbeidPaaLand JSON_OBJECT_T;
    fysiskeArbeidssteder JSON_ARRAY_T;
    fysiskArbeidssted JSON_OBJECT_T;
    fysiskArbeidsstedAdresse JSON_OBJECT_T;
    foretakUtlandArray JSON_ARRAY_T;
    foretakUtland JSON_OBJECT_T;
    foretakUtlandAdresse JSON_OBJECT_T;
    oppgittAdresse JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);

            arbeidPaaLand := jsonData.get_Object('arbeidPaaLand');
            IF arbeidPaaLand IS NOT NULL THEN
                fysiskeArbeidssteder := arbeidPaaLand.get_Array('fysiskeArbeidssteder');
                FOR i IN 0 .. fysiskeArbeidssteder.get_size - 1
                    LOOP
                        fysiskArbeidssted := TREAT(fysiskeArbeidssteder.get(i) AS JSON_OBJECT_T);
                        fysiskArbeidsstedAdresse := fysiskArbeidssted.get('adresse');
                        IF fysiskArbeidsstedAdresse.get_Object('husnummer') IS NULL AND
                           fysiskArbeidsstedAdresse.get('husnummer').isNull() THEN
                            fysiskArbeidsstedAdresse.rename_key('husnummer', 'husnummerEtasjeLeilighet');
                        END IF;
                    END LOOP;
            END IF;

            foretakUtlandArray := jsonData.get_Array('foretakUtland');
            FOR i IN 0 .. foretakUtlandArray.get_size - 1
                LOOP
                    foretakUtland := TREAT(foretakUtlandArray.get(i) AS JSON_OBJECT_T);
                    foretakUtlandAdresse := foretakUtland.get_Object('adresse');
                    IF foretakUtlandAdresse.get_Object('husnummer') IS NULL AND
                       foretakUtlandAdresse.get('husnummer').isNull() THEN
                        foretakUtlandAdresse.rename_key('husnummer', 'husnummerEtasjeLeilighet');
                    END IF;
                END LOOP;

            oppgittAdresse := jsonData.get_Object('bosted').get_Object('oppgittAdresse');
            IF oppgittAdresse.get_Object('husnummer') IS NULL AND
               oppgittAdresse.get_Object('husnummer').isNull() THEN
                oppgittAdresse.rename_key('husnummer', 'husnummerEtasjeLeilighet');
            END IF;

            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;

        END LOOP;
END;
/
