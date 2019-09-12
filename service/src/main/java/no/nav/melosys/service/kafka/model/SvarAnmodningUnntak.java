package no.nav.melosys.service.kafka.model;

public class SvarAnmodningUnntak {

    public enum Beslutning {
        INNVILGELSE, DELVIS_INNVILGELSE, AVSLAG;
    }

    private Beslutning beslutning;
    private String begrunnelse;
    private Periode delvisInnvilgetPeriode;

    public Beslutning getBeslutning() {
        return beslutning;
    }

    public void setBeslutning(Beslutning beslutning) {
        this.beslutning = beslutning;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Periode getDelvisInnvilgetPeriode() {
        return delvisInnvilgetPeriode;
    }

    public void setDelvisInnvilgetPeriode(Periode delvisInnvilgetPeriode) {
        this.delvisInnvilgetPeriode = delvisInnvilgetPeriode;
    }
}