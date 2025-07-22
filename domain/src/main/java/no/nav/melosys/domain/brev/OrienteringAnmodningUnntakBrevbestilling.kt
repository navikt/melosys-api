package no.nav.melosys.domain.brev;

public class OrienteringAnmodningUnntakBrevbestilling extends DokgenBrevbestilling {
    private String anmodningUnntakFritekst;

    public OrienteringAnmodningUnntakBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }


    public String getAnmodningUnntakFritekst() {
        return anmodningUnntakFritekst;
    }

    public OrienteringAnmodningUnntakBrevbestilling.Builder toBuilder() {
        return new OrienteringAnmodningUnntakBrevbestilling.Builder(this);
    }

    private OrienteringAnmodningUnntakBrevbestilling(Builder builder) {
        super(builder);
        this.anmodningUnntakFritekst = builder.anmodningUnntakFritekst;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String anmodningUnntakFritekst;

        public Builder() {
        }

        public Builder(OrienteringAnmodningUnntakBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.anmodningUnntakFritekst = brevbestilling.anmodningUnntakFritekst;
        }

        public OrienteringAnmodningUnntakBrevbestilling build() {
            return new OrienteringAnmodningUnntakBrevbestilling(this);
        }


        public Builder medFritekst(String fritekst) {
            this.anmodningUnntakFritekst = fritekst;
            return this;
        }
    }
}
