package no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;

public enum A1TypeUtstedelse {
    FØRSTEGANG,
    ENDRING,
    ANNULLERING;

    public static A1TypeUtstedelse av(Vedtakstyper vedtakstype) {
        switch (vedtakstype) {
            case FØRSTEGANGSVEDTAK:
                return FØRSTEGANG;
            case KORRIGERT_VEDTAK:
            case OMGJØRINGSVEDTAK:
            case ENDRINGSVEDTAK:
                return ENDRING;
            default:
                throw new UnsupportedOperationException(String.format(
                    "Vedtakstype %s er ikke støttet ved sending av melding om utstedt A1", vedtakstype));
        }
    }
}
