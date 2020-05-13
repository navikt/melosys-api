package no.nav.melosys.service.kontroll.vedtak;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;

class VedtakKontrollFactory {

    private VedtakKontrollFactory() {
    }

    static Set<Function<VedtakKontrollData, Kontroll_begrunnelser>> hentKontrollerForVedtakstype(Vedtakstyper vedtakstype) {
        switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK:
            case OMGJØRINGSVEDTAK:
            case KORRIGERT_VEDTAK:
                return vedtakKontroller();
            default:
                throw new UnsupportedOperationException("Kan ikke hente kontroller for vedtakstype " + vedtakstype);
        }
    }

    private static Set<Function<VedtakKontrollData, Kontroll_begrunnelser>> vedtakKontroller() {
        return Set.of(
            VedtakKontroller::bostedsadresseForA1,
            VedtakKontroller::overlappendeMedlemsperiode,
            VedtakKontroller::periodeOver24Mnd
        );
    }
}
