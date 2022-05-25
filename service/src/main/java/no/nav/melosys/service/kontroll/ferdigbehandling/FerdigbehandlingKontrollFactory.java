package no.nav.melosys.service.kontroll.ferdigbehandling;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.validering.Kontrollfeil;

public class FerdigbehandlingKontrollFactory {

    private FerdigbehandlingKontrollFactory() {
    }

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> KONTROLLER_EU_EOS = Set.of(
        FerdigbehandlingKontroller::adresseRegistrert,
        FerdigbehandlingKontroller::overlappendeMedlemsperiode,
        FerdigbehandlingKontroller::periodeOver24Mnd,
        FerdigbehandlingKontroller::periodeManglerSluttdato,
        FerdigbehandlingKontroller::arbeidsstedManglerFelter,
        FerdigbehandlingKontroller::foretakUtlandManglerFelter
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> KONTROLLER_TRYGDEAVTALER = Set.of(
        FerdigbehandlingKontroller::adresseRegistrert,
        FerdigbehandlingKontroller::overlappendeMedlemsperiode,
        FerdigbehandlingKontroller::periodeOverTreÅr,
        FerdigbehandlingKontroller::periodeManglerSluttdato,
        FerdigbehandlingKontroller::arbeidsstedManglerFelter,
        FerdigbehandlingKontroller::representantIUtlandetMangler
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> KONTROLLER_AVSLAG_HENLEGGELSE = Set.of(
        FerdigbehandlingKontroller::adresseRegistrert
    );

    static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentKontrollerForAvslagOgHenleggelse() {
        return KONTROLLER_AVSLAG_HENLEGGELSE;
    }

    static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentKontrollerForVedtak(Sakstyper sakstype) {
        return switch (sakstype) {
            case EU_EOS -> KONTROLLER_EU_EOS;
            case FTRL -> Collections.emptySet();
            case TRYGDEAVTALE -> KONTROLLER_TRYGDEAVTALER;
        };
    }
}
