UPDATE BEHANDLINGSRESULTAT
SET RESULTAT_TYPE = 'FERDIGBEHANDLET'
WHERE behandling_id IN (SELECT behandling.id
                        FROM behandling
                                 JOIN fagsak ON fagsak.SAKSNUMMER = behandling.SAKSNUMMER
                                 LEFT JOIN BEHANDLINGSRESULTAT ON BEHANDLINGSRESULTAT.behandling_id = behandling.id
                        WHERE EXISTS(
                            SELECT 1
                            FROM fagsak
                            WHERE fagsak.SAKSNUMMER = behandling.SAKSNUMMER
                              AND (
                                    (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'MEDLEMSKAP_LOVVALG' AND
                                     behandling.BEH_TEMA = 'TRYGDETID' AND behandling.BEH_TYPE = 'FØRSTEGANG')
                                    OR
                                    (fagsak.FAGSAK_TYPE = 'TRYGDEAVTALE' AND fagsak.TEMA = 'UNNTAK' AND
                                     behandling.BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET' AND
                                     behandling.BEH_TYPE = 'HENVENDELSE')
                                    OR
                                    (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'MEDLEMSKAP_LOVVALG' AND
                                     behandling.BEH_TEMA = 'TRYGDETID' AND behandling.BEH_TYPE = 'HENVENDELSE')
                                    OR
                                    (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'UNNTAK' AND
                                     behandling.BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET' AND
                                     behandling.BEH_TYPE = 'HENVENDELSE')
                                    OR
                                    (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'MEDLEMSKAP_LOVVALG' AND
                                     behandling.BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET' AND
                                     behandling.BEH_TYPE = 'HENVENDELSE')
                                )
                            )
                          AND behandling.STATUS = 'AVSLUTTET');
