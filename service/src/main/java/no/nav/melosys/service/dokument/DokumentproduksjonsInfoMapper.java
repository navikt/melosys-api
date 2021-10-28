package no.nav.melosys.service.dokument;

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
public class DokumentproduksjonsInfoMapper {
    private final Unleash unleash;

    private static final ImmutableMap<Produserbaredokumenter, DokumentproduksjonsInfo> DOKUMENTPRODUKSJONS_INFO_MAP;

    static {
        DOKUMENTPRODUKSJONS_INFO_MAP = Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, DokumentproduksjonsInfo>builder()
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
                new DokumentproduksjonsInfo("saksbehandlingstid_soknad",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.FORVALTNINGSMELDING.getTittel())
            )
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                new DokumentproduksjonsInfo("saksbehandlingstid_soknad",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.FORVALTNINGSMELDING.getTittel())
            )
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
                new DokumentproduksjonsInfo("saksbehandlingstid_klage",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.FORVALTNINGSMELDING.getTittel())
            )
            .put(MANGELBREV_BRUKER,
                new DokumentproduksjonsInfo("mangelbrev_bruker",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MANGELBREV.getTittel())
            )
            .put(MANGELBREV_ARBEIDSGIVER,
                new DokumentproduksjonsInfo("mangelbrev_arbeidsgiver",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MANGELBREV.getTittel())
            )
            .put(INNVILGELSE_FOLKETRYGDLOVEN_2_8,
                new DokumentproduksjonsInfo("innvilgelse_ftrl_2_8",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_FTRL_2_8.getTittel())
            )
            .put(ATTEST_NO_UK_1,
                new DokumentproduksjonsInfo("attest_no_uk_1",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.ATTEST_NO_UK_1.getTittel()))
            .build());
    }

    @Autowired
    public DokumentproduksjonsInfoMapper(Unleash unleash) {
        this.unleash = unleash;
    }

    public Set<Produserbaredokumenter> utledTilgjengeligeMaler() {
        return DOKUMENTPRODUKSJONS_INFO_MAP.keySet().stream()
            .filter(key -> unleash.isEnabled("melosys.brev." + key.name()))
            .collect(toSet());
    }

    public String hentMalnavn(Produserbaredokumenter produserbartDokument) {
        sjekkOmStøttetDokument(produserbartDokument);

        DokumentproduksjonsInfo dokumentproduksjonsInfo = DOKUMENTPRODUKSJONS_INFO_MAP.get(produserbartDokument);
        return dokumentproduksjonsInfo.dokgenMalnavn();
    }

    public DokumentproduksjonsInfo hentDokumentproduksjonsInfo(Produserbaredokumenter produserbartDokument) {
        sjekkOmStøttetDokument(produserbartDokument);

        return DOKUMENTPRODUKSJONS_INFO_MAP.get(produserbartDokument);
    }

    private void sjekkOmStøttetDokument(Produserbaredokumenter produserbartDokument) {
        if (!DOKUMENTPRODUKSJONS_INFO_MAP.containsKey(produserbartDokument)) {
            throw new FunksjonellException(format("ProduserbartDokument %s er ikke støttet", produserbartDokument));
        }
    }

    private enum JournalforingsTittel {
        FORVALTNINGSMELDING("Melding om forventet saksbehandlingstid"),
        MANGELBREV("Melding om manglende opplysninger"),
        INNVILGELSE_FTRL_2_8("Vedtak om frivillig medlemskap"),
        ATTEST_NO_UK_1("Attest medlemskap folketrygden uk");

        private String tittel;

        public String getTittel() {
            return tittel;
        }

        JournalforingsTittel(String tittel) {
            this.tittel = tittel;
        }
    }
}
