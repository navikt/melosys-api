package no.nav.melosys.domain.brev;

public class OrienteringAnmodningUnntakBrevbestilling extends DokgenBrevbestilling {
    private String fritekst;

    public OrienteringAnmodningUnntakBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }


    public String getFritekst() {
        return fritekst;
    }

    public OrienteringAnmodningUnntakBrevbestilling.Builder toBuilder() {
        return new OrienteringAnmodningUnntakBrevbestilling.Builder(this);
    }

    private OrienteringAnmodningUnntakBrevbestilling(Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekst;

        public Builder() {
        }

        public Builder(OrienteringAnmodningUnntakBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.fritekst = brevbestilling.fritekst;
        }

        public OrienteringAnmodningUnntakBrevbestilling build() {
            return new OrienteringAnmodningUnntakBrevbestilling(this);
        }


        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }
    }
}
