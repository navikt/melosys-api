package no.nav.melosys.domain.arkiv;

public class DokumentVariant {
    private static final String ARKIV = "ARKIV";

    private byte[] data;
    private Filtype filtype;
    private String variantFormat;

    public static DokumentVariant lagArkivVariant(byte[] pdf) {
        DokumentVariant dokumentVariant = new DokumentVariant();
        dokumentVariant.setVariantFormat(ARKIV);
        dokumentVariant.setFiltype(DokumentVariant.Filtype.PDFA);
        dokumentVariant.setData(pdf);
        return dokumentVariant;
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
