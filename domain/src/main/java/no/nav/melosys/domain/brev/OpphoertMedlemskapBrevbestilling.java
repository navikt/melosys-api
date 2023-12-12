package no.nav.melosys.domain.brev;

public class OpphoertMedlemskapBrevbestilling extends DokgenBrevbestilling {
    private String opphoertBegrunnelseFritekst;

    public OpphoertMedlemskapBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private OpphoertMedlemskapBrevbestilling(Builder builder) {
        super(builder);
        this.opphoertBegrunnelseFritekst = builder.opphoertBegrunnelseFritekst;
    }

    public String getOpphoertBegrunnelseFritekst() {
        return opphoertBegrunnelseFritekst;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String opphoertBegrunnelseFritekst;

        public Builder() {
        }

        public Builder(OpphoertMedlemskapBrevbestilling opphoertMedlemskapBrevbestilling) {
            super(opphoertMedlemskapBrevbestilling);
            this.opphoertBegrunnelseFritekst = opphoertMedlemskapBrevbestilling.opphoertBegrunnelseFritekst;
        }

        public Builder medOpphoertBegrunnelseFritekst(String opphoertBegrunnelseFritekst) {
            this.opphoertBegrunnelseFritekst = opphoertBegrunnelseFritekst;
            return this;
        }

        public OpphoertMedlemskapBrevbestilling build() {
            return new OpphoertMedlemskapBrevbestilling(this);
        }
    }
}
