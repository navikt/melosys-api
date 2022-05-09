package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.validering.Kontrollfeil;

class VedtakKontrollFactory {

    private VedtakKontrollFactory() {
    }

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_EU_EOS = Set.of(
        VedtakKontroller::adresseRegistrert,
        VedtakKontroller::overlappendeMedlemsperiode,
        VedtakKontroller::periodeOver24Mnd,
        VedtakKontroller::periodeManglerSluttdato,
        VedtakKontroller::arbeidsstedManglerFelter,
        VedtakKontroller::foretakUtlandManglerFelter
    );

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_TRYGDEAVTALER = Set.of(
        VedtakKontroller::adresseRegistrert,
        VedtakKontroller::overlappendeMedlemsperiode,
        VedtakKontroller::periodeOverTreÅr,
        VedtakKontroller::periodeManglerSluttdato,
        VedtakKontroller::arbeidsstedManglerFelter,
        VedtakKontroller::representantIUtlandetMangler
    );

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_AVSLAG_HENLEGGELSE = Set.of(
        VedtakKontroller::adresseRegistrert
    );

    static Set<Function<VedtakKontrollData, Kontrollfeil>> hentKontrollerForAvslagOgHenleggelse() {
        return KONTROLLER_AVSLAG_HENLEGGELSE;
    }

    static Set<Function<VedtakKontrollData, Kontrollfeil>> hentKontrollerForVedtak(Sakstyper sakstype) {
        return switch (sakstype) {
            case EU_EOS -> KONTROLLER_EU_EOS;
            case FTRL -> Collections.emptySet();
            case TRYGDEAVTALE -> KONTROLLER_TRYGDEAVTALER;
        };
    }
}
