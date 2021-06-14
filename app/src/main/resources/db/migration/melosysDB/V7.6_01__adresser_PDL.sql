DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
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

            fysiskeArbeidssteder := jsonData.get_Object('arbeidPaaLand').get_Array('fysiskeArbeidssteder');
            FOR i IN 0 .. fysiskeArbeidssteder.get_size - 1
                LOOP
                    fysiskArbeidssted := TREAT(fysiskeArbeidssteder.get(i) AS JSON_OBJECT_T);
                    fysiskArbeidsstedAdresse := fysiskArbeidssted.get_Object('adresse');
                    fysiskArbeidsstedAdresse.rename_key('husnummer', 'husnummerEtasjeLeilighet');
                    fysiskArbeidsstedAdresse.put_Null('postboks');
                    fysiskArbeidsstedAdresse.put_Null('tilleggsnavn');
                END LOOP;

            foretakUtlandArray := jsonData.get_Array('foretakUtland');
            FOR i IN 0 .. foretakUtlandArray.get_size - 1
                LOOP
                    foretakUtland := TREAT(foretakUtlandArray.get(i) AS JSON_OBJECT_T);
                    foretakUtlandAdresse := foretakUtland.get_Object('adresse');
                    foretakUtlandAdresse.rename_key('husnummer', 'husnummerEtasjeLeilighet');
                    foretakUtlandAdresse.put_Null('postboks');
                    foretakUtlandAdresse.put_Null('tilleggsnavn');
                END LOOP;

            oppgittAdresse := jsonData.get_Object('bosted').get_Object('oppgittAdresse');
            oppgittAdresse.rename_key('husnummer', 'husnummerEtasjeLeilighet');
            oppgittAdresse.put_Null('postboks');
            oppgittAdresse.put_Null('tilleggsnavn');

            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
/
