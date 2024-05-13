package no.nav.melosys.domain.brev;

import java.time.LocalDate;

public class VedtakOpphoertMedlemskapBrevbestilling extends DokgenBrevbestilling {
    private String opphørtBegrunnelseFritekst;
    private LocalDate opphørtDato;

    public VedtakOpphoertMedlemskapBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private VedtakOpphoertMedlemskapBrevbestilling(Builder builder) {
        super(builder);
        this.opphørtBegrunnelseFritekst = builder.opphørtBegrunnelseFritekst;
        this.opphørtDato = builder.opphørtDato;
    }

    public String getOpphørtBegrunnelseFritekst() {
        return opphørtBegrunnelseFritekst;
    }

    public LocalDate getOpphørtDato() {
        return opphørtDato;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String opphørtBegrunnelseFritekst;
        private LocalDate opphørtDato;

        public Builder() {
        }

        public Builder(VedtakOpphoertMedlemskapBrevbestilling vedtakOpphoertMedlemskapBrevbestilling) {
            super(vedtakOpphoertMedlemskapBrevbestilling);
            this.opphørtBegrunnelseFritekst = vedtakOpphoertMedlemskapBrevbestilling.opphørtBegrunnelseFritekst;
            this.opphørtDato = vedtakOpphoertMedlemskapBrevbestilling.opphørtDato;
        }

        public Builder medOpphørtBegrunnelseFritekst(String opphørtBegrunnelseFritekst) {
            this.opphørtBegrunnelseFritekst = opphørtBegrunnelseFritekst;
            return this;
        }

        public Builder medOpphørtDato(LocalDate opphørtDato) {
            this.opphørtDato = opphørtDato;
            return this;
        }

        public VedtakOpphoertMedlemskapBrevbestilling build() {
            return new VedtakOpphoertMedlemskapBrevbestilling(this);
        }
    }
}
