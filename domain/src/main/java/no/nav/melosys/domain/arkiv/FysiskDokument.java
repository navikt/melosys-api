package no.nav.melosys.domain.arkiv;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.msm.AltinnDokument;

import static no.nav.melosys.domain.arkiv.DokumentVariant.lagDokumentVariant;

public class FysiskDokument extends ArkivDokument {
    private static final String DOKUMENT_KATEGORI_SED = "SED";
    private static final String DOKUMENT_KATEGORI_SOKNAD = "SOK";
    private static final String DOKUMENT_KATEGORI_INFOBREV = "IB";

    private List<DokumentVariant> dokumentVarianter;
    private String brevkode;
    private String dokumentKategori;

    static FysiskDokument lagFysiskDokumentSed(SedType sedType, byte[] sedPdf) {
        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DOKUMENT_KATEGORI_SED);
        fysiskDokument.setTittel(hentTittelForSedType(sedType));
        fysiskDokument.setBrevkode(sedType.name());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagDokumentVariant(sedPdf)));
        return fysiskDokument;
    }

    static FysiskDokument lagFysiskHovedDokumentAltinn(AltinnDokument altinnDokument,
                                                  Behandlingsgrunnlag behandlingsgrunnlag) {
        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DOKUMENT_KATEGORI_SOKNAD);
        fysiskDokument.setTittel(hentTittelForAltinnDokument(altinnDokument.getDokumentType()));
        byte[] innhold = Base64.getDecoder().decode(altinnDokument.getInnhold());
        var dokumentVarianter = List.of(
            lagDokumentVariant(innhold),
            lagDokumentVariant(
                behandlingsgrunnlag.getOriginalData().getBytes(StandardCharsets.UTF_8),
                DokumentVariant.VariantFormat.ORIGINAL
            )
        );
        fysiskDokument.setDokumentVarianter(dokumentVarianter);
        return fysiskDokument;
    }

    static FysiskDokument lagFysiskDokumentAltinn(AltinnDokument altinnDokument) {
        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DOKUMENT_KATEGORI_SOKNAD);
        fysiskDokument.setTittel(hentTittelForAltinnDokument(altinnDokument.getDokumentType()));
        byte[] innhold = Base64.getDecoder().decode(altinnDokument.getInnhold());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagDokumentVariant(innhold)));
        return fysiskDokument;
    }

    static FysiskDokument lagInfoBrevPdf(String tittel, byte[] pdf) {
        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DOKUMENT_KATEGORI_INFOBREV);
        fysiskDokument.setTittel(tittel);
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagDokumentVariant(pdf)));
        return fysiskDokument;
    }

    private static String hentTittelForSedType(SedType sedType) {
        switch (sedType) {
            case A002:
                return "Delvis eller fullt avslag på søknad om unntak";
            case A003:
                return "Beslutning om lovvalg";
            case A008:
                return "Melding om relevant informasjon";
            case A011:
                return "Innvilgelse av søknad om unntak";
            default:
                throw new IllegalArgumentException("Kan ikke opprette journalpost av sed-type " + sedType);
        }
    }

    private static String hentTittelForAltinnDokument(AltinnDokument.AltinnDokumentType dokumentType) {
        switch (dokumentType) {
            case SOKNAD:
                return "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits";
            case FULLMAKT:
                return "Fullmakt";
            default:
                throw new IllegalArgumentException("Ukjent AltinnDokumentType " + dokumentType);

        }
    }

    public List<DokumentVariant> getDokumentVarianter() {
        return dokumentVarianter;
    }

    public void setDokumentVarianter(List<DokumentVariant> dokumentVarianter) {
        this.dokumentVarianter = dokumentVarianter;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public void setBrevkode(String brevkode) {
        this.brevkode = brevkode;
    }

    public String getDokumentKategori() {
        return dokumentKategori;
    }

    public void setDokumentKategori(String dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
    }
}
