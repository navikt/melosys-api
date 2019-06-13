package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;

/**
 * DokumenttypeIdMapper-er registrert i Dokprod.
 */
public enum DokumenttypeIdMapper {
    INSTANS;

    static String hentID(Produserbaredokumenter produserbartDokument) throws TekniskException {
        switch (produserbartDokument) {
            case ATTEST_A1:
                return "000116";
            case INNVILGELSE_YRKESAKTIV:
                return "000108";
            case AVSLAG_ARBEIDSGIVER:
                return "000109";
            case INNVILGELSE_ARBEIDSGIVER:
                return "000127";
            case AVSLAG_YRKESAKTIV:
                return "NY_KODE_FOR_AVSLAG";
            case ORIENTERING_ANMODNING_UNNTAK:
                return "000081";
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
                return "000082";
            case MELDING_HENLAGT_SAK:
                return "000072";
            case MELDING_MANGLENDE_OPPLYSNINGER:
                return "000074";
            case ANMODNING_UNNTAK:
                return "000116"; // Attestene bruker samme dokumentmal med ulikt innhold
            default:
                throw new TekniskException("Fant ikke dokumentTypeID for produserbartDokument " + produserbartDokument);
        }
    }
}
