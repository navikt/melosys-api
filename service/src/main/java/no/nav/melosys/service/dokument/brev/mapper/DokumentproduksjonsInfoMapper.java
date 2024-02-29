package no.nav.melosys.service.dokument.brev.mapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.dokument.VedleggTyper;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class DokumentproduksjonsInfoMapper {
    private static final ImmutableMap<Produserbaredokumenter, DokumentproduksjonsInfo> DOKUMENTPRODUKSJONS_INFO_MAP;

    static {
        DOKUMENTPRODUKSJONS_INFO_MAP = Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, DokumentproduksjonsInfo>builder()
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
            .put(INNVILGELSE_FOLKETRYGDLOVEN,
                new DokumentproduksjonsInfo("innvilgelse_ftrl",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_FTRL.getTittel())
            )
            .put(VEDTAK_OPPHOERT_MEDLEMSKAP,
                new DokumentproduksjonsInfo("vedtak_opphoert_medlemskap",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.VEDTAK_OPPHOERT_MEDLEMSKAP.getTittel())
            )
            .put(TRYGDEAVTALE_GB,
                new DokumentproduksjonsInfo("trygdeavtale_gb",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    Map.of(VedleggTyper.VEDTAKSBREV, JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                        VedleggTyper.ATTEST, JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel())))
            .put(TRYGDEAVTALE_US,
                new DokumentproduksjonsInfo("trygdeavtale_us",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    Map.of(VedleggTyper.VEDTAKSBREV, JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                        VedleggTyper.ATTEST, JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel())))
            .put(TRYGDEAVTALE_CAN,
                new DokumentproduksjonsInfo("trygdeavtale_ca",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    Map.of(VedleggTyper.VEDTAKSBREV, JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                        VedleggTyper.ATTEST, JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel())))
            .put(TRYGDEAVTALE_AU,
                new DokumentproduksjonsInfo("trygdeavtale_au",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    Map.of(VedleggTyper.VEDTAKSBREV, JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                        VedleggTyper.ATTEST, JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel())))
            .put(GENERELT_FRITEKSTBREV_BRUKER,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode()))
            .put(GENERELT_FRITEKSTBREV_VIRKSOMHET,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode()))
            .put(GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode()))
            .put(FRITEKSTBREV,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode()))
            .put(GENERELT_FRITEKSTVEDLEGG,
                new DokumentproduksjonsInfo("fritekstvedlegg",
                    DokumentKategoriKode.IB.getKode()))
            .put(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
                new DokumentproduksjonsInfo("trygdeavtale_fritekstbrev",
                    DokumentKategoriKode.IB.getKode()))
            .put(AVSLAG_MANGLENDE_OPPLYSNINGER,
                new DokumentproduksjonsInfo("avslag_manglende_opplysninger",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.AVSLAG_MANGLENDE_OPPLYSNINGER.getTittel()))
            .put(MELDING_HENLAGT_SAK,
                new DokumentproduksjonsInfo("henleggelse",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MELDING_HENLAGT_SAK.getTittel()))
            .put(IKKE_YRKESAKTIV_VEDTAKSBREV,
                new DokumentproduksjonsInfo("ikke_yrkesaktiv_vedtaksbrev",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.IKKE_YRKESAKTIV_VEDTAKSBREV.getTittel()))
            .put(IKKE_YRKESAKTIV_PLIKTIG_FTRL,
                new DokumentproduksjonsInfo("ikke_yrkesaktiv_frivillig_ftrl",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.IKKE_YRKESAKTIV_FRIVILLIG_FTRL.getTittel()))
            .put(VARSELBREV_MANGLENDE_INNBETALING,
                new DokumentproduksjonsInfo("varsel_manglende_innbetaling",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.VARSELBREV_MANGLENDE_INNBETALING.getTittel()))
            .build());
    }

    public Set<Produserbaredokumenter> tilgjengeligeMalerIDokgen() {
        return new HashSet<>(DOKUMENTPRODUKSJONS_INFO_MAP.keySet());
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
        INNVILGELSE_FTRL("Vedtak om frivillig medlemskap"),
        VEDTAK_OPPHOERT_MEDLEMSKAP("Vedtak om opphør av frivillig medlemskap"),
        TRYGDEAVTALE("Vedtak om medlemskap, Attest for medlemskap i folketrygden"),
        TRYGDEAVTALE_VEDTAKSBREV("Vedtak om medlemskap"),
        TRYGDEAVTALE_ATTEST("Attest for medlemskap i folketrygden"),
        AVSLAG_MANGLENDE_OPPLYSNINGER("Avslag pga manglende opplysninger"),
        MELDING_HENLAGT_SAK("Henleggelse av søknad"),
        IKKE_YRKESAKTIV_VEDTAKSBREV("Vedtak om medlemskap"),
        IKKE_YRKESAKTIV_FRIVILLIG_FTRL("Vedtak om frivillig medlemskap"),
        VARSELBREV_MANGLENDE_INNBETALING("Varsel om manglende innbetaling av trygdeavgift");

        private final String tittel;

        public String getTittel() {
            return tittel;
        }

        JournalforingsTittel(String tittel) {
            this.tittel = tittel;
        }
    }
}
