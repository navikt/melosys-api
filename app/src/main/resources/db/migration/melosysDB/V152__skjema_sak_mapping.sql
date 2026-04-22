-- Mapping mellom skjemaId fra melosys-skjema-api og fagsak/mottatte opplysninger i melosys-api.
-- Brukes for å koble nye instanser av samme søknad til eksisterende sak.
CREATE TABLE skjema_sak_mapping (
    skjema_id                   RAW(16)         NOT NULL,
    saksnummer                  VARCHAR2(99)    NOT NULL,
    mottatte_opplysninger_id    NUMBER(19)      NULL,
    original_data               CLOB            NULL,
    journalpost_id              VARCHAR2(99)    NULL,
    innsendt_dato               TIMESTAMP       NULL,
    opprettet_dato              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_skjema_sak_mapping PRIMARY KEY (skjema_id),
    CONSTRAINT fk_skjema_sak_mapping_fagsak FOREIGN KEY (saksnummer) REFERENCES fagsak(saksnummer),
    CONSTRAINT fk_skjema_sak_mapping_mottopp FOREIGN KEY (mottatte_opplysninger_id) REFERENCES mottatteopplysninger(id)
);

CREATE INDEX idx_skjema_sak_mapping_saksnr ON skjema_sak_mapping(saksnummer);
CREATE INDEX idx_skjema_sak_mapping_mottopp ON skjema_sak_mapping(mottatte_opplysninger_id);

-- Ny prosesstype for journalføring av digital søknad på eksisterende sak
INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES ('MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD', 'Mottak av digital søknad for eksisterende sak');

-- Rename PROSESS_STEG-koder fra V147 til DIGITAL_SØKNAD-koder.
-- Oracle støtter ikke ON UPDATE CASCADE, og PROSESSINSTANS.SIST_FULLFORT_STEG /
-- PROSESSINSTANS_HENDELSER.STEG har FK til PROSESS_STEG(KODE). Derfor må vi
-- INSERT nye koder, UPDATE barnerader, og DELETE gamle koder (i den rekkefølgen).
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('HENT_DIGITAL_SØKNADSDATA', 'Henter søknadsdata');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD', 'Oppretter sak og behandling for søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD', 'Oppretter og ferdigstiller journalpost for søknad');

UPDATE PROSESSINSTANS SET SIST_FULLFORT_STEG = 'HENT_DIGITAL_SØKNADSDATA' WHERE SIST_FULLFORT_STEG = 'HENT_SØKNADSDATA';
UPDATE PROSESSINSTANS SET SIST_FULLFORT_STEG = 'OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD' WHERE SIST_FULLFORT_STEG = 'OPPRETT_SAK_OG_BEHANDLING_SØKNAD';
UPDATE PROSESSINSTANS SET SIST_FULLFORT_STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD' WHERE SIST_FULLFORT_STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD';

UPDATE PROSESSINSTANS_HENDELSER SET STEG = 'HENT_DIGITAL_SØKNADSDATA' WHERE STEG = 'HENT_SØKNADSDATA';
UPDATE PROSESSINSTANS_HENDELSER SET STEG = 'OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD' WHERE STEG = 'OPPRETT_SAK_OG_BEHANDLING_SØKNAD';
UPDATE PROSESSINSTANS_HENDELSER SET STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD' WHERE STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD';

DELETE FROM PROSESS_STEG WHERE KODE = 'HENT_SØKNADSDATA';
DELETE FROM PROSESS_STEG WHERE KODE = 'OPPRETT_SAK_OG_BEHANDLING_SØKNAD';
DELETE FROM PROSESS_STEG WHERE KODE = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD';

-- Nye prosesssteg for eksisterende-sak-flyten
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD', 'Håndterer eksisterende sak for mottatt digital søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API', 'Sender saksnummer tilbake til melosys-skjema-api');
