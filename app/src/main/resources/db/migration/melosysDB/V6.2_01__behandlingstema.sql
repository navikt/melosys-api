-- OPPRETT behandling_tema TABELL
CREATE TABLE behandling_tema
(
    kode VARCHAR2(99) NOT NULL,
    navn VARCHAR2(99) NOT NULL,
    CONSTRAINT pk_behandling_tema PRIMARY KEY (kode)
);

ALTER TABLE BEHANDLING ADD beh_tema VARCHAR2(99);
ALTER TABLE BEHANDLING ADD CONSTRAINT fk_behandling_tema FOREIGN KEY (beh_tema) REFERENCES behandling_tema;

-- LEGG INN behandling_tema VERDIER
INSERT INTO behandling_tema (kode, navn) VALUES ('UTSENDT_ARBEIDSTAKER', 'Utsendt arbeidstaker');
INSERT INTO behandling_tema (kode, navn) VALUES ('UTSENDT_SELVSTENDIG', 'Utsendt selvstendig næringsdrivende');
INSERT INTO behandling_tema (kode, navn) VALUES ('ARBEID_ETT_LAND_ØVRIG', 'Øvrig arbeid og næring');
INSERT INTO behandling_tema (kode, navn) VALUES ('IKKE_YRKESAKTIV', 'Ikke yrkesaktiv');
INSERT INTO behandling_tema (kode, navn) VALUES ('ARBEID_FLERE_LAND', 'Arbeid i flere land');
INSERT INTO behandling_tema (kode, navn) VALUES ('ARBEID_NORGE_BOSATT_ANNET_LAND', 'Arbeid i Norge - bosatt i et annet land');
INSERT INTO behandling_tema (kode, navn) VALUES ('REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING', 'Registrering av unntak fra norsk trygd – utstasjonerte (A009)');
INSERT INTO behandling_tema (kode, navn) VALUES ('REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE', 'Registrering av unntak fra norsk trygd – øvrige (A010)');
INSERT INTO behandling_tema (kode, navn) VALUES ('BESLUTNING_LOVVALG_NORGE', 'Norge er utpekt (A003)');
INSERT INTO behandling_tema (kode, navn) VALUES ('BESLUTNING_LOVVALG_ANNET_LAND', 'Utenlandsk myndighet har utpekt et annet land enn Norge (A003)');
INSERT INTO behandling_tema (kode, navn) VALUES ('ANMODNING_OM_UNNTAK_HOVEDREGEL', 'Behandling av en mottatt anmodning om unntak hovedregel (A001)');
INSERT INTO behandling_tema (kode, navn) VALUES ('ØVRIGE_SED', 'Behandling av alle øvrige SED');
INSERT INTO behandling_tema (kode, navn) VALUES ('TRYGDETID', 'Forespørsel om trygdetid');

INSERT INTO behandling_type (kode, navn) VALUES ('SED', 'SED');

