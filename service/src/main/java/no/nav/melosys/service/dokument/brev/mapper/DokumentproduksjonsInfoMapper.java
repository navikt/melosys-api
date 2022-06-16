package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.dokument.VedleggTyper;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class DokumentproduksjonsInfoMapper {
    private final Unleash unleash;

    private static final ImmutableMap<Produserbaredokumenter, DokumentproduksjonsInfo> DOKUMENTPRODUKSJONS_INFO_MAP;
    static final Set<Produserbaredokumenter> DOKUMENTMALER_PRODSATT = Set.of(
        AVSLAG_MANGLENDE_OPPLYSNINGER,
        GENERELT_FRITEKSTBREV_BRUKER,
        GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
        MANGELBREV_ARBEIDSGIVER,
        MANGELBREV_BRUKER,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
        STORBRITANNIA,
        MELDING_HENLAGT_SAK
    );

    static {
        DOKUMENTPRODUKSJONS_INFO_MAP = Maps.immutableEnumMap(ImmutableMap.<Produserbaredokumenter, DokumentproduksjonsInfo>builder()
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
                new DokumentproduksjonsInfo("saksbehandlingstid_soknad",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.FORVALTNINGSMELDING.getTittel(), null)
            )
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                new DokumentproduksjonsInfo("saksbehandlingstid_soknad",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.FORVALTNINGSMELDING.getTittel(), null)
            )
            .put(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
                new DokumentproduksjonsInfo("saksbehandlingstid_klage",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.FORVALTNINGSMELDING.getTittel(), null)
            )
            .put(MANGELBREV_BRUKER,
                new DokumentproduksjonsInfo("mangelbrev_bruker",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MANGELBREV.getTittel(), null)
            )
            .put(MANGELBREV_ARBEIDSGIVER,
                new DokumentproduksjonsInfo("mangelbrev_arbeidsgiver",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MANGELBREV.getTittel(), null)
            )
            .put(INNVILGELSE_FOLKETRYGDLOVEN_2_8,
                new DokumentproduksjonsInfo("innvilgelse_ftrl_2_8",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.INNVILGELSE_FTRL_2_8.getTittel(), null)
            )
            .put(STORBRITANNIA,
                new DokumentproduksjonsInfo("uk_innvilgelse_og_attest",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.STORBRITANNIA.getTittel(),
                    Map.of(VedleggTyper.VEDTAKSBREV, JournalforingsTittel.STORBRITANNIA_VEDTAKSBREV.getTittel(),
                        VedleggTyper.ATTEST, JournalforingsTittel.STORBRITANNIA_ATTEST.getTittel())))
            .put(GENERELT_FRITEKSTBREV_BRUKER,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    null, null))
            .put(GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
                new DokumentproduksjonsInfo("fritekstbrev",
                    DokumentKategoriKode.IB.getKode(),
                    null, null))
            .put(AVSLAG_MANGLENDE_OPPLYSNINGER,
                new DokumentproduksjonsInfo("avslag_manglende_opplysninger",
                    DokumentKategoriKode.VB.getKode(),
                    JournalforingsTittel.AVSLAG_MANGLENDE_OPPLYSNINGER.getTittel(), null))
            .put(MELDING_HENLAGT_SAK,
                new DokumentproduksjonsInfo("henleggelse",
                    DokumentKategoriKode.IB.getKode(),
                    JournalforingsTittel.MELDING_HENLAGT_SAK.getTittel(), null))
            .build());
    }

    public DokumentproduksjonsInfoMapper(Unleash unleash) {
        this.unleash = unleash;
    }

    public Set<Produserbaredokumenter> utledTilgjengeligeMaler() {
        return DOKUMENTPRODUKSJONS_INFO_MAP.keySet().stream()
            .filter(isEnabled())
            .collect(toSet());
    }

    private Predicate<Produserbaredokumenter> isEnabled() {
        return key -> DOKUMENTMALER_PRODSATT.contains(key) || unleash.isEnabled("melosys.brev." + key.name());
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
        STORBRITANNIA("Vedtak om medlemskap, Attest for utsendt arbeidstaker"),
        STORBRITANNIA_VEDTAKSBREV("Vedtak om medlemskap"),
        STORBRITANNIA_ATTEST("Attest for utsendt arbeidstaker"),
        AVSLAG_MANGLENDE_OPPLYSNINGER("Avslag pga manglende opplysninger"),
        MELDING_HENLAGT_SAK("Henleggelse av søknad");

        private final String tittel;

        public String getTittel() {
            return tittel;
        }

        JournalforingsTittel(String tittel) {
            this.tittel = tittel;
        }
    }
}
