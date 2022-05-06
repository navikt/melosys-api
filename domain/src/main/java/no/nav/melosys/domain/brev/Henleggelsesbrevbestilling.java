package no.nav.melosys.domain.brev;

public class Henleggelsesbrevbestilling extends DokgenBrevbestilling {
    private String fritekst;
    private String begrunnelseKode;

    public Henleggelsesbrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    public Henleggelsesbrevbestilling(Henleggelsesbrevbestilling.Builder builder) {
        super(builder);
        this.fritekst = builder.fritekst;
        this.begrunnelseKode = builder.begrunnelseKode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public String getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String fritekst;
        private String begrunnelseKode;

        public Builder() {
        }

        public Builder(Henleggelsesbrevbestilling fritekstbrevBrevbestilling) {
            super(fritekstbrevBrevbestilling);
            this.fritekst = fritekstbrevBrevbestilling.fritekst;
            this.begrunnelseKode = fritekstbrevBrevbestilling.begrunnelseKode;
        }

        public Builder medFritekst(String fritekst) {
            this.fritekst = fritekst;
            return this;
        }

        public Builder medBegrunnelseKode(String begrunnelseKode) {
            this.begrunnelseKode = begrunnelseKode;
            return this;
        }

        public Henleggelsesbrevbestilling build() {
            return new Henleggelsesbrevbestilling(this);
        }
    }
}
