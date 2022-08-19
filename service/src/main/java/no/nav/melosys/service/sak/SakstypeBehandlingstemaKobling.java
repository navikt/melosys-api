package no.nav.melosys.service.sak;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public final class SakstypeBehandlingstemaKobling {

    private SakstypeBehandlingstemaKobling() {
    }

    public static boolean erGyldigBehandlingstemaForSakstype(Sakstyper sakstype, Behandlingstema behandlingstema) {
        return lagSakstypeBehandlingstemaMap().get(sakstype).contains(behandlingstema);
    }

    private static Map<Sakstyper, Collection<Behandlingstema>> lagSakstypeBehandlingstemaMap() {
        return Map.of(
            Sakstyper.EU_EOS, Set.of(
                Behandlingstema.UTSENDT_ARBEIDSTAKER,
                Behandlingstema.UTSENDT_SELVSTENDIG,
                Behandlingstema.ARBEID_ETT_LAND_ØVRIG,
                Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
                Behandlingstema.ARBEID_FLERE_LAND,
                Behandlingstema.TRYGDETID,
                Behandlingstema.IKKE_YRKESAKTIV,
                Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND,
                Behandlingstema.ØVRIGE_SED_MED,
                Behandlingstema.ØVRIGE_SED_UFM,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                Behandlingstema.PENSJONIST
            ),
            Sakstyper.FTRL, Set.of(
                Behandlingstema.ARBEID_I_UTLANDET,
                Behandlingstema.YRKESAKTIV,
                Behandlingstema.IKKE_YRKESAKTIV,
                Behandlingstema.UNNTAK_MEDLEMSKAP,
                Behandlingstema.PENSJONIST
            ),
            Sakstyper.TRYGDEAVTALE, Set.of(
                Behandlingstema.YRKESAKTIV,
                Behandlingstema.IKKE_YRKESAKTIV,
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL,
                Behandlingstema.REGISTRERING_UNNTAK,
                Behandlingstema.PENSJONIST
            )
        );
    }
}
