UPDATE behandlingsresultat
SET resultat_type = 'HENLEGGELSE_BORTFALT'
WHERE resultat_type = 'HENLEGGELSE'
  AND behandling_id IN (
    SELECT id
    FROM behandling
    WHERE saksnummer IN (
        SELECT saksnummer
        FROM fagsak
        WHERE status = 'HENLAGT_BORTFALT'
    )
);
