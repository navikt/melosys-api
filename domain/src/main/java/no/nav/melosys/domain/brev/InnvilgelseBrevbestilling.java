package no.nav.melosys.domain.brev;

public class InnvilgelseBrevbestilling extends DokgenBrevbestilling {
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String ektefelleFritekst;
    private String barnFritekst;

    public InnvilgelseBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelseBrevbestilling(InnvilgelseBrevbestilling.Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.ektefelleFritekst = builder.ektefelleFritekst;
        this.barnFritekst = builder.barnFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getEktefelleFritekst() {
        return ektefelleFritekst;
    }

    public String getBarnFritekst() {
        return barnFritekst;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String ektefelleFritekst;
        private String barnFritekst;

        public Builder() {
        }

        public Builder(InnvilgelseBrevbestilling innvilgelseBrevbestilling) {
            super(innvilgelseBrevbestilling);
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst;
            this.ektefelleFritekst = innvilgelseBrevbestilling.ektefelleFritekst;
            this.barnFritekst = innvilgelseBrevbestilling.barnFritekst;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medEktefelleFritekst(String ektefelleFritekst) {
            this.ektefelleFritekst = ektefelleFritekst;
            return this;
        }

        public Builder medBarnFritekst(String barnFritekst) {
            this.barnFritekst = barnFritekst;
            return this;
        }

        public InnvilgelseBrevbestilling build() {
            return new InnvilgelseBrevbestilling(this);
        }
    }
}
