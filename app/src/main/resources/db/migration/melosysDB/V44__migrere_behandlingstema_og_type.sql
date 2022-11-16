UPDATE BEHANDLING
SET BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET'
WHERE BEH_TEMA in ('ØVRIGE_SED_MED', 'ØVRIGE_SED_UFM');

UPDATE BEHANDLING
SET BEH_TEMA = 'ARBEID_TJENESTEPERSON_ELLER_FLY'
WHERE BEH_TEMA = 'ARBEID_ETT_LAND_ØVRIG';

UPDATE BEHANDLING
SET BEH_TEMA = 'YRKESAKTIV'
WHERE BEH_TEMA = 'ARBEID_I_UTLANDET';

-- Oppdaterer behandlingstype fra SOEKNAD og SED til FØRSTEGANG hvis raden er første behandling for fagsaken
UPDATE BEHANDLING
SET BEH_TYPE = 'FØRSTEGANG'
WHERE ID in (select id
             from (with behandling_med_sak_counter AS (SELECT id,
                                                              BEH_TYPE,
                                                              ROW_NUMBER() over (PARTITION BY SAKSNUMMER ORDER BY REGISTRERT_DATO) AS sak_row_number
                                                       FROM BEHANDLING)
                   SELECT *
                   FROM behandling_med_sak_counter
                   WHERE BEH_TYPE in ('SOEKNAD', 'SED')
                     AND sak_row_number = 1));
