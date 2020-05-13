package no.nav.melosys.domain.eessi.sed;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.eessi.melding.SvarAnmodningUnntak;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.exception.TekniskException;

public class SvarAnmodningUnntakDto {

    private SvarAnmodningUnntak.Beslutning beslutning;
    private String begrunnelse;
    private Periode delvisInnvilgetPeriode;

    public SvarAnmodningUnntakDto(SvarAnmodningUnntak.Beslutning beslutning, String begrunnelse, Periode delvisInnvilgetPeriode) {
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

    private static SvarAnmodningUnntak.Beslutning hentBeslutningForSvartype(Anmodningsperiodesvartyper anmodningsperiodeSvarType) throws TekniskException {
        switch (anmodningsperiodeSvarType) {
            case INNVILGELSE:
                return SvarAnmodningUnntak.Beslutning.INNVILGELSE;
            case DELVIS_INNVILGELSE:
                return SvarAnmodningUnntak.Beslutning.DELVIS_INNVILGELSE;
            case AVSLAG:
                return SvarAnmodningUnntak.Beslutning.AVSLAG;
            default:
                throw new TekniskException("Ukjent AnmodningsperiodeSvarType " + anmodningsperiodeSvarType + " kan ikke mappes til Beslutning");
        }
    }

    public SvarAnmodningUnntak.Beslutning getBeslutning() {
        return beslutning;
    }

    public void setBeslutning(SvarAnmodningUnntak.Beslutning beslutning) {
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
