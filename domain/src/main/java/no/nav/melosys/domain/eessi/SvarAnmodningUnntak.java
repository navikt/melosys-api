package no.nav.melosys.domain.eessi;

import java.util.Objects;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;

public class SvarAnmodningUnntak {
    public enum Beslutning {
        INNVILGELSE, DELVIS_INNVILGELSE, AVSLAG
    }

    private Beslutning beslutning;
    private String begrunnelse;
    private Periode delvisInnvilgetPeriode;

    public SvarAnmodningUnntak() {
    }

    private SvarAnmodningUnntak(Beslutning beslutning, String begrunnelse, Periode delvisInnvilgetPeriode) {
        this.beslutning = beslutning;
        this.begrunnelse = begrunnelse;
        this.delvisInnvilgetPeriode = delvisInnvilgetPeriode;
    }

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

    public static SvarAnmodningUnntak av(AnmodningsperiodeSvar anmodningsperiodeSvar) {
        return new SvarAnmodningUnntak(
            hentBeslutningForSvartype(anmodningsperiodeSvar.getAnmodningsperiodeSvarType()),
            anmodningsperiodeSvar.getBegrunnelseFritekst(),
            new Periode(
                anmodningsperiodeSvar.getInnvilgetFom(),
                anmodningsperiodeSvar.getInnvilgetTom()
            )
        );
    }

    private static Beslutning hentBeslutningForSvartype(Anmodningsperiodesvartyper anmodningsperiodeSvarType) {
        switch (anmodningsperiodeSvarType) {
            case INNVILGELSE:
                return Beslutning.INNVILGELSE;
            case DELVIS_INNVILGELSE:
                return Beslutning.DELVIS_INNVILGELSE;
            case AVSLAG:
                return Beslutning.AVSLAG;
            default:
                throw new IllegalArgumentException("Ukjent AnmodningsperiodeSvarType " + anmodningsperiodeSvarType + " kan ikke mappes til Beslutning");
        }
    }
}