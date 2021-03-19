package no.nav.melosys.integrasjon.dokgen;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class DokumentInfoMapper {
    private final Unleash unleash;

    private static final ImmutableMap<Produserbaredokumenter, DokumentInfo> DOKGEN_MALER =
        Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, DokumentInfo>builder()
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
                new DokumentInfo("saksbehandlingstid_soknad",
                    DokumentKategoriKode.IB,
                    "Melding om forventet sakbehandlingstid")
            )
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                new DokumentInfo("saksbehandlingstid_soknad",
                    DokumentKategoriKode.IB,
                    "Melding om forventet sakbehandlingstid")
            )
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
                new DokumentInfo("saksbehandlingstid_klage",
                    DokumentKategoriKode.IB,
                    "Melding om forventet sakbehandlingstid")
            )
            .put(MANGELBREV_BRUKER,
                new DokumentInfo("mangelbrev_bruker",
                    DokumentKategoriKode.IB,
                    "Melding om manglende opplysninger")
            )
            .put(MANGELBREV_ARBEIDSGIVER,
                new DokumentInfo("mangelbrev_arbeidsgiver",
                    DokumentKategoriKode.IB,
                    "Melding om manglende opplysninger")
            )
// NOTE ikke tatt i bruk enda
//            .put(INNVILGELSE_FOLKETRYGDLOVEN_2_8,
//                new DokumentInfo("innvilgelse_ftrl_2_8", DokumentKategoriKode.VB, "Vedtak om frivillig medlemskap")
//            )
            .build());

    @Autowired
    public DokumentInfoMapper(Unleash unleash) {
        this.unleash = unleash;
    }

    public Set<Produserbaredokumenter> utledTilgjengeligeMaler() {
        return DOKGEN_MALER.keySet().stream()
            .filter(key -> unleash.isEnabled("melosys.brev." + key.name()))
            .collect(toSet());
    }

    public String hentMalnavn(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        sjekkOmStøttetDokument(produserbartDokument);

        DokumentInfo dokumentInfo = DOKGEN_MALER.get(produserbartDokument);
        return dokumentInfo.getDokgenMalnavn();
    }

    public DokumentInfo hentDokumentInfo(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        sjekkOmStøttetDokument(produserbartDokument);

        return DOKGEN_MALER.get(produserbartDokument);
    }

    private void sjekkOmStøttetDokument(Produserbaredokumenter produserbartDokument) throws FunksjonellException {
        if (!DOKGEN_MALER.containsKey(produserbartDokument)) {
            throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet", produserbartDokument));
        }
    }
}
