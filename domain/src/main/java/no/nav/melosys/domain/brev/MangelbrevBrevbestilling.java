package no.nav.melosys.domain.brev;

public class MangelbrevBrevbestilling extends DokgenBrevbestilling {
    private final String fritekstMangelInfo;
    private final String fritekstMottaksInfo;
    private final String fullmektigNavn;

    private MangelbrevBrevbestilling(MangelbrevBrevbestilling.Builder builder) {
        super(builder);
        this.fritekstMangelInfo = builder.fritekstMangelInfo;
        this.fritekstMottaksInfo = builder.fritekstMottaksInfo;
        this.fullmektigNavn = builder.fullmektigNavn;
    }

    public String getFritekstMangelInfo() {
        return fritekstMangelInfo;
    }

    public String getFritekstMottaksInfo() {
        return fritekstMottaksInfo;
    }

    public String getFullmektigNavn() {
        return fullmektigNavn;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekstMangelInfo;
        private String fritekstMottaksInfo;
        private String fullmektigNavn;

        public Builder() {
        }

        public Builder(MangelbrevBrevbestilling mangelbrevBrevbestilling) {
            super(mangelbrevBrevbestilling);
            this.fritekstMangelInfo = mangelbrevBrevbestilling.fritekstMangelInfo;
            this.fritekstMottaksInfo = mangelbrevBrevbestilling.fritekstMottaksInfo;
            this.fullmektigNavn = mangelbrevBrevbestilling.fullmektigNavn;
        }

        public Builder medFritekstMangelInfo(String fritekstMangelInfo) {
            this.fritekstMangelInfo = fritekstMangelInfo;
            return this;
        }

        public Builder medFritekstMottaksInfo(String fritekstMottaksInfo) {
            this.fritekstMottaksInfo = fritekstMottaksInfo;
            return this;
        }

        public Builder medFullmektigNavn(String fullmektigNavn) {
            this.fullmektigNavn = fullmektigNavn;
            return this;
        }

        public MangelbrevBrevbestilling build() {
            return new MangelbrevBrevbestilling(this);
        }
    }
}
