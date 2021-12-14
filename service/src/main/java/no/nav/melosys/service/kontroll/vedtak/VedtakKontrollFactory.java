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

    static Set<Function<VedtakKontrollData, Kontrollfeil>> hentKontrollerForVedtakstype(Vedtakstyper vedtakstype, Sakstyper sakstype) {
        return switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK, OMGJØRINGSVEDTAK, KORRIGERT_VEDTAK -> vedtakKontroller(sakstype);
            default -> throw new UnsupportedOperationException("Kan ikke hente kontroller for vedtakstype " + vedtakstype);
        };
    }

    private static Set<Function<VedtakKontrollData, Kontrollfeil>> vedtakKontroller(Sakstyper sakstype) {
        return switch (sakstype) {
            case EU_EOS -> Set.of(
                VedtakKontroller::adresseRegistrert,
                VedtakKontroller::overlappendeMedlemsperiode,
                VedtakKontroller::periodeOver24Mnd,
                VedtakKontroller::periodeManglerSluttdato,
                VedtakKontroller::arbeidsstedManglerFelter,
                VedtakKontroller::foretakUtlandManglerFelter
            );
            case TRYGDEAVTALE -> Set.of(
                VedtakKontroller::adresseRegistrert,
                VedtakKontroller::overlappendeMedlemsperiode,
                VedtakKontroller::periodeManglerSluttdato,
                VedtakKontroller::periodeOverTreÅr,
                VedtakKontroller::arbeidsstedManglerFelter,
                VedtakKontroller::representantIUtlandetMangler
            );
            default -> Collections.emptySet();
        };

    }
}
