-- MELOSYS-8151: Mottak av digital søknad samles i én prosesstype som avgjør ny/eksisterende sak i sagaen.
-- MELOSYS_MOTTAK_DIGITAL_SØKNAD og MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD beholdes for eksisterende prosessinstanser.
INSERT INTO PROSESS_TYPE(KODE, NAVN) VALUES ('MOTTAK_DIGITAL_SØKNAD', 'Mottak av digital søknad (ny eller eksisterende sak)');

INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('AVKLAR_SAK_OG_BEHANDLING_DIGITAL_SØKNAD', 'Oppretter ny sak eller bruker eksisterende sak for mottatt søknad');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_ARKIVSAK_DIGITAL_SØKNAD', 'Oppretter arkivsak hvis mottatt søknad ga ny sak');
INSERT INTO PROSESS_STEG(KODE, NAVN) VALUES ('OPPRETT_OPPGAVE_DIGITAL_SØKNAD', 'Oppretter behandlingsoppgave hvis mottatt søknad ga ny sak');
