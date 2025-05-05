package no.nav.melosys.domain.brev;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class VedtakOpphoertMedlemskapBrevbestilling extends DokgenBrevbestilling {
    private String opphørtBegrunnelseFritekst;
    private LocalDate opphørtDato;
    private Behandlingstema behandlingstema;
    private List<String> land;

    public VedtakOpphoertMedlemskapBrevbestilling() {
        super();
        //Tom constructor på grunn av deserialsering i prosessinstans
    }

    private VedtakOpphoertMedlemskapBrevbestilling(Builder builder) {
        super(builder);
        this.opphørtBegrunnelseFritekst = builder.opphørtBegrunnelseFritekst;
        this.opphørtDato = builder.opphørtDato;
        this.behandlingstema = builder.behandlingstema;
        this.land = builder.land;
    }

    public String getOpphørtBegrunnelseFritekst() {
        return opphørtBegrunnelseFritekst;
    }

    public LocalDate getOpphørtDato() {
        return opphørtDato;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public List<String> getLand() {
        return land;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends DokgenBrevbestilling.Builder<Builder> {
        private String opphørtBegrunnelseFritekst;
        private LocalDate opphørtDato;
        private Behandlingstema behandlingstema;
        private List<String> land;

        public Builder() {
        }

        public Builder(VedtakOpphoertMedlemskapBrevbestilling vedtakOpphoertMedlemskapBrevbestilling) {
            super(vedtakOpphoertMedlemskapBrevbestilling);
            this.opphørtBegrunnelseFritekst = vedtakOpphoertMedlemskapBrevbestilling.opphørtBegrunnelseFritekst;
            this.opphørtDato = vedtakOpphoertMedlemskapBrevbestilling.opphørtDato;
            this.behandlingstema = vedtakOpphoertMedlemskapBrevbestilling.behandlingstema;
            this.land = vedtakOpphoertMedlemskapBrevbestilling.land;
        }

        public Builder medOpphørtBegrunnelseFritekst(String opphørtBegrunnelseFritekst) {
            this.opphørtBegrunnelseFritekst = opphørtBegrunnelseFritekst;
            return this;
        }

        public Builder medOpphørtDato(LocalDate opphørtDato) {
            this.opphørtDato = opphørtDato;
            return this;
        }

        public Builder medBehandlingstema(Behandlingstema behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public Builder medLand(List<String> land) {
            this.land = land;
            return this;
        }

        public VedtakOpphoertMedlemskapBrevbestilling build() {
            return new VedtakOpphoertMedlemskapBrevbestilling(this);
        }
    }
}
