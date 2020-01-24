package no.nav.melosys.service.dokument.brev;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

/**
 * DokumenttypeIdMapper-er registrert i Dokprod.
 */
public enum DokumenttypeIdMapper {
    INSTANS;

    private static final ImmutableMap<Produserbaredokumenter, String> DOKUMENTTYPE_ID_MAP =
        Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, String>builder()
            .put(ANMODNING_UNNTAK, "000153")
            .put(ATTEST_A1, "000116")
            .put(AVSLAG_ARBEIDSGIVER, "000109")
            .put(AVSLAG_MANGLENDE_OPPLYSNINGER, "000125")
            .put(AVSLAG_YRKESAKTIV, "000081")
            .put(INNVILGELSE_ARBEIDSGIVER, "000127")
            .put(INNVILGELSE_YRKESAKTIV, "000108")
            .put(INNVILGELSE_YRKESAKTIV_FLERE_LAND, "000083")
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID, "000082")
            .put(MELDING_HENLAGT_SAK, "000072")
            .put(MELDING_MANGLENDE_OPPLYSNINGER, "000074")
            .put(ORIENTERING_ANMODNING_UNNTAK, "000084")
            .put(ORIENTERING_UTPEKING_UTLAND, "000168")
            .put(ORIENTERING_VIDERESENDT_SOEKNAD, "000146")
            .build());

    public static String hentID(Produserbaredokumenter produserbartDokument) throws TekniskException {
        if (DOKUMENTTYPE_ID_MAP.containsKey(produserbartDokument)) {
            return DOKUMENTTYPE_ID_MAP.get(produserbartDokument);
        } else {
            throw new TekniskException("Fant ikke dokumentTypeID for produserbartDokument " + produserbartDokument);
        }
    }
}
