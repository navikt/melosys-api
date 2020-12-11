package no.nav.melosys.domain.arkiv;

public final class JournalpostBestilling {
    private final String tittel;
    private final String brevkode;
    private final String brukerFnr;
    private final String avsenderNavn;
    private final String avsenderId;
    private final boolean erAvsenderOrg;
    private final String arkivSakId;
    private final byte[] pdf;

    private JournalpostBestilling(String tittel, String brevkode, String brukerFnr, String avsenderNavn,
                                 String avsenderId, boolean erAvsenderOrg, String arkivSakId, byte[] pdf) {
        this.tittel = tittel;
        this.brevkode = brevkode;
        this.brukerFnr = brukerFnr;
        this.avsenderNavn = avsenderNavn;
        this.avsenderId = avsenderId;
        this.erAvsenderOrg = erAvsenderOrg;
        this.arkivSakId = arkivSakId;
        this.pdf = pdf;
    }

    public String getTittel() {
        return tittel;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public String getBrukerFnr() {
        return brukerFnr;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public String getAvsenderId() {
        return avsenderId;
    }

    public boolean erAvsenderOrg() {
        return erAvsenderOrg;
    }

    public String getArkivSakId() {
        return arkivSakId;
    }

    public byte[] getPdf() {
        return pdf;
    }

    public static class Builder {
        private String tittel;
        private String brevkode;
        private String brukerFnr;
        private String avsenderNavn;
        private String avsenderId;
        private boolean erAvsenderOrg;
        private String arkivSakId;
        private byte[] pdf;

        public Builder medTittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public Builder medBrevkode(String brevkode) {
            this.brevkode = brevkode;
            return this;
        }

        public Builder medBrukerFnr(String brukerFnr) {
            this.brukerFnr = brukerFnr;
            return this;
        }

        public Builder medAvsenderNavn(String avsenderNavn) {
            this.avsenderNavn = avsenderNavn;
            return this;
        }

        public Builder medAvsenderId(String avsenderId) {
            this.avsenderId = avsenderId;
            return this;
        }

        public Builder medErAvsenderOrg(boolean erAvsenderOrg) {
            this.erAvsenderOrg = erAvsenderOrg;
            return this;
        }

        public Builder medArkivSakId(String arkivSakId) {
            this.arkivSakId = arkivSakId;
            return this;
        }

        public Builder medPdf(byte[] pdf) {
            this.pdf = pdf;
            return this;
        }

        public JournalpostBestilling build() {
            return new JournalpostBestilling(tittel, brevkode, brukerFnr, avsenderNavn, avsenderId, erAvsenderOrg, arkivSakId, pdf);
        }
    }
}
