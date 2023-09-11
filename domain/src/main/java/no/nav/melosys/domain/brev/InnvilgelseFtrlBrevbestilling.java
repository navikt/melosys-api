package no.nav.melosys.domain.brev;

public class InnvilgelseFtrlBrevbestilling extends DokgenBrevbestilling {
    private String innledningFritekst;
    private String begrunnelseFritekst;
    private String trygdeavgiftFritekst;

    public InnvilgelseFtrlBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private InnvilgelseFtrlBrevbestilling(Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
        this.begrunnelseFritekst = builder.begrunnelseFritekst;
        this.trygdeavgiftFritekst = builder.trygdeavgiftFritekst;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getTrygdeavgiftFritekst() {
        return trygdeavgiftFritekst;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String innledningFritekst;
        private String begrunnelseFritekst;
        private String trygdeavgiftFritekst;

        public Builder() {
        }

        public Builder(InnvilgelseFtrlBrevbestilling innvilgelseBrevbestilling) {
            super(innvilgelseBrevbestilling);
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst;
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst;
            this.trygdeavgiftFritekst = innvilgelseBrevbestilling.trygdeavgiftFritekst;
        }

        public Builder medInnledningFritekst(String innledningFritekst) {
            this.innledningFritekst = innledningFritekst;
            return this;
        }

        public Builder medBegrunnelseFritekst(String begrunnelseFritekst) {
            this.begrunnelseFritekst = begrunnelseFritekst;
            return this;
        }

        public Builder medTrygdeavgiftFritekst(String trygdeavgiftFritekst) {
            this.trygdeavgiftFritekst = trygdeavgiftFritekst;
            return this;
        }

        public InnvilgelseFtrlBrevbestilling build() {
            return new InnvilgelseFtrlBrevbestilling(this);
        }
    }
}
