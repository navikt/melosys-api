DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
    maritimeArbeidssteder JSON_ARRAY_T;
    maritimtArbeidssted JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            maritimeArbeidssteder := TREAT(jsonData.get('maritimtArbeid') AS JSON_ARRAY_T);
            FOR i IN 0 .. maritimeArbeidssteder.get_size - 1
                LOOP
                    maritimtArbeidssted := TREAT(maritimeArbeidssteder.get(i) AS JSON_OBJECT_T);
                    maritimtArbeidssted.remove('foretakNavn');
                    maritimtArbeidssted.remove('foretakOrgnr');
                    maritimtArbeidssted.rename_key('installasjonsLandkode', 'innretningLandkode');
                    maritimtArbeidssted.put_Null('innretningstype');
                END LOOP;
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
