package no.nav.melosys.domain

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class LovvalgsperiodeTest {

    @Test
    fun konverterAnmodningTilLovvalgsperiode_innvilgelse_girInnvilgetOgLovvalgsland() {
        val innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.INNVILGELSE)
        val lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.anmodningsperiodeSvar, Medlemskapstyper.PLIKTIG)


        lovvalgsperiode.run {
            fom shouldBe innvilgetPeriode.fom
            tom shouldBe innvilgetPeriode.tom
            innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
            lovvalgsland shouldBe Land_iso2.NO
        }
    }

    @Test
    fun konverterAnmodningTilLovvalgsperiode_delvisInnvilgelse_girInnvilgetOgLovvalgsland() {
        val innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.DELVIS_INNVILGELSE)
        val svar = innvilgetPeriode.anmodningsperiodeSvar
        val lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.anmodningsperiodeSvar, Medlemskapstyper.PLIKTIG)


        lovvalgsperiode.run {
            fom shouldBe svar.innvilgetFom
            tom shouldBe svar.innvilgetTom
            innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
            lovvalgsland shouldBe Land_iso2.NO
        }
    }

    @Test
    fun konverterAnmodningTilLovvalgsperiode_avslag_girAvslagOgTomtLovvalgsland() {
        val innvilgetPeriode = lagAnmodningsperiode(Anmodningsperiodesvartyper.AVSLAG)
        val lovvalgsperiode = Lovvalgsperiode.av(innvilgetPeriode.anmodningsperiodeSvar, Medlemskapstyper.PLIKTIG)


        lovvalgsperiode.run {
            fom shouldBe innvilgetPeriode.fom
            tom shouldBe innvilgetPeriode.tom
            innvilgelsesresultat shouldBe InnvilgelsesResultat.AVSLAATT
            lovvalgsland.shouldBeNull()
        }
    }

    private fun lagAnmodningsperiode(svarType: Anmodningsperiodesvartyper): Anmodningsperiode {
        val anmodningsperiode = Anmodningsperiode(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            Land_iso2.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, null,
            Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO
        )

        val svar = AnmodningsperiodeSvar().apply {
            anmodningsperiodeSvarType = svarType
            this.anmodningsperiode = anmodningsperiode
            innvilgetFom = LocalDate.of(2020, 7, 1)
            innvilgetTom = LocalDate.now()
        }

        anmodningsperiode.anmodningsperiodeSvar = svar
        return anmodningsperiode
    }
}
