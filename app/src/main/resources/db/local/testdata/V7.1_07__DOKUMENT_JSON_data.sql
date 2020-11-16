INSERT INTO SAKSOPPLYSNING (BEHANDLING_ID, OPPLYSNING_TYPE, VERSJON, KILDE, REGISTRERT_DATO, ENDRET_DATO, DOKUMENT_XML)
VALUES (1, 'SEDOPPL', '1', 'EESSI', sysdate, sysdate, XMLType('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 <sedDokument>
  <fnr>01017012345</fnr>
  <lovvalgBestemmelse>FO_883_2004_ART11_1</lovvalgBestemmelse>
  <bucType>LA_BUC_04</bucType>
  <erEndring>true</erEndring>
  <lovvalgslandKode>SE</lovvalgslandKode>
  <lovvalgsperiode>
    <fom>2020-01-01</fom>
    <tom>2020-12-31</tom>
  </lovvalgsperiode>
  <rinaDokumentID>427688f4799f83560d270eece298a2c7</rinaDokumentID>
  <rinaSaksnummer>12345</rinaSaksnummer>
  <sedType>A009</sedType>
  <statsborgerskapKoder>NO</statsborgerskapKoder>
</sedDokument>
'));

INSERT INTO SAKSOPPLYSNING (BEHANDLING_ID, OPPLYSNING_TYPE, VERSJON, KILDE, REGISTRERT_DATO, ENDRET_DATO, DOKUMENT_XML)
VALUES (2, 'SEDOPPL', '1', 'EESSI', sysdate, sysdate, XMLType('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
 <sedDokument>
  <lovvalgBestemmelse>FO_987_2009_ART14_11</lovvalgBestemmelse>
  <bucType>LA_BUC_04</bucType>
  <erEndring>true</erEndring>
  <lovvalgslandKode>DK</lovvalgslandKode>
  <lovvalgsperiode>
    <fom>2020-01-01</fom>
    <tom>2020-12-31</tom>
  </lovvalgsperiode>
  <rinaDokumentID>226f8d2d72f82bed8540c342ee1bc798</rinaDokumentID>
  <avsenderLandkode>PL</avsenderLandkode>
  <rinaSaksnummer>23456</rinaSaksnummer>
  <sedType>A009</sedType>
  <statsborgerskapKoder>NO</statsborgerskapKoder>
  <statsborgerskapKoder>SE</statsborgerskapKoder>
  <arbeidssteder>
    <navn>Et arbeidssted</navn>
    <hjemmebase>true</hjemmebase>
  </arbeidssteder>
  <arbeidssteder>
   <navn><![CDATA[Et <i>helt</i> annet arbeidssted]]></navn>
   <hjemmebase>false</hjemmebase>
  </arbeidssteder>
</sedDokument>
'));

UPDATE SAKSOPPLYSNING SET INTERN_XML = DOKUMENT_XML;
