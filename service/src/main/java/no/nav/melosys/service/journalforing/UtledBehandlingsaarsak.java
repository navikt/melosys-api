package no.nav.melosys.service.journalforing;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.TRYGDEAVGIFT;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

public class UtledBehandlingsaarsak {
    private UtledBehandlingsaarsak() {
    }

    public static Behandlingsaarsaktyper utledÅrsaktype(Journalpost journalpost, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        if (journalpost.mottaksKanalErEessi()) {
            return Behandlingsaarsaktyper.SED;
        }
        if (erSøknad(sakstema, behandlingstema, behandlingstype)) {
            return Behandlingsaarsaktyper.SØKNAD;
        }
        if (behandlingstype == HENVENDELSE) {
            return Behandlingsaarsaktyper.HENVENDELSE;
        }
        return Behandlingsaarsaktyper.ANNET;
    }

    private static boolean erSøknad(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return (sakstema == MEDLEMSKAP_LOVVALG || sakstema == TRYGDEAVGIFT) && (behandlingstema != BESLUTNING_LOVVALG_ANNET_LAND)
            && (behandlingstype == FØRSTEGANG || behandlingstype == NY_VURDERING || behandlingstype == ENDRET_PERIODE);
    }

}
