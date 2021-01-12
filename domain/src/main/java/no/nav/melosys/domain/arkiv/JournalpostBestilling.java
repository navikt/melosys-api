package no.nav.melosys.domain.arkiv;

public final class JournalpostBestilling {
    private final String tittel;
    private final String brevkode;
    private final String brukerFnr;
    private final String mottakerNavn;
    private final String mottakerId;
    private final boolean erMottakerOrg;
    private final String arkivSakId;
    private final byte[] pdf;

    private JournalpostBestilling(Builder builder) {
        this.tittel = builder.tittel;
        this.brevkode = builder.brevkode;
        this.brukerFnr = builder.brukerFnr;
        this.mottakerNavn = builder.mottakerNavn;
        this.mottakerId = builder.mottakerId;
        this.erMottakerOrg = builder.erMottakerOrg;
        this.arkivSakId = builder.arkivSakId;
        this.pdf = builder.pdf;
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

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getMottakerId() {
        return mottakerId;
    }

    public boolean erMottakerOrg() {
        return erMottakerOrg;
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
        private String mottakerNavn;
        private String mottakerId;
        private boolean erMottakerOrg;
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

        public Builder medMottakerNavn(String mottakerNavn) {
            this.mottakerNavn = mottakerNavn;
            return this;
        }

        public Builder medMottakerId(String mottakerId) {
            this.mottakerId = mottakerId;
            return this;
        }

        public Builder medErMottakerOrg(boolean erMottakerOrg) {
            this.erMottakerOrg = erMottakerOrg;
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
            return new JournalpostBestilling(this);
        }
    }
}
