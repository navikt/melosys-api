package no.nav.melosys.domain.eessi.melding;

import java.util.Objects;

public class SvarAnmodningUnntak {
    public enum Beslutning {
        INNVILGELSE, DELVIS_INNVILGELSE, AVSLAG
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SvarAnmodningUnntak)) return false;
        SvarAnmodningUnntak that = (SvarAnmodningUnntak) o;
        return getBeslutning() == that.getBeslutning() &&
            Objects.equals(getBegrunnelse(), that.getBegrunnelse()) &&
            Objects.equals(getDelvisInnvilgetPeriode(), that.getDelvisInnvilgetPeriode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBeslutning(), getBegrunnelse(), getDelvisInnvilgetPeriode());
    }
}