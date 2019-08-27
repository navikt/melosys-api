INSERT INTO FAGSAK (SAKSNUMMER, GSAK_SAKSNUMMER, FAGSAK_TYPE, STATUS, REGISTRERT_DATO, ENDRET_DATO)
VALUES('MELTEST-999', '321', 'EU_EOS', 'OPPRETTET', TIMESTAMP '2019-10-19 12:31:36.000000', SYSDATE);

Insert into BEHANDLING (SAKSNUMMER, STATUS, BEH_TYPE, REGISTRERT_DATO, ENDRET_DATO, REGISTRERT_AV)
    values ('MELTEST-999', 'OPPRETTET', 'SOEKNAD',
            to_timestamp('19.10.2017 14.35.54,000000000','DD.MM.RRRR HH24.MI.SSXFF'),
            to_timestamp('12.12.2018 11.59.10,000000000','DD.MM.RRRR HH24.MI.SSXFF'), 'K135703');

Insert into BEHANDLINGSRESULTAT (BEHANDLING_ID, BEHANDLINGSMAATE, RESULTAT_TYPE, FASTSATT_AV_LAND, VEDTAK_DATO,
                                 VEDTAK_KLAGEFRIST, REGISTRERT_DATO, REGISTRERT_AV, ENDRET_DATO,ENDRET_AV)
    values ((select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703'),
            'AUTOMATISERT','FASTSATT_LOVVALGSLAND', 'NO',
            to_timestamp('12.12.2018 12.16.42,372000000','DD.MM.RRRR HH24.MI.SSXFF'),
            to_date('23.01.2019','DD.MM.RRRR'),
            to_timestamp('12.12.2018 11.59.11,000000000','DD.MM.RRRR HH24.MI.SSXFF'), 'Hot-hot.. in a parking lot',
            to_timestamp('12.12.2018 12.16.42,469000000','DD.MM.RRRR HH24.MI.SSXFF'),'MELOSYS');

insert into lovvalg_periode(beh_resultat_id, fom_dato, tom_dato, LOVVALGSLAND,
                            LOVVALG_BESTEMMELSE, INNVILGELSE_RESULTAT,
                            MEDLEMSKAPSTYPE, trygde_dekning)
values (3, '13-NOV-92', '13-NOV-93', 'NO', 'FO_883_2004_ART12_1', 'INNVILGET',
        'FRIVILLIG', 'FULL_DEKNING_EOSFO');

insert into lovvalg_periode(beh_resultat_id, fom_dato, tom_dato, LOVVALGSLAND,
                            LOVVALG_BESTEMMELSE, INNVILGELSE_RESULTAT,
                            MEDLEMSKAPSTYPE, trygde_dekning)
    values ((select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703'),
            '13-NOV-2017', '13-NOV-2018', 'NO', 'FO_883_2004_ART12_1', 'INNVILGET',
            'FRIVILLIG', 'FULL_DEKNING_EOSFO');


insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
values (3, 'VIRKSOMHET', '123456789', 'TRUE', 'VIRKSOMHET');

insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
    values ((select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703'),
            'VIRKSOMHET', '819731322', 'TRUE', 'VIRKSOMHET');

insert into AVKLARTEFAKTA (BEH_RESULTAT_ID,TYPE,SUBJEKT,FAKTA,REFERANSE)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703'),
            'VIRKSOMHET', '982683955', 'TRUE', 'VIRKSOMHET');

Insert into SAKSOPPLYSNING (BEHANDLING_ID,OPPLYSNING_TYPE,VERSJON,KILDE,REGISTRERT_DATO,ENDRET_DATO,DOKUMENT_XML,INTERN_XML)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703'),
        'SØKNAD', '1.0', 'SBH', to_timestamp('12.12.2018 11.59.11,002000000','DD.MM.RRRR HH24.MI.SSXFF'),
        to_timestamp('12.12.2018 11.59.11,002000000','DD.MM.RRRR HH24.MI.SSXFF'),
        '<?xml version="1.0" encoding="UTF-8" standalone=''yes''?>
<soeknadDokument>
  <periode>
    <fom>2018-01-01</fom>
    <tom>2018-12-01</tom>
  </periode>
  <soeknadsland>
    <landkoder>AT</landkoder>
  </soeknadsland>
  <personOpplysninger>
    <medfolgendeAndre>99999999994</medfolgendeAndre>
  </personOpplysninger>
  <oppholdUtland>
    <oppholdslandkoder>AT</oppholdslandkoder>
    <oppholdsPeriode>
      <fom>2018-01-01</fom>
      <tom>2018-12-01</tom>
    </oppholdsPeriode>
    <ektefelleEllerBarnINorge>false</ektefelleEllerBarnINorge>
    <sammeAdresseSomArbeidsgiver>false</sammeAdresseSomArbeidsgiver>
  </oppholdUtland>
  <arbeidNorge/>
  <arbeidUtland>
    <foretakNavn>Equinor</foretakNavn>
     <adresse>
      <gatenavn>NEDRE VARÅSEN</gatenavn>
      <husnummer>1</husnummer>
      <postnummer>0666</postnummer>
      <poststed>Oslo</poststed>
      <region>Akershus</region>
      <landkode>NO</landkode>
    </adresse>
  </arbeidUtland>
  <foretakUtland>
    <navn>Jarlsberg International</navn>
    <adresse>
      <gatenavn>NEDRE VARÅSEN</gatenavn>
      <husnummer>1</husnummer>
      <postnummer>0666</postnummer>
      <poststed>Oslo</poststed>
      <region>Akershus</region>
      <landkode>NO</landkode>
    </adresse>
  </foretakUtland>
  <selvstendigArbeid>
    <erSelvstendig>false</erSelvstendig>
  </selvstendigArbeid>
  <juridiskArbeidsgiverNorge>
    <utsendteNeste12Mnd>0</utsendteNeste12Mnd>
    <antallAdmAnsatte>0</antallAdmAnsatte>
    <antallAnsatte>0</antallAnsatte>
    <arbeidstakereRekruttertILand>SE</arbeidstakereRekruttertILand>
    <oppdragsKontrakterIHovedsakInngaattILand>NO</oppdragsKontrakterIHovedsakInngaattILand>
    <ekstraArbeidsgivere>982683955</ekstraArbeidsgivere>
  </juridiskArbeidsgiverNorge>
  <arbeidsinntekt>
    <inntektNorskIPerioden>0</inntektNorskIPerioden>
    <inntektUtenlandskIPerioden>0</inntektUtenlandskIPerioden>
    <inntektNaeringIPerioden>0</inntektNaeringIPerioden>
    <inntektErInnrapporteringspliktig>false</inntektErInnrapporteringspliktig>
    <inntektTrygdeavgiftBlirTrukket>false</inntektTrygdeavgiftBlirTrukket>
  </arbeidsinntekt>
  <arbeidsgiversBekreftelse>
    <arbeidsgiverBekrefterUtsendelse>false</arbeidsgiverBekrefterUtsendelse>
    <arbeidstakerAnsattUnderUtsendelsen>false</arbeidstakerAnsattUnderUtsendelsen>
    <erstatterArbeidstakerenUtsendte>false</erstatterArbeidstakerenUtsendte>
    <arbeidstakerTidligereUtsendt24Mnd>false</arbeidstakerTidligereUtsendt24Mnd>
    <arbeidsgiverBetalerArbeidsgiveravgift>false</arbeidsgiverBetalerArbeidsgiveravgift>
    <trygdeavgiftTrukketGjennomSkatt>false</trygdeavgiftTrukketGjennomSkatt>
  </arbeidsgiversBekreftelse>
  <maritimtArbeid/>
  <bosted>
    <intensjonOmRetur>false</intensjonOmRetur>
    <antallMaanederINorge>0</antallMaanederINorge>
    <adresseIUtlandet>false</adresseIUtlandet>
    <oppgittAdresse/>
  </bosted>
</soeknadDokument>
',

'<?xml version="1.0" encoding="UTF-8" standalone=''yes''?>
<soeknadDokument>
  <periode>
    <fom>2018-01-01</fom>
    <tom>2018-12-01</tom>
  </periode>
  <soeknadsland>
    <landkoder>AT</landkoder>
  </soeknadsland>
    <personOpplysninger>
    <medfolgendeAndre>99999999994</medfolgendeAndre>
  </personOpplysninger>
  <oppholdUtland>
    <oppholdslandkoder>AT</oppholdslandkoder>
    <oppholdsPeriode>
      <fom>2018-01-01</fom>
      <tom>2018-12-01</tom>
    </oppholdsPeriode>
    <ektefelleEllerBarnINorge>false</ektefelleEllerBarnINorge>
    <sammeAdresseSomArbeidsgiver>false</sammeAdresseSomArbeidsgiver>
  </oppholdUtland>
  <arbeidNorge/>
  <arbeidUtland>
    <foretakNavn>Equinor</foretakNavn>
    <adresse>
      <gatenavn>NEDRE VARÅSEN</gatenavn>
      <husnummer>1</husnummer>
      <postnummer>0666</postnummer>
      <poststed>Oslo</poststed>
      <region>Akershus</region>
      <landkode>NO</landkode>
    </adresse>
  </arbeidUtland>
  <foretakUtland>
    <navn>Jarlsberg International</navn>
    <adresse>
      <gatenavn>NEDRE VARÅSEN</gatenavn>
      <husnummer>1</husnummer>
      <postnummer>0666</postnummer>
      <region/>
      <landkode>NO</landkode>
    </adresse>
  </foretakUtland>
  <selvstendigArbeid>
    <erSelvstendig>false</erSelvstendig>
  </selvstendigArbeid>
  <juridiskArbeidsgiverNorge>
    <utsendteNeste12Mnd>0</utsendteNeste12Mnd>
    <antallAdmAnsatte>0</antallAdmAnsatte>
    <antallAnsatte>0</antallAnsatte>
    <arbeidstakereRekruttertILand>SE</arbeidstakereRekruttertILand>
    <oppdragsKontrakterIHovedsakInngaattILand>NO</oppdragsKontrakterIHovedsakInngaattILand>
    <ekstraArbeidsgivere>982683955</ekstraArbeidsgivere>
  </juridiskArbeidsgiverNorge>
  <arbeidsinntekt>
    <inntektNorskIPerioden>0</inntektNorskIPerioden>
    <inntektUtenlandskIPerioden>0</inntektUtenlandskIPerioden>
    <inntektNaeringIPerioden>0</inntektNaeringIPerioden>
    <inntektErInnrapporteringspliktig>false</inntektErInnrapporteringspliktig>
    <inntektTrygdeavgiftBlirTrukket>false</inntektTrygdeavgiftBlirTrukket>
  </arbeidsinntekt>
  <arbeidsgiversBekreftelse>
    <arbeidsgiverBekrefterUtsendelse>false</arbeidsgiverBekrefterUtsendelse>
    <arbeidstakerAnsattUnderUtsendelsen>false</arbeidstakerAnsattUnderUtsendelsen>
    <erstatterArbeidstakerenUtsendte>false</erstatterArbeidstakerenUtsendte>
    <arbeidstakerTidligereUtsendt24Mnd>false</arbeidstakerTidligereUtsendt24Mnd>
    <arbeidsgiverBetalerArbeidsgiveravgift>false</arbeidsgiverBetalerArbeidsgiveravgift>
    <trygdeavgiftTrukketGjennomSkatt>false</trygdeavgiftTrukketGjennomSkatt>
  </arbeidsgiversBekreftelse>
  <maritimtArbeid/>
  <bosted>
    <intensjonOmRetur>false</intensjonOmRetur>
    <antallMaanederINorge>0</antallMaanederINorge>
    <adresseIUtlandet>false</adresseIUtlandet>
    <oppgittAdresse/>
  </bosted>
</soeknadDokument>
');

insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
    values ((select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703'),
            'YRKESGRUPPE', null, 'ORDINAER', 'YRKESGRUPPE');

Insert into SAKSOPPLYSNING (BEHANDLING_ID,OPPLYSNING_TYPE,VERSJON,KILDE,REGISTRERT_DATO,ENDRET_DATO,DOKUMENT_XML,INTERN_XML)
select (select behandling.id from behandling where saksnummer = 'MELTEST-999' and REGISTRERT_AV = 'K135703') as bid,
        OPPLYSNING_TYPE,VERSJON,KILDE,REGISTRERT_DATO,ENDRET_DATO,DOKUMENT_XML,INTERN_XML
from SAKSOPPLYSNING
where BEHANDLING_ID = 3 and opplysning_type = 'PERSOPL';
