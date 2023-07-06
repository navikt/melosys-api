UPDATE behandling
SET STATUS = 'AVSLUTTET'
WHERE STATUS != 'AVSLUTTET'
AND EXISTS(
          SELECT 1
          FROM fagsak
          WHERE fagsak.SAKSNUMMER = behandling.SAKSNUMMER
            AND (
                  (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'MEDLEMSKAP_LOVVALG' AND
                   behandling.BEH_TEMA = 'TRYGDETID' AND behandling.BEH_TYPE = 'FØRSTEGANG')
                  OR
                  (fagsak.FAGSAK_TYPE = 'TRYGDEAVTALE' AND fagsak.TEMA = 'UNNTAK' AND
                   behandling.BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET' AND behandling.BEH_TYPE = 'HENVENDELSE')
                  OR
                  (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'MEDLEMSKAP_LOVVALG' AND
                   behandling.BEH_TEMA = 'TRYGDETID' AND behandling.BEH_TYPE = 'HENVENDELSE')
                  OR
                  (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'UNNTAK' AND
                   behandling.BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET' AND behandling.BEH_TYPE = 'HENVENDELSE')
                  OR
                  (fagsak.FAGSAK_TYPE = 'EU_EOS' AND fagsak.TEMA = 'MEDLEMSKAP_LOVVALG' AND
                   behandling.BEH_TEMA = 'FORESPØRSEL_TRYGDEMYNDIGHET' AND behandling.BEH_TYPE = 'HENVENDELSE')
              )
          );
