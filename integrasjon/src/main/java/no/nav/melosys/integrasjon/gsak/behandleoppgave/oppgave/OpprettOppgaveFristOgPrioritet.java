package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk.PrioritetType;

public class OpprettOppgaveFristOgPrioritet {
    private LocalDate aktivFra;
    private LocalDate aktivTil;
    private PrioritetType prioritetType;

    private OpprettOppgaveFristOgPrioritet(LocalDate aktivFra, LocalDate aktivTil, PrioritetType prioritetType) {
        this.aktivFra = aktivFra;
        this.aktivTil = aktivTil;
        this.prioritetType = prioritetType;
    }

    public LocalDate getAktivFra() {
        return aktivFra;
    }

    public LocalDate getAktivTil() {
        return aktivTil;
    }

    public PrioritetType getPrioritetType() {
        return prioritetType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate aktivFra;
        private LocalDate aktivTil;
        private PrioritetType prioritetType;

        public Builder aktivFra(LocalDate aktivFra) {
            this.aktivFra = aktivFra;
            return this;
        }

        public Builder aktivTil(LocalDate aktivTil) {
            this.aktivTil = aktivTil;
            return this;
        }

        public Builder medPrioritetType(PrioritetType prioritetType) {
            this.prioritetType = prioritetType;
            return this;
        }

        public OpprettOppgaveFristOgPrioritet build() {
            return new OpprettOppgaveFristOgPrioritet(aktivFra, aktivTil, prioritetType);
        }
    }
}
