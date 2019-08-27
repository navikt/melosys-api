package no.nav.melosys.integrasjon.eessi.dto;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.exception.TekniskException;

public class SvarAnmodningUnntakDto {

    public enum Beslutning {
        INNVILGELSE, DELVIS_INNVILGELSE, AVSLAG;
    }

    private Beslutning beslutning;
    private String begrunnelse;
    private Periode delvisInnvilgetPeriode;

    public SvarAnmodningUnntakDto(Beslutning beslutning, String begrunnelse, Periode delvisInnvilgetPeriode) {
        this.beslutning = beslutning;
        this.begrunnelse = begrunnelse;
        this.delvisInnvilgetPeriode = delvisInnvilgetPeriode;
    }

    public static SvarAnmodningUnntakDto av(AnmodningsperiodeSvar anmodningsperiodeSvar) throws TekniskException {
        return new SvarAnmodningUnntakDto(
            hentBeslutningForSvartype(anmodningsperiodeSvar.getAnmodningsperiodeSvarType()),
            anmodningsperiodeSvar.getBegrunnelseFritekst(),
            new Periode(
                anmodningsperiodeSvar.getInnvilgetFom(),
                anmodningsperiodeSvar.getInnvilgetTom()
            )
        );
    }

    private static Beslutning hentBeslutningForSvartype(Anmodningsperiodesvartyper anmodningsperiodeSvarType) throws TekniskException {
        switch (anmodningsperiodeSvarType) {
            case INNVILGELSE:
                return Beslutning.INNVILGELSE;
            case DELVIS_INNVILGELSE:
                return Beslutning.DELVIS_INNVILGELSE;
            case AVSLAG:
                return Beslutning.AVSLAG;
            default:
                throw new TekniskException("Ukjent AnmodningsperiodeSvarType " + anmodningsperiodeSvarType + " kan ikke mappes til Beslutning");
        }
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
}
