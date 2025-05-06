ALTER TABLE aarsavregning
    ADD endelig_avgift_valg VARCHAR(255);

UPDATE aarsavregning
SET endelig_avgift_valg = 'OPPLYSNINGER_ENDRET'
WHERE har_avvik = 1;
UPDATE aarsavregning
SET endelig_avgift_valg = 'OPPLYSNINGER_UENDRET'
WHERE har_avvik = 0;

ALTER TABLE aarsavregning
    ADD manuelt_avgift_beloep DECIMAL(12, 2);
