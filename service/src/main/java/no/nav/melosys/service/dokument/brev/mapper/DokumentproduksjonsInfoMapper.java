package no.nav.melosys.service.dokument.brev.mapper;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
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
                    JournalforingsTittel.INNVILGELSE_FTRL_FRIVILLIG.getTittel())
            )
            .put(VEDTAK_OPPHOERT_MEDLEMSKAP,
                new DokumentproduksjonsInfo("vedtak_opphoert_medlemskap",
                    DokumentKategoriKode.VB.getKode(),
                    VEDTAK_OPPHOERT_MEDLEMSKAP.getBeskrivelse())
            )
            .put(TRYGDEAVTALE_GB,
                new DokumentproduksjonsInfo("trygdeavtale_gb",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel()))
            .put(TRYGDEAVTALE_US,
                new DokumentproduksjonsInfo("trygdeavtale_us",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel()))
            .put(TRYGDEAVTALE_CAN,
                new DokumentproduksjonsInfo("trygdeavtale_ca",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel()))
            .put(TRYGDEAVTALE_AU,
                new DokumentproduksjonsInfo("trygdeavtale_au",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.TRYGDEAVTALE.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_VEDTAKSBREV.getTittel(),
                    JournalforingsTittel.TRYGDEAVTALE_ATTEST.getTittel()))
            .put(INNHENTING_AV_INNTEKTSOPPLYSNINGER,
                new DokumentproduksjonsInfo("innhenting_av_inntektsopplysninger",
                    DokumentKategoriKode.IB.getKode(),
                    INNHENTING_AV_INNTEKTSOPPLYSNINGER.getBeskrivelse()))
            .put(ORIENTERING_ANMODNING_UNNTAK,
                new DokumentproduksjonsInfo("orientering_anmodning_unntak",
                    DokumentKategoriKode.IB.getKode(),
                    ORIENTERING_ANMODNING_UNNTAK.getBeskrivelse()))
            .put(ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK,
                new DokumentproduksjonsInfo("orientering_til_arbeidsgiver_om_vedtak",
                    DokumentKategoriKode.IB.getKode(),
                    ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK.getBeskrivelse()))
            .put(GENERELT_FRITEKSTBREV_BRUKER,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    GENERELT_FRITEKSTBREV_BRUKER.getBeskrivelse()))
            .put(GENERELT_FRITEKSTBREV_VIRKSOMHET,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    GENERELT_FRITEKSTBREV_VIRKSOMHET.getBeskrivelse()))
            .put(GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    GENERELT_FRITEKSTBREV_ARBEIDSGIVER.getBeskrivelse()))
            .put(FRITEKSTBREV,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    FRITEKSTBREV.getBeskrivelse()))
            .put(GENERELT_FRITEKSTVEDLEGG,
                new DokumentproduksjonsInfo("fritekstvedlegg",
                    DokumentKategoriKode.IB.getKode(),
                    GENERELT_FRITEKSTVEDLEGG.getBeskrivelse()))
            .put(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
                new DokumentproduksjonsInfo("trygdeavtale_fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV.getBeskrivelse()))
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
                new DokumentproduksjonsInfo("ikke_yrkesaktiv_pliktig_ftrl",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_FTRL_PLIKTIG.getTittel()))
            .put(INNVILGELSE_EFTA_STORBRITANNIA,
                new DokumentproduksjonsInfo("innvilgelse_efta_storbritannia",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_EFTA_STORBRITANNIA.getTittel()))
            .put(IKKE_YRKESAKTIV_FRIVILLIG_FTRL,
                new DokumentproduksjonsInfo("ikke_yrkesaktiv_frivillig_ftrl",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_FTRL_FRIVILLIG.getTittel()))
            .put(PLIKTIG_MEDLEM_FTRL,
                new DokumentproduksjonsInfo("pliktig_medlem_ftrl",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_FTRL_PLIKTIG.getTittel()))
            .put(VARSELBREV_MANGLENDE_INNBETALING,
                new DokumentproduksjonsInfo("varsel_manglende_innbetaling",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MELDING_MANGLENDE_INNBETALING.getTittel(),
                    JournalforingsTittel.VARSEL_OPPHØRT_MEDLEMSKAP.getTittel()))
            .put(AVSLAG_EFTA_STORBRITANNIA,
                new DokumentproduksjonsInfo("avslag_efta_storbritannia",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.AVSLAG_EFTA_STORBRITANNIA.getTittel()))
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
        INNVILGELSE_FTRL_FRIVILLIG("Vedtak om frivillig medlemskap"),
        INNVILGELSE_FTRL_PLIKTIG("Vedtak om pliktig medlemskap"),
        INNVILGELSE_EFTA_STORBRITANNIA("Vedtak om medlemskap"),
        TRYGDEAVTALE("Vedtak om medlemskap, Attest for medlemskap i folketrygden"),
        TRYGDEAVTALE_VEDTAKSBREV("Vedtak om medlemskap"),
        TRYGDEAVTALE_ATTEST("Attest for medlemskap i folketrygden"),
        AVSLAG_MANGLENDE_OPPLYSNINGER("Avslag pga manglende opplysninger"),
        MELDING_HENLAGT_SAK("Henleggelse av søknad"),
        IKKE_YRKESAKTIV_VEDTAKSBREV("Vedtak om medlemskap"),
        MELDING_MANGLENDE_INNBETALING("Melding om manglende innbetaling av trygdeavgift"),
        VARSEL_OPPHØRT_MEDLEMSKAP("Varsel om opphør av frivillig medlemskap"),
        AVSLAG_EFTA_STORBRITANNIA("Avslag på søknad om medlemskap");

        private final String tittel;

        public String getTittel() {
            return tittel;
        }

        JournalforingsTittel(String tittel) {
            this.tittel = tittel;
        }
    }
}
