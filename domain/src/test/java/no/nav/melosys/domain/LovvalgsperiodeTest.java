package no.nav.melosys.domain;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LovvalgsperiodeTest {

    @Test
    void konverterAnmodningTilLovvalgsperiode_innvilgelse_girInnvilgetOgLovvalgsland() {
        Anmodningsperiode innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.INNVILGELSE);
        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG);
        assertThat(lovvalgsperiode.getFom()).isEqualTo(innvilgetPeriode.getFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(innvilgetPeriode.getTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
        assertThat(lovvalgsperiode.getLovvalgsland()).isEqualTo(Land_iso2.NO);
    }

    @Test
    void konverterAnmodningTilLovvalgsperiode_delvisInnvilgelse_girInnvilgetOgLovvalgsland() {
        Anmodningsperiode innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE);
        AnmodningsperiodeSvar svar = innvilgetPeriode.getAnmodningsperiodeSvar();
        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG);
        assertThat(lovvalgsperiode.getFom()).isEqualTo(svar.getInnvilgetFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(svar.getInnvilgetTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.INNVILGET);
        assertThat(lovvalgsperiode.getLovvalgsland()).isEqualTo(Land_iso2.NO);
    }

    @Test
    void konverterAnmodningTilLovvalgsperiode_avslag_girAvslagOgTomtLovvalgsland() {
        Anmodningsperiode innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.AVSLAG);
        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.getAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG);
        assertThat(lovvalgsperiode.getFom()).isEqualTo(innvilgetPeriode.getFom());
        assertThat(lovvalgsperiode.getTom()).isEqualTo(innvilgetPeriode.getTom());
        assertThat(lovvalgsperiode.getInnvilgelsesresultat()).isEqualTo(InnvilgelsesResultat.AVSLAATT);
        assertThat(lovvalgsperiode.getLovvalgsland()).isNull();
    }

    private Anmodningsperiode lagAnmodningsperiode(Anmodningsperiodesvartyper svarType) {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            Land_iso2.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, null,
            Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);

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
