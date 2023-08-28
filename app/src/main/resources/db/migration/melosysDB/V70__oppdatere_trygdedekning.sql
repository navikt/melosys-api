update lovvalg_periode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_A_HELSE'
where trygde_dekning = 'HELSEDEL';
update lovvalg_periode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER'
where trygde_dekning = 'HELSEDEL_MED_SYKE_OG_FORELDREPENGER';
update lovvalg_periode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_B_PENSJON'
where trygde_dekning = 'PENSJONSDEL';
update lovvalg_periode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON'
where trygde_dekning = 'HELSE_OG_PENSJONSDEL';
update lovvalg_periode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER'
where trygde_dekning = 'HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER';

update anmodningsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_A_HELSE'
where trygde_dekning = 'HELSEDEL';
update anmodningsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER'
where trygde_dekning = 'HELSEDEL_MED_SYKE_OG_FORELDREPENGER';
update anmodningsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_B_PENSJON'
where trygde_dekning = 'PENSJONSDEL';
update anmodningsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON'
where trygde_dekning = 'HELSE_OG_PENSJONSDEL';
update anmodningsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER'
where trygde_dekning = 'HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER';

update medlemskapsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_A_HELSE'
where trygde_dekning = 'HELSEDEL';
update medlemskapsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER'
where trygde_dekning = 'HELSEDEL_MED_SYKE_OG_FORELDREPENGER';
update medlemskapsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_B_PENSJON'
where trygde_dekning = 'PENSJONSDEL';
update medlemskapsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON'
where trygde_dekning = 'HELSE_OG_PENSJONSDEL';
update medlemskapsperiode
set trygde_dekning = 'FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER'
where trygde_dekning = 'HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER';

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
