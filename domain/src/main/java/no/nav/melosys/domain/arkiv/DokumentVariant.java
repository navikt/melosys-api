package no.nav.melosys.domain.arkiv;

public class DokumentVariant {
    private final byte[] data;
    private final Filtype filtype;
    private final VariantFormat variantFormat;

    private DokumentVariant(byte[] data, Filtype filtype, VariantFormat variantFormat) {
        this.data = data;
        this.filtype = filtype;
        this.variantFormat = variantFormat;
    }

    public static DokumentVariant lagDokumentVariant(byte[] data) {
        return new DokumentVariant(
            data,
            DokumentVariant.Filtype.PDFA,
            VariantFormat.ARKIV
        );
    }

    public static DokumentVariant lagDokumentVariant(byte[] data,
                                                     Filtype filtype,
                                                     VariantFormat variantFormat) {
        return new DokumentVariant(
            data,
            filtype,
            variantFormat
        );
    }

    public byte[] getData() {
        return data;
    }

    public Filtype getFiltype() {
        return filtype;
    }

    public VariantFormat getVariantFormat() {
        return variantFormat;
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

    public enum VariantFormat {
        ARKIV,
        ORIGINAL
    }
}
