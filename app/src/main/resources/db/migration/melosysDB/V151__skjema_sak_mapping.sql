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

-- Ny prosesstype og prosesssteg for journalføring av digital søknad på eksisterende sak
INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES ('MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD', 'Mottak av digital søknad for eksisterende sak');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('HÅNDTER_EKSISTERENDE_SAK_SØKNAD', 'Håndterer eksisterende sak for mottatt søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('SEND_SAKSNUMMER_TIL_SKJEMA', 'Sender saksnummer tilbake til melosys-skjema-api');
