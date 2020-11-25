DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG FOR UPDATE;
    jsonData JSON_OBJECT_T;
    juridiskArbeidsgiverNorge JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;
BEGIN
    FOR bg IN bgs
        LOOP
            jsonData := TREAT (JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
            juridiskArbeidsgiverNorge := TREAT (jsonData.get('juridiskArbeidsgiverNorge') AS JSON_OBJECT_T);
            juridiskArbeidsgiverNorge.remove('utsendteNeste12Mnd');
            juridiskArbeidsgiverNorge.remove('arbeidstakereRekruttertILand');
            juridiskArbeidsgiverNorge.remove('oppdragsKontrakterIHovedsakInngaattILand');
            juridiskArbeidsgiverNorge.put_null('antallUtsendte');
            juridiskArbeidsgiverNorge.put_null('andelRekruttertINorge');
            oppdatertJsonData := jsonData.stringify;
            UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
        END LOOP;
END;
