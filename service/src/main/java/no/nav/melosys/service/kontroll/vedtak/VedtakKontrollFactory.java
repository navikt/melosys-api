package no.nav.melosys.service.kontroll.vedtak;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.service.validering.Kontrollfeil;

class VedtakKontrollFactory {

    private VedtakKontrollFactory() {
    }

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_EU_EOS_FØRSTE_GANG = Set.of(
        VedtakKontroller::adresseRegistrert,
        VedtakKontroller::overlappendeMedlemsperiode,
        VedtakKontroller::periodeOver24Mnd,
        VedtakKontroller::periodeManglerSluttdato,
        VedtakKontroller::arbeidsstedManglerFelter,
        VedtakKontroller::foretakUtlandManglerFelter
    );

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_EU_EOS_ANDRE_GANG = Set.of(
        VedtakKontroller::adresseRegistrert,
        VedtakKontroller::overlappendeMedlemsperiode,
        VedtakKontroller::periodeOver24Mnd,
        VedtakKontroller::periodeManglerSluttdato,
        VedtakKontroller::arbeidsstedManglerFelter,
        VedtakKontroller::foretakUtlandManglerFelter
    );

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_TRYGDEAVTALER_FØRSTE_GANG = Set.of(
        VedtakKontroller::adresseRegistrert,
        VedtakKontroller::overlappendeMedlemsperiode,
        VedtakKontroller::periodeOverTreÅr,
        VedtakKontroller::periodeManglerSluttdato,
        VedtakKontroller::arbeidsstedManglerFelter,
        VedtakKontroller::representantIUtlandetMangler
    );

    private static final Set<Function<VedtakKontrollData, Kontrollfeil>> KONTROLLER_TRYGDEAVTALER_ANDRE_GANG = Set.of(
        VedtakKontroller::adresseRegistrert,
        VedtakKontroller::overlappendeMedlemsperiode,
        VedtakKontroller::periodeOverTreÅr,
        VedtakKontroller::periodeManglerSluttdato,
        VedtakKontroller::arbeidsstedManglerFelter,
        VedtakKontroller::representantIUtlandetMangler
    );

    static Set<Function<VedtakKontrollData, Kontrollfeil>> hentKontrollerForVedtak(Sakstyper sakstype,
                                                                                   Vedtakstyper vedtakstype) {
        return switch (sakstype) {
            case EU_EOS -> hentKontrollerForEøs(vedtakstype);
            case FTRL -> Collections.emptySet();
            case TRYGDEAVTALE -> hentKontrollerForTrygdeavtaler(vedtakstype);
        };
    }

    private static Set<Function<VedtakKontrollData, Kontrollfeil>> hentKontrollerForEøs(Vedtakstyper vedtakstype) {
        return switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK -> KONTROLLER_EU_EOS_FØRSTE_GANG;
            case ENDRINGSVEDTAK, KORRIGERT_VEDTAK, OMGJØRINGSVEDTAK -> KONTROLLER_EU_EOS_ANDRE_GANG;
        };
    }

    private static Set<Function<VedtakKontrollData, Kontrollfeil>> hentKontrollerForTrygdeavtaler(
        Vedtakstyper vedtakstype) {
        return switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK -> KONTROLLER_TRYGDEAVTALER_FØRSTE_GANG;
            case ENDRINGSVEDTAK, KORRIGERT_VEDTAK, OMGJØRINGSVEDTAK -> KONTROLLER_TRYGDEAVTALER_ANDRE_GANG;
        };
    }
}
