ALTER TABLE aarsavregning
    ADD endeligAvgiftValg VARCHAR(255);

UPDATE aarsavregning
SET endeligAvgiftValg = 'OPPLYSNINGER_ENDRET'
WHERE har_avvik = 1;
UPDATE aarsavregning
SET endeligAvgiftValg = 'OPPLYSNINGER_UENDRET'
WHERE har_avvik = 0;

ALTER TABLE aarsavregning
    ADD manuelt_avgift_beloep DECIMAL(12, 2);
