-- Cross-instance lås for atomisk sak-resolusjon ved mottak av digital søknad (MELOSYS-8151).
-- Når flere relaterte deler av samme søknad konsumeres samtidig, må «ny vs. eksisterende sak»
-- avgjøres serielt per person — ellers opprettes duplikate fagsaker (distribuert kappløp).
-- Det digitale-søknad-NY-steget tar SELECT ... FOR UPDATE på aktoer_id og holder låsen til
-- stegets transaksjon committer sak + skjema_sak_mapping. Se docs/duplikate-saker-digital-soknad.md
CREATE TABLE digital_soknad_sak_lock (
    aktoer_id   VARCHAR2(50)  NOT NULL,
    opprettet   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_digital_soknad_sak_lock PRIMARY KEY (aktoer_id)
);
