package no.nav.melosys.service.kontroll.vedtak;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;

public class VedtakKontrollFactory {

    private VedtakKontrollFactory() {
    }

    public static Set<Function<VedtakKontrollData, Kontroll_begrunnelser>> hentKontrollerForVedtakstype(Vedtakstyper vedtakstype) {
        switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK:
                return førstegangsvedtakKontroller();
            case OMGJØRINGSVEDTAK:
            case KORRIGERT_VEDTAK:
                return korrigertVedtakKontroller();
            default:
                throw new UnsupportedOperationException("Kan ikke hente kontroller for vedtakstype " + vedtakstype);
        }
    }

    private static Set<Function<VedtakKontrollData, Kontroll_begrunnelser>> førstegangsvedtakKontroller() {
        return Set.of(
            VedtakKontroller::overlappendeMedlemsperiode,
            VedtakKontroller::periodeOver24Mnd
        );
    }

    private static Set<Function<VedtakKontrollData, Kontroll_begrunnelser>> korrigertVedtakKontroller() {
        return Set.of(
            VedtakKontroller::periodeOver24Mnd
        );
    }
}
