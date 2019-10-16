package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

public class DokumentVariant {

    private JournalpostFiltype filtype = JournalpostFiltype.PDFA;

    /**
     * "ARKIV brukes for dokumentvarianter i menneskelesbart format (for eksempel PDF/A).  Gosys og
     * nav.no henter arkivvariant og viser denne til bruker.\n" + "ORIGINAL skal brukes for
     * dokumentvariant i maskinlesbart format (for eksempel XML og JSON) som brukes for automatisk
     * saksbehandling\n" + "Alle dokumenter må ha én variant med variantFormat ARKIV."
     */
    private String variantformat;
    private byte[] fysiskDokument;

    public DokumentVariant(JournalpostFiltype filtype, String variantformat, byte[] fysiskDokument) {
        this.filtype = filtype;
        this.variantformat = variantformat;
        this.fysiskDokument = fysiskDokument;
    }

    public DokumentVariant() {
    }

    public static DokumentVariantBuilder builder() {
        return new DokumentVariantBuilder();
    }

    public JournalpostFiltype getFiltype() {
        return this.filtype;
    }

    public String getVariantformat() {
        return this.variantformat;
    }

    public byte[] getFysiskDokument() {
        return this.fysiskDokument;
    }

    public static class DokumentVariantBuilder {
        private JournalpostFiltype filtype;
        private String variantformat;
        private byte[] fysiskDokument;

        DokumentVariantBuilder() {
        }

        public DokumentVariant.DokumentVariantBuilder filtype(JournalpostFiltype filtype) {
            this.filtype = filtype;
            return this;
        }

        public DokumentVariant.DokumentVariantBuilder variantformat(String variantformat) {
            this.variantformat = variantformat;
            return this;
        }

        public DokumentVariant.DokumentVariantBuilder fysiskDokument(byte[] fysiskDokument) {
            this.fysiskDokument = fysiskDokument;
            return this;
        }

        public DokumentVariant build() {
            return new DokumentVariant(filtype, variantformat, fysiskDokument);
        }
    }
}
