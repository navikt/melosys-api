package no.nav.melosys.integrasjon.dokgen;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.dokgen.dto.DokgenDto;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidKlage;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;

@Component
public class DokgenMalResolver {

    private final Unleash unleash;

    private static final ImmutableMap<Produserbaredokumenter, String> DOKGEN_MALER =
        Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, String>builder()
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID, "saksbehandlingstid_soknad")
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, "saksbehandlingstid_soknad")
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, "saksbehandlingstid_klage")
            .build());

    @Autowired
    public DokgenMalResolver(Unleash unleash) {
        this.unleash = unleash;
    }

    public Set<Produserbaredokumenter> utledTilgjengeligeMaler () {
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

    public DokgenDto mapBehandling(Produserbaredokumenter produserbartDokument, Behandling behandling) throws TekniskException, FunksjonellException {
        switch (produserbartDokument) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD:
                return SaksbehandlingstidSoknad.av(behandling);
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE:
                return SaksbehandlingstidKlage.av(behandling);
            default:
                throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet av melosys-dokgen", produserbartDokument));
        }
    }
}
