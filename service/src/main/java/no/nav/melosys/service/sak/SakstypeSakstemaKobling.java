package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.UNNTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;

public final class SakstypeSakstemaKobling {

    private SakstypeSakstemaKobling() {
    }

    // Midlertidig metode for å introdusere sakstema inntil det kan angis i frontend
    public static Sakstemaer sakstema(Sakstyper sakstyper, Behandlingstema behandlingstema) {
        return switch (sakstyper) {
            case EU_EOS -> sakstemaForEøs(behandlingstema);
            case TRYGDEAVTALE, FTRL -> MEDLEMSKAP_LOVVALG;
        };
    }

    private static Sakstemaer sakstemaForEøs(Behandlingstema behandlingstema) {
        if (Behandling.erBehandlingAvSøknadGammel(behandlingstema) || behandlingstema == BESLUTNING_LOVVALG_NORGE) {
            return MEDLEMSKAP_LOVVALG;
        }
        if (behandlingstema == TRYGDETID) {
            return MEDLEMSKAP_LOVVALG;
        }
        if (Behandling.erAnmodningOmUnntak(behandlingstema) || Behandling.erRegistreringAvUnntak(behandlingstema)) {
            return UNNTAK;
        }
        if (behandlingstema == ØVRIGE_SED_MED) {
            return MEDLEMSKAP_LOVVALG;
        }
        if (behandlingstema == ØVRIGE_SED_UFM) {
            return UNNTAK;
        }

        throw new FunksjonellException(String.format("Finner ikke sakstema for behandlingstema %s", behandlingstema));
    }
}
