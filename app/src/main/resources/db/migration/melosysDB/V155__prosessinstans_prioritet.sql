-- Default 'NORMAL' gjør ADD til en metadata-operasjon (rask, ingen full-tabell-UPDATE) og lukker
-- NOT NULL-vinduet under rullerende deploy: gamle podder uten prioritet-kolonnen får defaulten ved INSERT.
-- Backfill-listene under speiler ProsessType.prioritet (kilden til sannhet) per V155.
-- Merk: backfill setter type-default. Allerede køede sub-prosesser blir ikke retroaktivt løftet til
-- parentens prioritet — det gjelder kun nye prosesser fra og med denne endringen. Akseptabelt da
-- prosessinstans er en kortlevd arbeidstabell.
ALTER TABLE prosessinstans ADD prioritet VARCHAR2(10) DEFAULT 'NORMAL' NOT NULL;

UPDATE prosessinstans SET prioritet = 'HØY' WHERE prosess_type IN (
    'IVERKSETT_VEDTAK_EOS',
    'IVERKSETT_VEDTAK_EOS_FORKORT_PERIODE',
    'IVERKSETT_VEDTAK_FTRL',
    'IVERKSETT_VEDTAK_IKKE_YRKESAKTIV',
    'IVERKSETT_VEDTAK_TRYGDEAVTALE',
    'JFR_ANDREGANG_NY_BEHANDLING',
    'JFR_ANDREGANG_REPLIKER_BEHANDLING',
    'JFR_KNYTT',
    'JFR_NY_SAK_BRUKER',
    'JFR_NY_SAK_VIRKSOMHET'
);

UPDATE prosessinstans SET prioritet = 'LAV' WHERE prosess_type IN (
    'OPPRETT_NY_BEHANDLING_AARSAVREGNING',
    'SATSENDRING',
    'SATSENDRING_TILBAKESTILL_NY_VURDERING'
);
