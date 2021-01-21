package no.nav.melosys.integrasjon.dokgen;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class DokgenMalResolver {

    private final Unleash unleash;

    private static final ImmutableMap<Produserbaredokumenter, String> DOKGEN_MALER =
        Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, String>builder()
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID, "saksbehandlingstid_soknad")
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, "saksbehandlingstid_soknad")
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, "saksbehandlingstid_klage")
            .put(MANGELBREV_BRUKER, "mangelbrev_bruker")
            .put(MANGELBREV_ARBEIDSGIVER, "mangelbrev_arbeidsgiver")
            .build());

    @Autowired
    public DokgenMalResolver(Unleash unleash) {
        this.unleash = unleash;
    }

    public Set<Produserbaredokumenter> utledTilgjengeligeMaler() {
        return DOKGEN_MALER.keySet().stream()
            .filter(key -> unleash.isEnabled("melosys.brev." + key.name()))
            .collect(toSet());
    }

    public String hentMalnavn(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        if (DOKGEN_MALER.containsKey(produserbartDokument)) {
            return DOKGEN_MALER.get(produserbartDokument);
        } else {
            throw new FunksjonellException(format("Fant ikke malnavn for produserbartDokument %s", produserbartDokument));
        }
    }
}
