-- Ny prosesstype og prosesssteg for digital søknadsmottak (skjema fra melosys-skjema)
INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES ('MELOSYS_MOTTAK_DIGITAL_SØKNAD', 'Mottak av digital søknad');

INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('HENT_SØKNADSDATA', 'Henter søknadsdata fra melosys-skjema-api');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_SAK_OG_BEHANDLING_SØKNAD', 'Oppretter sak og behandling for søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_OG_FERDIGSTILL_JOURNALPOST_SØKNAD', 'Oppretter og ferdigstiller journalpost for søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('LAGRE_SAKSOPPLYSNINGER_SØKNAD', 'Lagrer saksopplysninger fra søknad');
