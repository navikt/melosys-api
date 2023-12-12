package no.nav.melosys.domain.brev;

public class VedtakOpphoertMedlemskapBrevbestilling extends DokgenBrevbestilling {
    private String opphoertBegrunnelseFritekst;

    public VedtakOpphoertMedlemskapBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private VedtakOpphoertMedlemskapBrevbestilling(Builder builder) {
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

        public Builder(VedtakOpphoertMedlemskapBrevbestilling vedtakOpphoertMedlemskapBrevbestilling) {
            super(vedtakOpphoertMedlemskapBrevbestilling);
            this.opphoertBegrunnelseFritekst = vedtakOpphoertMedlemskapBrevbestilling.opphoertBegrunnelseFritekst;
        }

        public Builder medOpphoertBegrunnelseFritekst(String opphoertBegrunnelseFritekst) {
            this.opphoertBegrunnelseFritekst = opphoertBegrunnelseFritekst;
            return this;
        }

        public VedtakOpphoertMedlemskapBrevbestilling build() {
            return new VedtakOpphoertMedlemskapBrevbestilling(this);
        }
    }
}
