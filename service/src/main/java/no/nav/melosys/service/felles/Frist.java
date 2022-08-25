package no.nav.melosys.service.felles;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class Frist {

    // Behandlingsfrist for behandling og fristFerdigstillelse for oppgaver er den samme
    public static LocalDate utledBehandlingsfrist(Behandlingstema behandlingstema) {
        return utledFristForBehandlingstema(behandlingstema);
    }
    public static LocalDate utledFristFerdigstillelse(Behandlingstema behandlingstema) {
        return utledFristForBehandlingstema(behandlingstema);
    }

    private static LocalDate utledFristForBehandlingstema(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_FLERE_LAND,
                ARBEID_ETT_LAND_ØVRIG, IKKE_YRKESAKTIV, ARBEID_I_UTLANDET, ARBEID_NORGE_BOSATT_ANNET_LAND,
                YRKESAKTIV, UNNTAK_MEDLEMSKAP, REGISTRERING_UNNTAK, PENSJONIST -> fristDager(30);
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE -> fristUker(2);
            case BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND -> fristUker(4);
            case ANMODNING_OM_UNNTAK_HOVEDREGEL, ØVRIGE_SED_UFM, ØVRIGE_SED_MED, FORESPØRSEL_TRYGDEMYNDIGHET, TRYGDETID ->
                fristUker(8);
            default ->
                throw new IllegalArgumentException("Melosys støtter ikke mapping for behandlingstema  " + behandlingstema);
        };
    }

    private static LocalDate fristUker(int uker) {
        return LocalDate.now().plusWeeks(uker);
    }

    private static LocalDate fristDager(int dager) {
        return LocalDate.now().plusDays(dager);
    }
}
