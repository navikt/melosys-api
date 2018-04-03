package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.Objects;

import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeSortering;

public class FinnOppgaveListeRequestMal {
    private FinnOppgaveListeSokMal sok;
    private FinnOppgaveListeFilterMal filter;
    private FinnOppgaveListeSortering sortering;
    private String ikkeTidligereFordeltTil;

    public FinnOppgaveListeRequestMal(FinnOppgaveListeSokMal sok, FinnOppgaveListeFilterMal filter,
                                      FinnOppgaveListeSortering sortering, String ikkeTidligereFordeltTil) {
        this.sok = sok;
        this.filter = filter;
        this.sortering = sortering;
        this.ikkeTidligereFordeltTil = ikkeTidligereFordeltTil;
    }

    public static FinnOppgaveListeRequestMal.Builder builder() {
        return new FinnOppgaveListeRequestMal.Builder();
    }

    public FinnOppgaveListeSokMal getSok() {
        return sok;
    }

    public FinnOppgaveListeFilterMal getFilter() {
        return filter;
    }

    public FinnOppgaveListeSortering getSortering() {
        return sortering;
    }

    public String getIkkeTidligereFordeltTil() {
        return ikkeTidligereFordeltTil;
    }

    public static class Builder {
        private FinnOppgaveListeSokMal sok;
        private FinnOppgaveListeFilterMal filter;
        private FinnOppgaveListeSortering sortering;
        private String ikkeTidligereFordeltTil;

        public Builder medSok(FinnOppgaveListeSokMal sok) {
            this.sok = sok;
            return this;
        }

        public Builder medFilter(FinnOppgaveListeFilterMal filter) {
            this.filter = filter;
            return this;
        }

        public Builder medSortering(FinnOppgaveListeSortering sortering) {
            this.sortering = sortering;
            return this;
        }

        public Builder medIkkeTidligereFordeltTil(String ikkeTidligereFordeltTil) {
            this.ikkeTidligereFordeltTil = ikkeTidligereFordeltTil;
            return this;
        }

        public FinnOppgaveListeRequestMal build() {
            Objects.requireNonNull(sok, "FinnOppgaveListeSokMal");
            return new FinnOppgaveListeRequestMal(sok, filter, sortering, ikkeTidligereFordeltTil);
        }
    }
}
