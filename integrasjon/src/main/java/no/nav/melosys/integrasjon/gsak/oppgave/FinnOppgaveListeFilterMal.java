package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.List;

public class FinnOppgaveListeFilterMal {
    private String underkategoriKode;
    private Boolean aktiv;
    private Boolean ufordelte;
    private List<String> oppgavetypeKodeListe;

    public FinnOppgaveListeFilterMal(String underkategoriKode, boolean aktiv, boolean ufordelte, List<String> oppgavetypeKodeListe) {
        this.underkategoriKode = underkategoriKode;
        this.oppgavetypeKodeListe = oppgavetypeKodeListe;
        this.aktiv = aktiv;
        this.ufordelte = ufordelte;
        this.oppgavetypeKodeListe = oppgavetypeKodeListe;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Boolean getAktiv() {
        return aktiv;
    }

    public Boolean getUfordelte() {
        return ufordelte;
    }

    public String getUnderkategoriKode() {
        return underkategoriKode;
    }

    public List<String> getOppgavetypeKodeListe() {
        return oppgavetypeKodeListe;
    }

    public static class Builder {
        private String underkategoriKode;
        private boolean aktiv;
        private boolean ufordelte;
        private List<String> oppgavetypeKodeListe;

        public Builder medUnderkategori(String underkategoriKode) {
            this.underkategoriKode = underkategoriKode;
            return this;
        }

        public Builder medAktiv(boolean aktiv) {
            this.aktiv = aktiv;
            return this;
        }

        public Builder medUfordelte(boolean ufordelte) {
            this.ufordelte = ufordelte;
            return this;
        }

        public Builder medOppgavetypeKodeListe(List<String> oppgavetypeKodeListe) {
            this.oppgavetypeKodeListe = oppgavetypeKodeListe;
            return this;
        }

        public FinnOppgaveListeFilterMal build() {
            return new FinnOppgaveListeFilterMal(underkategoriKode, aktiv, ufordelte, oppgavetypeKodeListe);
        }
    }
}
