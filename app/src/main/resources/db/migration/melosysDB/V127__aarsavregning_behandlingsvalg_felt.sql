ALTER TABLE aarsavregning ADD COLUMN behandlingsvalg VARCHAR(255);

UPDATE aarsavregning SET behandlingsvalg = 'OPPLYSNINGER_ENDRET' WHERE har_avvik = 1;
UPDATE aarsavregning SET behandlingsvalg = 'OPPLYSNINGER_UENDRET' WHERE har_avvik = 0;
