insert into lovvalg_periode(beh_resultat_id, fom_dato, tom_dato, LOVVALGSLAND,
                            LOVVALG_BESTEMMELSE, INNVILGELSE_RESULTAT,
                            MEDLEMSKAPSTYPE, trygde_dekning)
    values ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
            '13-NOV-2017', '13-NOV-2018', 'NO', 'FO_883_2004_ART12_1', 'INNVILGET',
            'FRIVILLIG', 'FULL_DEKNING_EOSFO');

insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        null, 'AT', 'TRUE', 'SOKNADSLAND');

insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        null, null, 'ETT_LAND_IKKE_NORGE', 'YRKESAKTIVITET_ANTALL_LAND');

insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        'YRKESGRUPPE', null, 'ORDINAER', 'YRKESGRUPPE');

insert into AVKLARTEFAKTA (BEH_RESULTAT_ID,TYPE,SUBJEKT,FAKTA,REFERANSE)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
            'VIRKSOMHET', '982683955', 'TRUE', 'VIRKSOMHET');

insert into avklartefakta (beh_resultat_id, type, subjekt, fakta, referanse)
values ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        null, null, 'ORDINAER_ARBEIDSTAKER', 'YRKESAKTIVITET');

INSERT INTO VILKAARSRESULTAT (BEH_RESULTAT_ID, VILKAAR, OPPFYLT, BEGRUNNELSE_FRITEKST, REGISTRERT_DATO, REGISTRERT_AV, ENDRET_DATO, ENDRET_AV)
VALUES ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        'ART12_1_FORUTGAAENDE_MEDLEMSKAP', 1, null, TO_TIMESTAMP('2019-10-02 14:02:52.905000', 'YYYY-MM-DD HH24:MI:SS.FF6'), 'Z990836', TO_TIMESTAMP('2019-10-02 14:02:52.905000', 'YYYY-MM-DD HH24:MI:SS.FF6'), 'Z990836');

INSERT INTO VILKAARSRESULTAT (BEH_RESULTAT_ID, VILKAAR, OPPFYLT, BEGRUNNELSE_FRITEKST, REGISTRERT_DATO, REGISTRERT_AV, ENDRET_DATO, ENDRET_AV)
VALUES ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        'ART12_1_VESENTLIG_VIRKSOMHET', 1, null, TO_TIMESTAMP('2019-10-02 14:02:52.909000', 'YYYY-MM-DD HH24:MI:SS.FF6'), 'Z990836', TO_TIMESTAMP('2019-10-02 14:02:52.909000', 'YYYY-MM-DD HH24:MI:SS.FF6'), 'Z990836');

INSERT INTO VILKAARSRESULTAT (BEH_RESULTAT_ID, VILKAAR, OPPFYLT, BEGRUNNELSE_FRITEKST, REGISTRERT_DATO, REGISTRERT_AV, ENDRET_DATO, ENDRET_AV)
VALUES ((select behandling.id from behandling where saksnummer = 'MELTEST-3'),
        'FO_883_2004_ART12_1', 1, null, TO_TIMESTAMP('2019-10-02 14:02:52.912000', 'YYYY-MM-DD HH24:MI:SS.FF6'), 'Z990836', TO_TIMESTAMP('2019-10-02 14:02:52.912000', 'YYYY-MM-DD HH24:MI:SS.FF6'), 'Z990836');