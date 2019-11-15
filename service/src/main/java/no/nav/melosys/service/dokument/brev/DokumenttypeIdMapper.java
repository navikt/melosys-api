package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;

/**
 * DokumenttypeIdMapper-er registrert i Dokprod.
 */
public enum DokumenttypeIdMapper {
    INSTANS;

    public static String hentID(Produserbaredokumenter produserbartDokument) throws TekniskException {
        switch (produserbartDokument) {
            case ATTEST_A1:
                return "000116";
            case INNVILGELSE_YRKESAKTIV:
                return "000108";
            case INNVILGELSE_YRKESAKTIV_FLERE_LAND:
                return "000083";
            case AVSLAG_ARBEIDSGIVER:
                return "000109";
            case INNVILGELSE_ARBEIDSGIVER:
                return "000127";
            case AVSLAG_YRKESAKTIV:
                return "000081";
            case AVSLAG_MANGLENDE_OPPLYSNINGER:
                return "000125";
            case ORIENTERING_ANMODNING_UNNTAK:
                return "000084";
            case ORIENTERING_VIDERESENDT_SOEKNAD:
                return "000146";
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
                return "000082";
            case MELDING_HENLAGT_SAK:
                return "000072";
            case MELDING_MANGLENDE_OPPLYSNINGER:
                return "000074";
            case ANMODNING_UNNTAK:
                return "000153";
            default:
                throw new TekniskException("Fant ikke dokumentTypeID for produserbartDokument " + produserbartDokument);
        }
    }
}
