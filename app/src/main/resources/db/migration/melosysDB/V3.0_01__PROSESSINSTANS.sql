-- Arbeidstabell for saksflyt
CREATE TABLE prosessinstans (
    id              NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    prosess_type    VARCHAR2(99)   NOT NULL,
    behandling_id   NUMBER(19)     NULL,
    data            VARCHAR2(4000) NULL,
    steg            VARCHAR2(99)   NULL,
    registrert_dato TIMESTAMP      NOT NULL,
    endret_dato     TIMESTAMP      NOT NULL,
    antall_retry    INTEGER        DEFAULT 0 NOT NULL,
    sist_forsoekt   TIMESTAMP      NULL,
    sover_til       TIMESTAMP      NULL,
    CONSTRAINT pk_prosessinstans PRIMARY KEY (id)
);

CREATE TABLE prosess_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_prosess_type PRIMARY KEY (kode)
);

INSERT INTO prosess_type (kode, navn) VALUES ('JFR_KNYTT', 'Journalføring på eksisterende sak');
INSERT INTO prosess_type (kode, navn) VALUES ('JFR_NY_SAK', 'Journalføring med ny sak og søknad');
INSERT INTO prosess_type (kode, navn) VALUES ('SØKNAD_A1', 'Søknad A1');

CREATE TABLE prosess_steg (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_steg PRIMARY KEY (kode)
);


INSERT INTO prosess_steg (kode, navn) VALUES ('MOT_VURDER_AUTOMATISK_JFR', 'Vurder om journalføring kan skje automatisk');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_AVSLUTT_OPPGAVE', 'Avslutter journalføringsoppgaven i GSAK');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_AKTOER_ID', 'Henter aktørID');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPRETT_SAK_OG_BEH', 'Oppretter ny sak og behandling i Melosys');
INSERT INTO prosess_steg (kode, navn) VALUES ('STATUS_BEH_OPPR', 'Oppdater Sak og Behandling ved oppretting av behandling'),
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPRETT_GSAK_SAK', 'Oppretter Sak i GSAK');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPDATER_JOURNALPOST', 'Oppdaterer journalposten i Joark');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_FERDIGSTILL_JOURNALPOST', 'Ferdigstiller journalposten i Joark');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_HENT_PERS_OPPL', 'Hent personopplysninger fra TPS');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_VURDER_INNGANGSVILKÅR', 'Vurderer inngangsvilkår');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_ARBF_OPPL', 'Hent arbeidsforholdopplysninger fra AAREG');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_INNT_OPPL', 'Hent inntektopplysninger fra INNTK');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_ORG_OPPL', 'Hent organisasjoner fra EREG');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_MEDL_OPPL', 'Hent medlemskapsopplysninger fra MEDL');
INSERT INTO prosess_steg (kode, navn) VALUES ('OPPRETT_OPPGAVE', 'Oppretter oppgave i GSAK');
INSERT INTO prosess_steg (kode, navn) VALUES ('FEILET_MASKINELT', 'Feilet maskinelt');
INSERT INTO prosess_steg (kode, navn) VALUES ('FERDIG', 'Ferdig');
INSERT INTO prosess_steg (kode, navn) VALUES ('FATTET_VEDTAK', 'Saksbehandler har fattet vedtak i Melosys'),
INSERT INTO prosess_steg (kode, navn) VALUES ('STATUS_BEH_AVSL', 'Oppdater Sak og Behandling ved lukking av behandling'),

CREATE TABLE prosessinstans_hendelser (
    id                  NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    prosessinstans_id   NUMBER(19)     NOT NULL,
    registrert_dato     TIMESTAMP      NOT NULL,
    steg                VARCHAR2(99)   NOT NULL,
    type                VARCHAR2(99)   NULL,
    melding             VARCHAR2(4000) NOT NULL,
    CONSTRAINT pk_pi_hendelser PRIMARY KEY (id)
);


ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_type FOREIGN KEY (prosess_type) REFERENCES prosess_type;
ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_steg FOREIGN KEY (steg) REFERENCES prosess_steg;
ALTER TABLE prosessinstans_hendelser ADD CONSTRAINT fk_pihend_pi FOREIGN KEY (prosessinstans_id) REFERENCES prosessinstans;
ALTER TABLE prosessinstans_hendelser ADD CONSTRAINT fk_pihend_steg FOREIGN KEY (steg) REFERENCES prosess_steg;
