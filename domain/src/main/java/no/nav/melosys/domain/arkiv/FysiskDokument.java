package no.nav.melosys.domain.arkiv;

import no.nav.melosys.domain.eessi.SedType;

import java.util.Collections;
import java.util.List;

import static no.nav.melosys.domain.arkiv.DokumentVariant.lagArkivVariant;

public class FysiskDokument extends ArkivDokument {
    private static final String DOKUMENT_KATEGORI_SED = "SED";

    private List<DokumentVariant> dokumentVarianter;
    private String brevkode;
    private String dokumentKategori;

    static FysiskDokument lagFysiskDokumentSed(SedType sedType, byte[] sedPdf) {
        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DOKUMENT_KATEGORI_SED);
        fysiskDokument.setTittel(hentTittelForSedType(sedType));
        fysiskDokument.setBrevkode(sedType.name());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagArkivVariant(sedPdf)));
        return fysiskDokument;
    }

    private static String hentTittelForSedType(SedType sedType) {
        switch (sedType) {
            case A002:
                return "Delvis eller fullt avslag på søknad om unntak";
            case A008:
                return "Melding om relevant informasjon";
            case A011:
                return "Innvilgelse av søknad om unntak";
            default:
                throw new IllegalArgumentException("Kan ikke opprette journalpost av sed-type " + sedType);
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
