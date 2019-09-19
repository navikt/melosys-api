package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LovvalgsperiodeTest {

    @Test
    public void konverterAnmodningTilLovvalgsperiode_innvilgelse_girInnvilgetOgLovvalgsland() throws FunksjonellException {
        Anmodningsperiode innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.INNVILGELSE);
        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG);
        assertThat(lovvalgsperiode.getFom()).isEqualTo(innvilgetPeriode.getFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(innvilgetPeriode.getTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
        assertThat(lovvalgsperiode.getLovvalgsland()).isEqualTo(Landkoder.NO);
    }

    @Test
    public void konverterAnmodningTilLovvalgsperiode_delvisInnvilgelse_girInnvilgetOgLovvalgsland() throws FunksjonellException {
        Anmodningsperiode innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        AnmodningsperiodeSvar svar = innvilgetPeriode.getAnmodningsperiodeSvar();
        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG);
        assertThat(lovvalgsperiode.getFom()).isEqualTo(svar.getInnvilgetFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(svar.getInnvilgetTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
        assertThat(lovvalgsperiode.getLovvalgsland()).isEqualTo(Landkoder.NO);
    }

    @Test
    public void konverterAnmodningTilLovvalgsperiode_avslag_girAvslagOgTomtLovvalgsland() throws FunksjonellException {
        Anmodningsperiode innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.AVSLAG);
        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG);
        assertThat(lovvalgsperiode.getFom()).isEqualTo(innvilgetPeriode.getFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(innvilgetPeriode.getTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.AVSLAATT);
        assertThat(lovvalgsperiode.getLovvalgsland()).isNull();
    }

    private Anmodningsperiode lagAnmodningsperiode(Anmodningsperiodesvartyper svarType) {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(
            LocalDate.of(2020, 1,1),
            LocalDate.of(2020, 12, 31),
            Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, null,
            Landkoder.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(svarType);
        svar.setAnmodningsperiode(anmodningsperiode);
        svar.setInnvilgetFom(LocalDate.of(2020, 7, 1));
        svar.setInnvilgetFom(LocalDate.of(2020, 8, 1));
        anmodningsperiode.setAnmodningsperiodeSvar(svar);
        svar.setInnvilgetTom(LocalDate.now());
        return anmodningsperiode;
    }
}
