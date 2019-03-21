-- Arbeidstabell for saksflyt
CREATE TABLE prosessinstans (
    uuid            RAW(16) NOT NULL,
    prosess_type    VARCHAR2(99)   NOT NULL,
    behandling_id   NUMBER(19)     NULL,
    data            VARCHAR2(4000) NULL,
    steg            VARCHAR2(99)   NOT NULL,
    registrert_dato TIMESTAMP      NOT NULL,
    endret_dato     TIMESTAMP      NOT NULL,
    antall_retry    INTEGER        DEFAULT 0 NOT NULL,
    sist_forsoekt   TIMESTAMP      NULL,
    sover_til       TIMESTAMP      NULL,
    CONSTRAINT pk_prosessinstans PRIMARY KEY (uuid)
);

CREATE INDEX idx_prosessinstans_behandling ON prosessinstans(behandling_id);
CREATE INDEX idx_prosessinstans_type ON prosessinstans(prosess_type);
CREATE INDEX idx_prosessinstans_steg ON prosessinstans(steg);

CREATE TABLE prosess_type (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_prosess_type PRIMARY KEY (kode)
);

-- Alfabetisk rekkefølge
INSERT INTO prosess_type (kode, navn) VALUES ('ANMODNING_OM_UNNTAK', 'Anmodning om unntak');
INSERT INTO prosess_type (kode, navn) VALUES ('HENLEGG_SAK', 'Henlegg en sak');
INSERT INTO prosess_type (kode, navn) VALUES ('IVERKSETT_VEDTAK', 'Iverksett vedtak');
INSERT INTO prosess_type (kode, navn) VALUES ('IVERKSETT_VEDTAK_FORKORT_PERIODE', 'Iverksett nytt vedtak etter lovvalgsperioden har blitt forkortet');
INSERT INTO prosess_type (kode, navn) VALUES ('JFR_KNYTT', 'Journalføring på eksisterende sak');
INSERT INTO prosess_type (kode, navn) VALUES ('JFR_NY_SAK', 'Journalføring med ny sak og søknad');
INSERT INTO prosess_type (kode, navn) VALUES ('OPPFRISKNING', 'Oppfriskning av saksopplysninger');
INSERT INTO prosess_type (kode, navn) VALUES ('MANGELBREV', 'Opprett mangelbrev');

CREATE TABLE prosess_steg (
    kode    VARCHAR2(99)  NOT NULL,
    navn    VARCHAR2(99)  NOT NULL,
    CONSTRAINT pk_behandling_steg PRIMARY KEY (kode)
);

--Logisk rekkefølge
INSERT INTO prosess_steg (kode, navn) VALUES ('GSAK_OPPRETT_OPPGAVE', 'Oppretter oppgave i GSAK');
INSERT INTO prosess_steg (kode, navn) VALUES ('REPLIKER_BEHANDLING', 'Replikerer behandling');

INSERT INTO prosess_steg (kode, navn) VALUES ('MOT_VURDER_AUTOMATISK_JFR', 'Vurder om journalføring kan skje automatisk');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_VALIDERING', 'Grunnleggende validering');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_VURDER_JOURNALFOERINGSTYPE', 'Nytt innkommende dokument. Saksbehandler vurderer behov for opprettelse av ny behandling.');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_AKTØR_ID', 'Henter aktørID');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPRETT_SAK_OG_BEH', 'Oppretter ny sak og behandling i Melosys');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPRETT_SØKNAD', 'Oppretter ny søknad i Melosys');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPRETT_GSAK_SAK', 'Oppretter Sak i GSAK');
INSERT INTO prosess_steg (kode, navn) VALUES ('STATUS_BEH_OPPR', 'Oppdater Sak og Behandling ved oppretting av behandling');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_OPPDATER_JOURNALPOST', 'Oppdaterer journalposten i Joark');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_FERDIGSTILL_JOURNALPOST', 'Ferdigstiller journalposten i Joark');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_SETT_VURDER_DOKUMENT', 'Setter status til VURDER_DOKUMENT');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_HENT_PERS_OPPL', 'Hent personopplysninger fra TPS');
INSERT INTO prosess_steg (kode, navn) VALUES ('JFR_VURDER_INNGANGSVILKÅR', 'Vurderer inngangsvilkår');

INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_ARBF_OPPL', 'Hent arbeidsforholdopplysninger fra AAREG');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_INNT_OPPL', 'Hent inntektopplysninger fra INNTK');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_ORG_OPPL', 'Hent organisasjoner fra EREG');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_MEDL_OPPL', 'Hent medlemskapsopplysninger fra MEDL');
INSERT INTO prosess_steg (kode, navn) VALUES ('HENT_SOB_SAKER', 'Hent sak fra Sak og behandling');
INSERT INTO prosess_steg (kode, navn) VALUES ('OPPFRISK_SAKSOPPLYSNINGER', 'Oppfrisking av saksopplysninger');
INSERT INTO prosess_steg (kode, navn) VALUES ('SEND_FORVALTNINGSMELDING', 'Send forvaltningsmelding til søker');
INSERT INTO prosess_steg (kode, navn) VALUES ('FEILET_MASKINELT', 'Feilet maskinelt');
INSERT INTO prosess_steg (kode, navn) VALUES ('FATTET_VEDTAK', 'Saksbehandler har fattet vedtak i Melosys');

INSERT INTO prosess_steg (kode, navn) VALUES ('AOU_VALIDERING', 'Validering av data for anmodning om unntak');
INSERT INTO prosess_steg (kode, navn) VALUES ('AOU_OPPDATER_RESULTAT', 'Oppdatering av behandlingsresultat for anmodning om unntak');
INSERT INTO prosess_steg (kode, navn) VALUES ('AOU_OPPDATER_MEDL', 'Oppdatering av medlemskap med anmodning om unntak');
INSERT INTO prosess_steg (kode, navn) VALUES ('AOU_SEND_BREV', 'Send orienteringsbrev og A001 for anmodning om unntak');

INSERT INTO prosess_steg (kode, navn) VALUES ('IV_FORKORT_PERIODE', 'Legg til i AvklarteFakta begrunnelse for forkorting av lovvalgsperiode');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_VALIDERING', 'Validerer iverksett vedtak');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_OPPDATER_RESULTAT', 'Oppdatering av behandlingsresultat');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_AVKLAR_MYNDIGHET', 'Avklaring av utenlandsk trygdemyndighet');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_OPPDATER_MEDL', 'Oppdatering av medlemskap');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_SEND_BREV', 'Send brev etter iverksett vedtak');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_SEND_SED', 'Send SED etter iverksett vedtak');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_AVSLUTT_BEHANDLING', 'Avslutt fagsak og aktiv behandling');
INSERT INTO prosess_steg (kode, navn) VALUES ('IV_STATUS_BEH_AVSL', 'Oppdater Sak og Behandling ved lukking av behandling');

INSERT INTO prosess_steg (kode, navn) VALUES ('HS_OPPDATER_RESULTAT', 'Oppdatering av behandlingsresultat');
INSERT INTO prosess_steg (kode, navn) VALUES ('HS_HENLEGG_SAK', 'Henlegg en sak');
INSERT INTO prosess_steg (kode, navn) VALUES ('HS_SEND_BREV', 'Opprett henleggelsesbrev');

INSERT INTO prosess_steg (kode, navn) VALUES ('MANGELBREV', 'Opprett mangelbrev');

CREATE TABLE prosessinstans_hendelser (
    id                  NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    prosessinstans_id   RAW(16)     NOT NULL,
    registrert_dato     TIMESTAMP      NOT NULL,
    steg                VARCHAR2(99)   NOT NULL,
    type                VARCHAR2(99)   NULL,
    melding             VARCHAR2(4000) NOT NULL,
    CONSTRAINT pk_pi_hendelser PRIMARY KEY (id)
);

CREATE INDEX idx_pi_hendelser_pi ON prosessinstans_hendelser(prosessinstans_id);
CREATE INDEX idx_pi_hendelser_steg ON prosessinstans_hendelser(steg);

ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_behandling FOREIGN KEY (behandling_id) REFERENCES behandling;
ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_type FOREIGN KEY (prosess_type) REFERENCES prosess_type;
ALTER TABLE prosessinstans ADD CONSTRAINT fk_prosinst_steg FOREIGN KEY (steg) REFERENCES prosess_steg;
ALTER TABLE prosessinstans_hendelser ADD CONSTRAINT fk_pihend_pi FOREIGN KEY (prosessinstans_id) REFERENCES prosessinstans;
ALTER TABLE prosessinstans_hendelser ADD CONSTRAINT fk_pihend_steg FOREIGN KEY (steg) REFERENCES prosess_steg;
