package no.nav.melosys.integrasjon.dokgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;

import static java.lang.String.format;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;

public class DokgenMalMapper {
    private DokgenMalMapper(){}

    private static final ImmutableMap<Produserbaredokumenter, String> DOKGEN_MALER =
        Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, String>builder()
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, "saksbehandlingstid_soknad")
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, "saksbehandlingstid_klage")
            .build());

    public static String hentMalnavn(Produserbaredokumenter produserbartDokument) throws TekniskException {
        if (DOKGEN_MALER.containsKey(produserbartDokument)) {
            return DOKGEN_MALER.get(produserbartDokument);
        } else {
            throw new TekniskException(format("Fant ikke malnavn for produserbartDokument %s", produserbartDokument));
        }
    }
}
