package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.ProduserbartDokument;
import no.nav.melosys.exception.TekniskException;

/**
 * DokumenttypeID-er registrert i Dokprod.
 */
public final class DokumenttypeID {

    private DokumenttypeID() {
    }

    static String hentID(ProduserbartDokument produserbartDokument) throws TekniskException {
        switch (produserbartDokument) {
            case ATTEST_A1:
                return "000116";
            case INNVILGELSE_YRKESAKTIV:
                return "000108";
            case ORIENTERING_ANMODNING_UNNTAK:
                return "000116"; // Attestene bruker samme dokumentmal med ulikt innhold
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
                return "000082";
            case MELDING_HENLAGT_SAK:
                return "000072";
            case MELDING_MANGLENDE_OPPLYSNINGER:
                return "000074";
            default:
                throw new TekniskException("Fant ikke dokumentTypeID for produserbartDokument " + produserbartDokument);
        }
    }
}
