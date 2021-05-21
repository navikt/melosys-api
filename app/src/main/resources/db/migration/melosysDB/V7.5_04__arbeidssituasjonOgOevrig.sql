DECLARE
    CURSOR bgs IS SELECT * FROM BEHANDLINGSGRUNNLAG WHERE "TYPE" = 'SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS' FOR UPDATE;
    jsonData JSON_OBJECT_T;
    oppdatertJsonData BEHANDLINGSGRUNNLAG.DATA%TYPE;

    p xmlparser.parser;
    doc xmldom.DOMDocument;
    XMLNode xmldom.DOMNode;

    arbeidssituasjonOgOevrig JSON_OBJECT_T;
    harLoennetArbeidMinstEnMndFoerUtsending VARCHAR(20);
    beskrivelseArbeidSisteMnd VARCHAR(3000);
    harAndreArbeidsgivereIUtsendingsperioden VARCHAR(20);
    beskrivelseAnnetArbeid VARCHAR(3000);
    erSkattepliktig VARCHAR(20);
    mottarYtelserNorge VARCHAR(20);
    mottarYtelserUtlandet VARCHAR(20);
BEGIN
    FOR bg IN bgs
        LOOP
            IF bg.ORIGINAL_DATA IS NOT NULL THEN
                p := xmlparser.newParser;
                xmlParser.parseClob(p, bg.ORIGINAL_DATA);
                doc := xmlparser.getDocument(p);
                XMLNode := xslprocessor.selectSingleNode(xmldom.makeNode(doc), '/MedlemskapArbeidEOSM/innhold/midlertidigUtsendt');

                harLoennetArbeidMinstEnMndFoerUtsending := xslprocessor.valueOf(XMLNode,'loennetArbeidMinstEnMnd');
                beskrivelseArbeidSisteMnd := xslprocessor.valueOf(XMLNode,'beskrivArbeidSisteMnd');
                harAndreArbeidsgivereIUtsendingsperioden := xslprocessor.valueOf(XMLNode,'andreArbeidsgivereIUtsendingsperioden');
                beskrivelseAnnetArbeid := xslprocessor.valueOf(XMLNode,'beskrivelseAnnetArbeid');
                erSkattepliktig := xslprocessor.valueOf(XMLNode,'skattepliktig');
                mottarYtelserNorge := xslprocessor.valueOf(XMLNode,'mottaYtelserNorge');
                mottarYtelserUtlandet := xslprocessor.valueOf(XMLNode,'mottaYtelserUtlandet');

                arbeidssituasjonOgOevrig := JSON_OBJECT_T('{' ||
                                                          '"harLoennetArbeidMinstEnMndFoerUtsending": null,' ||
                                                          '"beskrivelseArbeidSisteMnd": null,' ||
                                                          '"harAndreArbeidsgivereIUtsendingsperioden": null,' ||
                                                          '"beskrivelseAnnetArbeid": null,' ||
                                                          '"erSkattepliktig": null,' ||
                                                          '"mottarYtelserNorge": null,' ||
                                                          '"mottarYtelserUtlandet": null' ||
                                                          '}');

                IF beskrivelseArbeidSisteMnd IS NOT NULL THEN
                    arbeidssituasjonOgOevrig.put('beskrivelseArbeidSisteMnd', beskrivelseArbeidSisteMnd);
                END IF;
                IF beskrivelseAnnetArbeid IS NOT NULL THEN
                    arbeidssituasjonOgOevrig.put('beskrivelseAnnetArbeid', beskrivelseAnnetArbeid);
                END IF;
                IF harLoennetArbeidMinstEnMndFoerUtsending = 'true' THEN
                    arbeidssituasjonOgOevrig.put('harLoennetArbeidMinstEnMndFoerUtsending', true);
                ELSIF harLoennetArbeidMinstEnMndFoerUtsending = 'false' THEN
                    arbeidssituasjonOgOevrig.put('harLoennetArbeidMinstEnMndFoerUtsending', false);
                END IF;
                IF harAndreArbeidsgivereIUtsendingsperioden = 'true' THEN
                    arbeidssituasjonOgOevrig.put('harAndreArbeidsgivereIUtsendingsperioden', true);
                ELSIF harAndreArbeidsgivereIUtsendingsperioden = 'false' THEN
                    arbeidssituasjonOgOevrig.put('harAndreArbeidsgivereIUtsendingsperioden', false);
                END IF;
                IF erSkattepliktig = 'true' THEN
                    arbeidssituasjonOgOevrig.put('erSkattepliktig', true);
                ELSIF erSkattepliktig = 'false' THEN
                    arbeidssituasjonOgOevrig.put('erSkattepliktig', false);
                END IF;
                IF mottarYtelserNorge = 'true' THEN
                    arbeidssituasjonOgOevrig.put('mottarYtelserNorge', true);
                ELSIF mottarYtelserNorge = 'false' THEN
                    arbeidssituasjonOgOevrig.put('mottarYtelserNorge', false);
                END IF;
                IF mottarYtelserUtlandet = 'true' THEN
                    arbeidssituasjonOgOevrig.put('mottarYtelserUtlandet', true);
                ELSIF mottarYtelserUtlandet = 'false' THEN
                    arbeidssituasjonOgOevrig.put('mottarYtelserUtlandet', false);
                END IF;

                jsonData := TREAT(JSON_ELEMENT_T.parse(bg.data) AS JSON_OBJECT_T);
                jsonData.put('arbeidssituasjonOgOevrig', arbeidssituasjonOgOevrig);

                oppdatertJsonData := jsonData.stringify;
                UPDATE BEHANDLINGSGRUNNLAG SET DATA = oppdatertJsonData WHERE CURRENT OF bgs;
            END IF;
        END LOOP;
END;
/
