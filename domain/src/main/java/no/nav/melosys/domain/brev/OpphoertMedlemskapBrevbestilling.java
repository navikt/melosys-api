package no.nav.melosys.domain.brev;

public class OpphoertMedlemskapBrevbestilling extends DokgenBrevbestilling {
    private String fritekst;

    public OpphoertMedlemskapBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private OpphoertMedlemskapBrevbestilling(Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
    }

    public String getFritekst() {
        return fritekst;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekst;

        public Builder() {
        }

        public Builder(OpphoertMedlemskapBrevbestilling opphoertMedlemskapBrevbestilling) {
            super(opphoertMedlemskapBrevbestilling);
            this.fritekst = opphoertMedlemskapBrevbestilling.fritekst;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public OpphoertMedlemskapBrevbestilling build() {
            return new OpphoertMedlemskapBrevbestilling(this);
        }
    }
}
