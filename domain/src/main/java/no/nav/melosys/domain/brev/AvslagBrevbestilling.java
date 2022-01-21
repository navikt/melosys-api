package no.nav.melosys.domain.brev;

public class AvslagBrevbestilling extends DokgenBrevbestilling {

    private String fritekst;

    public AvslagBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public AvslagBrevbestilling(AvslagBrevbestilling.Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
    }

    public AvslagBrevbestilling(AvslagBrevbestilling.Builder builder, String fritekst) {
        super(builder);
        this.fritekst = fritekst;
    }

    public String getFritekst() {
        return fritekst;
    }

    public AvslagBrevbestilling.Builder toBuilder() {
        return new AvslagBrevbestilling.Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<AvslagBrevbestilling.Builder> {
        private String fritekst;

        public Builder() {
        }

        public Builder(AvslagBrevbestilling fritekstbrevBrevbestilling) {
            super(fritekstbrevBrevbestilling);
            this.fritekst = fritekstbrevBrevbestilling.fritekst;
        }

        public AvslagBrevbestilling.Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public AvslagBrevbestilling build() {
            return new AvslagBrevbestilling(this);
        }
    }
}
