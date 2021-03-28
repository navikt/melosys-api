DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG WHERE "TYPE" = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS' FOR UPDATE;
    jsonData JSON_OBJECT_T;
    juridiskArbeidsgiverNorge JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;

    p xmlparser.parser;
    doc xmldom.DOMDocument;
    XMLNode xmldom.DOMNode;
    erOfv VARCHAR(100);
BEGIN
    FOR bg IN bgs
        LOOP
            IF bg.ORIGINAL_DATA IS NOT NULL THEN
                p := xmlparser.newParser;
                xmlParser.parseClob(p, bg.ORIGINAL_DATA);
                doc := xmlparser.getDocument(p);
                XMLNode := xslprocessor.selectSingleNode(xmldom.makeNode(doc), '/MedlemskapArbeidEOSM/innhold/arbeidsgiver');
                erOfv := xslprocessor.valueOf(XMLNode,'offentligVirksomhet');

                jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
                juridiskArbeidsgiverNorge := jsonData.GET_OBJECT('juridiskArbeidsgiverNorge');
                IF erOfv = 'true' THEN
                    juridiskArbeidsgiverNorge.put('erOffentligVirksomhet', true);
                ELSIF erOfv = 'false' THEN
                    juridiskArbeidsgiverNorge.put('erOffentligVirksomhet', false);
                END IF;
                jsonData.put('juridiskArbeidsgiverNorge', juridiskArbeidsgiverNorge);

                oppdatertJsonData := jsonData.stringify;
                UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
            END IF;
        END LOOP;
END;
/
