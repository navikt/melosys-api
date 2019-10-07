package no.nav.melosys.domain.arkiv;

public class FysiskDokument extends ArkivDokument {

    private byte[] data;
    private Filtype filtype;
    private String variantFormat;
    private String brevkode;
    private String dokumentKategori;

    public FysiskDokument() {
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Filtype getFiltype() {
        return filtype;
    }

    public void setFiltype(Filtype filtype) {
        this.filtype = filtype;
    }

    public String getVariantFormat() {
        return variantFormat;
    }

    public void setVariantFormat(String variantFormat) {
        this.variantFormat = variantFormat;
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

    public enum Filtype {
        PDF,
        PDFA,
        XML,
        RTF,
        AFP,
        META,
        DLF,
        JPEG,
        TIFF,
        DOC,
        DOCX,
        XLS,
        XLSX,
        AXML,
        DXML,
        JSON,
        PNG
    }
}
