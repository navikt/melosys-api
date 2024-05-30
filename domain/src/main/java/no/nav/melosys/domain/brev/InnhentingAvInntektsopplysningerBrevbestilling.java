package no.nav.melosys.domain.brev;

public class InnhentingAvInntektsopplysningerBrevbestilling extends DokgenBrevbestilling {

    private boolean skalViseStandardTekstOmOpplysninger;
    private String fritekst;

    public InnhentingAvInntektsopplysningerBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public boolean getSkalViseStandardTekstOmOpplysninger() {
        return skalViseStandardTekstOmOpplysninger;
    }

    public String getFritekst() {
        return fritekst;
    }

    public InnhentingAvInntektsopplysningerBrevbestilling.Builder toBuilder() {
        return new InnhentingAvInntektsopplysningerBrevbestilling.Builder(this);
    }

    private InnhentingAvInntektsopplysningerBrevbestilling(Builder builder) {
        super(builder);
        this.skalViseStandardTekstOmOpplysninger = builder.skalViseStandardTekstOmOpplysninger;
        this.fritekst = builder.fritekst;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private boolean skalViseStandardTekstOmOpplysninger;
        private String fritekst;

        public Builder() {
        }

        public Builder(InnhentingAvInntektsopplysningerBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.skalViseStandardTekstOmOpplysninger = brevbestilling.skalViseStandardTekstOmOpplysninger;
            this.fritekst = brevbestilling.fritekst;
        }

        public InnhentingAvInntektsopplysningerBrevbestilling build() {
            return new InnhentingAvInntektsopplysningerBrevbestilling(this);
        }

        public Builder medSkalViseStandardTekstOmOpplysninger(boolean skalViseStandardTekstOmOpplysninger) {
            this.skalViseStandardTekstOmOpplysninger = skalViseStandardTekstOmOpplysninger;
            return this;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }
    }
}
