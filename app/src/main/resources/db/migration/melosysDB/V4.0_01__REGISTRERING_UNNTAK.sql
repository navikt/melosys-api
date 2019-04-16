INSERT INTO prosess_type (kode, navn) VALUES ('REGISTRERING_UNNTAK', 'Registrering av unntak');

INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_OPPRETT_SAK_OG_BEH', 'Opprett sak og behandling');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_FERDIGSTILL_JOURNALPOST', 'Ferdigstiller journalpost');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_HENT_PERSON', 'Henter person tilknyttet SED');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_OPPRETT_SEDDOKUMENT', 'Oppretter sedinfo dokument');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_OPPDATER_BEHANDLING', 'Oppdaterer behandling og setter lovvalgsperiode');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_VALIDER_PERIODE', 'Validerer periode mottatt i søknad');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_VALIDER_MEDLEMSKAP', 'Validerer tidligere medlemskap mot MEDL');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_VALIDER_YTELSER', 'Sjekker offentlige ytelser for en person');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_VALIDER_STATSBORGERSKAP', 'Validerer om person har gyldig statsborgerskap for unntaksregel');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_BESTEM_BEHANDLINGSMAATE', 'Bestem om søknad skal registreres automatisk eller behandles manuelt');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_OPPDATER_MEDL', 'Sett periode endelig i MEDL');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_OPPRETT_OPPGAVE', 'Opprett oppgave for manuell behandling');
INSERT INTO prosess_steg (kode, navn) VALUES ('REG_UNNTAK_AVSLUTT_BEHANDLING', 'Avslutt behandling');

INSERT INTO saksopplysning_type (kode, navn) VALUES ('SEDOPPL', 'SED-opplysninger');
INSERT INTO saksopplysning_kilde (kode, navn) VALUES ('EESSI', 'EESSI-prosjektet');
