UPDATE FAGSAK
SET TEMA = 'MEDLEMSKAP_LOVVALG'
WHERE SAKSNUMMER in (
    SELECT f.SAKSNUMMER  FROM  FAGSAK f, BEHANDLING b
    WHERE b.ID in (select id
                   from (with behandling_med_counter AS (SELECT id, ROW_NUMBER() over (PARTITION BY SAKSNUMMER ORDER BY REGISTRERT_DATO DESC) AS beh_row_number
                                                         FROM BEHANDLING)
                         SELECT *
                         FROM behandling_med_counter
                         WHERE beh_row_number = 1)) AND f.SAKSNUMMER = b.SAKSNUMMER
      AND b.BEH_TEMA  IN ('BESLUTNING_LOVVALG_NORGE')
      AND f.TEMA != 'MEDLEMSKAP_LOVVALG'
    );

UPDATE FAGSAK
SET TEMA = 'UNNTAK'
WHERE SAKSNUMMER in (
    SELECT f.SAKSNUMMER  FROM  FAGSAK f, BEHANDLING b
    WHERE b.ID in (select id
                   from (with behandling_med_counter AS (SELECT id, ROW_NUMBER() over (PARTITION BY SAKSNUMMER ORDER BY REGISTRERT_DATO DESC) AS beh_row_number
                                                         FROM BEHANDLING)
                         SELECT *
                         FROM behandling_med_counter
                         WHERE beh_row_number = 1)) AND f.SAKSNUMMER = b.SAKSNUMMER
      AND b.BEH_TEMA  IN ('BESLUTNING_LOVVALG_ANNET_LAND', 'REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE', 'REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING')
      AND f.TEMA != 'UNNTAK'
);
