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

-- Rename prosesssteg fra V147 til nye DIGITAL_SØKNAD-koder
UPDATE PROSESS_STEG SET KODE = 'HENT_DIGITAL_SØKNADSDATA' WHERE KODE = 'HENT_SØKNADSDATA';
UPDATE PROSESS_STEG SET KODE = 'OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD' WHERE KODE = 'OPPRETT_SAK_OG_BEHANDLING_SØKNAD';
UPDATE PROSESS_STEG SET KODE = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD' WHERE KODE = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD';

-- Oppdater eksisterende prosessinstanser som bruker de gamle steg-kodene
UPDATE PROSESSINSTANS SET SIST_FULLFORT_STEG = 'HENT_DIGITAL_SØKNADSDATA' WHERE SIST_FULLFORT_STEG = 'HENT_SØKNADSDATA';
UPDATE PROSESSINSTANS SET SIST_FULLFORT_STEG = 'OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD' WHERE SIST_FULLFORT_STEG = 'OPPRETT_SAK_OG_BEHANDLING_SØKNAD';
UPDATE PROSESSINSTANS SET SIST_FULLFORT_STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD' WHERE SIST_FULLFORT_STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD';

-- Oppdater prosessinstans_hendelser som refererer til de gamle steg-kodene
UPDATE PROSESSINSTANS_HENDELSER SET STEG = 'HENT_DIGITAL_SØKNADSDATA' WHERE STEG = 'HENT_SØKNADSDATA';
UPDATE PROSESSINSTANS_HENDELSER SET STEG = 'OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD' WHERE STEG = 'OPPRETT_SAK_OG_BEHANDLING_SØKNAD';
UPDATE PROSESSINSTANS_HENDELSER SET STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_DIGITAL_SØKNAD' WHERE STEG = 'OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD';

-- Nye prosesssteg
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD', 'Håndterer eksisterende sak for mottatt digital søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('SEND_SAKSNUMMER_TIL_MELOSYS_SKJEMA_API', 'Sender saksnummer tilbake til melosys-skjema-api');
