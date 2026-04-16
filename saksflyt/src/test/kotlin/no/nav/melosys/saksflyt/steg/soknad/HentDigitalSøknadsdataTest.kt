package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class HentDigitalSøknadsdataTest {

    @MockK
    lateinit var melosysSkjemaApiClient: MelosysSkjemaApiClient

    private lateinit var hentDigitalSøknadsdata: HentDigitalSøknadsdata
    private lateinit var prosessinstans: Prosessinstans

    private val skjemaId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        hentDigitalSøknadsdata = HentDigitalSøknadsdata(melosysSkjemaApiClient)

        prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNAD_SKJEMA_ID, SkjemaMottattMelding(skjemaId))
        }
    }

    @Test
    fun `inngangsSteg returnerer HENT_SØKNADSDATA`() {
        hentDigitalSøknadsdata.inngangsSteg() shouldBe ProsessSteg.HENT_DIGITAL_SØKNADSDATA
    }

    @Test
    fun `utfør henter søknadsdata og lagrer på prosessinstans`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            referanseId = "MEL-TEST123"
        }

        every { melosysSkjemaApiClient.hentUtsendtArbeidstakerSkjema(skjemaId) } returns søknadsdata

        hentDigitalSøknadsdata.utfør(prosessinstans)

        verify { melosysSkjemaApiClient.hentUtsendtArbeidstakerSkjema(skjemaId) }

        val lagretData = prosessinstans.hentData<UtsendtArbeidstakerSkjemaM2MDto>(ProsessDataKey.DIGITAL_SØKNADSDATA)
        lagretData shouldBe søknadsdata
    }
}
