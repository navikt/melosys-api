package no.nav.melosys.domain.arkiv;

public record DokumentVariant(byte[] data, Filtype filtype,
                              VariantFormat variantFormat,
                              boolean saksbehandlerHarTilgang) {


    public DokumentVariant(VariantFormat variantFormat, boolean saksbehandlerHarTilgang) {
        this(null, null, variantFormat, saksbehandlerHarTilgang);
    }

    public static DokumentVariant lagDokumentVariant(byte[] data) {
        return new DokumentVariant(
            data,
            DokumentVariant.Filtype.PDFA,
            VariantFormat.ARKIV,
            true
        );
    }

    public static DokumentVariant lagDokumentVariant(byte[] data,
                                                     Filtype filtype,
                                                     VariantFormat variantFormat) {
        return new DokumentVariant(
            data,
            filtype,
            variantFormat,
            true
        );
    }

    public boolean erVariantArkiv() {
        return variantFormat == DokumentVariant.VariantFormat.ARKIV;
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

    public boolean getSaksbehandlerHarTilgang() {
        return saksbehandlerHarTilgang;
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
        ORIGINAL,
        // Brukes ikke ved opprettelse fra Melosys:
        BREVBESTILLING,
        FULLVERSJON,
        PRODUKSJON,
        SKANNING_META,
        PRODUKSJON_DLF,
        SLADDET
    }
}
