package no.nav.melosys.service.dokument

import io.kotest.matchers.optional.shouldBeEmpty
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Bosted
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BostedGrunnlagKtTest {
    private val soeknad = Soeknad()

    @MockK
    private lateinit var kodeverkService: KodeverkService

    private lateinit var bostedGrunnlag: BostedGrunnlag

    @Test
    fun `hentBostedsadresse, forvent strukturert adresse`() {
        bostedGrunnlag = BostedGrunnlag(soeknad, null, null, kodeverkService)
        soeknad.bosted = Bosted().apply {
            oppgittAdresse = StrukturertAdresse().apply {
                landkode = "SE"
                gatenavn = "gate"
            }
        }


        val strukturertAdresse = bostedGrunnlag.hentBostedsadresse()


        strukturertAdresse.run {
            gatenavn shouldBe "gate"
            landkode shouldBe "SE"
        }
    }

    @Test
    fun `hentBostedsadresse, ingen adresse, forvent exception`() {
        bostedGrunnlag = BostedGrunnlag(soeknad, null, null, kodeverkService)

        assertThrows<FunksjonellException> {
            bostedGrunnlag.hentBostedsadresse()
        }.message shouldContain "finnes ikke eller mangler landkode"
    }

    @Test
    fun `finnBostedsadresse, oppgitt adresse overstyrer register, når oppgitt adresse i søknad`() {
        bostedGrunnlag = BostedGrunnlag(
            soeknad,
            PersonopplysningerObjectFactory.lagPersonopplysninger().bostedsadresse,
            null,
            kodeverkService
        )
        val oppgittBosted = StrukturertAdresse().apply {
            gatenavn = "HerBorJegGata"
            husnummerEtasjeLeilighet = "123"
            postnummer = "0166"
            poststed = "Oslo"
            region = "Østlandet"
            landkode = "NO"
        }
        soeknad.bosted = Bosted().apply {
            oppgittAdresse = oppgittBosted
        }


        val strukturertAdresse = bostedGrunnlag.finnBostedsadresse()


        strukturertAdresse.shouldBePresent()
        strukturertAdresse.get().run {
            gatenavn shouldBe "HerBorJegGata"
            husnummerEtasjeLeilighet shouldBe "123"
            postnummer shouldBe "0166"
            poststed shouldBe "Oslo"
            region shouldBe "Østlandet"
            landkode shouldBe Landkoder.NO.kode
        }
    }

    @Test
    fun `finnBostedsadresse, har bostedsadresse i register, forvent bostedsadresse`() {
        bostedGrunnlag = BostedGrunnlag(
            soeknad,
            PersonopplysningerObjectFactory.lagPersonopplysninger().bostedsadresse,
            null,
            kodeverkService
        )


        val strukturertAdresse = bostedGrunnlag.finnBostedsadresse()


        strukturertAdresse.shouldBePresent()
        strukturertAdresse.get().run {
            gatenavn shouldBe "gatenavnFraBostedsadresse"
            landkode shouldBe "NO"
        }
    }

    @Test
    fun `finnBostedsadresse, ingen adresse, forvent tom optional`() {
        bostedGrunnlag = BostedGrunnlag(soeknad, null, null, kodeverkService)


        val strukturertAdresse = bostedGrunnlag.finnBostedsadresse()


        strukturertAdresse.shouldBeEmpty()
    }

    @Test
    fun `finnBostedsadresse, bostedsadresse fra person opplysninger, forvent bostedsadresse`() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        bostedGrunnlag = BostedGrunnlag(null, personopplysninger.bostedsadresse, null, kodeverkService)


        val strukturertAdresse = bostedGrunnlag.finnBostedsadresse()


        strukturertAdresse.shouldBePresent()
        strukturertAdresse.get().run {
            gatenavn shouldBe "gatenavnFraBostedsadresse"
            landkode shouldBe "NO"
            postnummer shouldBe "1234"
            poststed shouldBe "Oslo"
            region shouldBe "Norge"
        }
    }

    @Test
    fun `finnKontaktadresse, ingen adresse, forvent tom optional`() {
        bostedGrunnlag = BostedGrunnlag(soeknad, null, null, kodeverkService)


        val kontaktadresse = bostedGrunnlag.finnKontaktadresse()


        kontaktadresse.shouldBeEmpty()
    }

    @Test
    fun `finnKontaktadresse, kontaktadresse fra person opplysninger, forvent kontakt adresse`() {
        val personopplysninger = PersonopplysningerObjectFactory.lagPersonopplysninger()
        bostedGrunnlag = BostedGrunnlag(null, null, personopplysninger.finnKontaktadresse().orElse(null), kodeverkService)


        val kontaktadresse = bostedGrunnlag.finnKontaktadresse()


        kontaktadresse.shouldBePresent()
        kontaktadresse.get().run {
            gatenavn shouldBe "gatenavnKontaktadresseFreg"
            landkode shouldBe "NO"
            postnummer shouldBe "0123"
            poststed shouldBe "Poststed"
        }
    }
}
