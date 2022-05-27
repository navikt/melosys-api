package no.nav.melosys.service.kontroll.ferdigbehandling;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.validering.Kontrollfeil;

public class FerdigbehandlingRegelsett {

    static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentRegelsettForVedtak(Sakstyper sakstype) {
        return switch (sakstype) {
            case EU_EOS -> REGELSETT_EU_EOS;
            case FTRL -> Collections.emptySet();
            case TRYGDEAVTALE -> REGELSETT_TRYGDEAVTALER;
        };
    }
    static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentRegelsettForAvslagOgHenleggelse() {
        return REGELSETT_AVSLAG_HENLEGGELSE;
    }

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_EU_EOS = Set.of(
        FerdigbehandlingKontroller::adresseRegistrert,
        FerdigbehandlingKontroller::overlappendeMedlemsperiode,
        FerdigbehandlingKontroller::periodeOver24Mnd,
        FerdigbehandlingKontroller::periodeManglerSluttdato,
        FerdigbehandlingKontroller::arbeidsstedManglerFelter,
        FerdigbehandlingKontroller::foretakUtlandManglerFelter
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_TRYGDEAVTALER = Set.of(
        FerdigbehandlingKontroller::adresseRegistrert,
        FerdigbehandlingKontroller::overlappendeMedlemsperiode,
        FerdigbehandlingKontroller::periodeOverTreÅr,
        FerdigbehandlingKontroller::periodeManglerSluttdato,
        FerdigbehandlingKontroller::arbeidsstedManglerFelter,
        FerdigbehandlingKontroller::representantIUtlandetMangler
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_AVSLAG_HENLEGGELSE = Set.of(
        FerdigbehandlingKontroller::adresseRegistrert
    );
}
