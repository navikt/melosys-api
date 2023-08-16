UPDATE MOTTATTEOPPLYSNINGER
SET DATA = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    DATA,
                    'HELSEDEL',
                    'FTRL_2_9_FØRSTE_LEDD_A_HELSE'
                    ),
                'HELSEDEL_MED_SYKE_OG_FORELDREPENGER',
                'FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER'
                ),
            'PENSJONSDEL',
            'FTRL_2_9_FØRSTE_LEDD_B_PENSJON'
            ),
        'HELSE_OG_PENSJONSDEL',
        'FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON'
        ),
    'HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER',
    'FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER'
    )
WHERE JSON_VALUE(DATA, '$.trygdedekning')
          IN (
              'HELSEDEL',
              'HELSEDEL_MED_SYKE_OG_FORELDREPENGER',
              'PENSJONSDEL',
              'HELSE_OG_PENSJONSDEL',
              'HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER'
          );
