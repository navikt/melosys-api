package no.nav.melosys.domain.util

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Bostedsland
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MottatteOpplysningerUtilsTest {

    @Test
    fun hentSoeknadsland() {
        val soeknad = Soeknad()
        soeknad.soeknadsland.landkoder = listOf(Landkoder.BE.kode, Landkoder.BG.kode)
        soeknad.soeknadsland.setFlereLandUkjentHvilke(true)


        val soeknadsland = MottatteOpplysningerUtils.hentSøknadsland(soeknad)


        soeknadsland.run {
            landkoder shouldContain Landkoder.BE.kode
            landkoder shouldContain Landkoder.BG.kode
            isFlereLandUkjentHvilke() shouldBe true
        }
    }

    @Test
    fun hentOppgittAdresse_medGatenavnOgLand_ErIkkeNull() {
        val søknad = Soeknad()
        val oppgittAdresse = StrukturertAdresse().apply {
            gatenavn = "HjemGata"
            landkode = "NO"
        }
        søknad.bosted.oppgittAdresse = oppgittAdresse


        MottatteOpplysningerUtils.hentBostedsadresse(søknad).shouldNotBeNull()
    }

    @Test
    fun hentOppgittAdresse_somErTom_ErNull() {
        val søknad = Soeknad()
        søknad.bosted.oppgittAdresse = StrukturertAdresse()


        MottatteOpplysningerUtils.hentBostedsadresse(søknad).shouldBeNull()
    }

    @Test
    fun hentPeriode_opphold() {
        val soeknad = Soeknad()
        leggTilFysiskArbeidssted(soeknad)

        val periode2 = Periode(LocalDate.now(), null)
        soeknad.periode = periode2


        val res = MottatteOpplysningerUtils.hentPeriode(soeknad)


        res shouldBe periode2
    }

    @Test
    fun hentOppgittBostedsland_landkodeSverige_girLandkode() {
        val soeknad = Soeknad()
        soeknad.bosted.oppgittAdresse.landkode = "SE"


        val landkoder = MottatteOpplysningerUtils.hentOppgittBostedsland(soeknad)


        landkoder shouldBePresent {
            it shouldBe Bostedsland(Landkoder.SE)
        }
    }

    @Test
    fun hentOppgittBostedsland_eksistererIkke_girEmpty() {
        val soeknad = Soeknad()


        val landkoder = MottatteOpplysningerUtils.hentOppgittBostedsland(soeknad)


        landkoder.shouldBeEmpty()
    }

    private fun leggTilFysiskArbeidssted(soeknad: Soeknad) {
        val fysiskArbeidssted = FysiskArbeidssted().apply {
            adresse.landkode = Landkoder.BE.kode
        }
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = listOf(fysiskArbeidssted)
    }
}
