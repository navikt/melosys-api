-- MELOSYS-8151: Mottak av digital søknad samles i én prosesstype som avgjør ny/eksisterende sak i sagaen.
-- Den gamle ny-sak-flyten får nytt navn, slik at MELOSYS_MOTTAK_DIGITAL_SØKNAD kan brukes av den nye flyten.
-- Eksisterende prosessinstanser (også uferdige) flyttes til det nye navnet og fullføres med gammel flyt.
INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES ('MELOSYS_MOTTAK_DIGITAL_SØKNAD_NY_SAK', 'Mottak av digital søknad for ny sak');

UPDATE PROSESSINSTANS SET PROSESS_TYPE = 'MELOSYS_MOTTAK_DIGITAL_SØKNAD_NY_SAK' WHERE PROSESS_TYPE = 'MELOSYS_MOTTAK_DIGITAL_SØKNAD';

UPDATE PROSESS_TYPE SET NAVN = 'Mottak av digital søknad (ny eller eksisterende sak)' WHERE KODE = 'MELOSYS_MOTTAK_DIGITAL_SØKNAD';

INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('AVKLAR_SAK_OG_BEHANDLING_DIGITAL_SØKNAD', 'Oppretter ny sak eller bruker eksisterende sak for mottatt søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_ARKIVSAK_DIGITAL_SØKNAD', 'Oppretter arkivsak hvis mottatt søknad ga ny sak');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_OPPGAVE_DIGITAL_SØKNAD', 'Oppretter behandlingsoppgave hvis mottatt søknad ga ny sak');
