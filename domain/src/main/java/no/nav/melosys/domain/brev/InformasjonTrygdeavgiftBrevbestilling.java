package no.nav.melosys.domain.brev;

public class InformasjonTrygdeavgiftBrevbestilling extends DokgenBrevbestilling {
    private String innledningFritekst;

    public InformasjonTrygdeavgiftBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }


    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public InformasjonTrygdeavgiftBrevbestilling.Builder toBuilder() {
        return new InformasjonTrygdeavgiftBrevbestilling.Builder(this);
    }

    private InformasjonTrygdeavgiftBrevbestilling(Builder builder) {
        super(builder);
        this.innledningFritekst = builder.innledningFritekst;
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String innledningFritekst;

        public Builder() {
        }

        public Builder(InformasjonTrygdeavgiftBrevbestilling brevbestilling) {
            super(brevbestilling);
            this.innledningFritekst = brevbestilling.innledningFritekst;
        }

        public InformasjonTrygdeavgiftBrevbestilling build() {
            return new InformasjonTrygdeavgiftBrevbestilling(this);
        }


        public Builder medFritekst(String fritekst) {
            this.innledningFritekst = fritekst;
            return this;
        }
    }
}
