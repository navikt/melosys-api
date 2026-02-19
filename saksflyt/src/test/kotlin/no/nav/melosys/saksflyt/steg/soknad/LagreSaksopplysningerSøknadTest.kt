package no.nav.melosys.saksflyt.steg.soknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.utsendtarbeidstaker.UtsendtArbeidstakerSøknad
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class LagreSaksopplysningerSøknadTest {

    @MockK
    lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @MockK
    lateinit var objectMapper: ObjectMapper

    private lateinit var lagreSaksopplysningerSøknad: LagreSaksopplysningerSøknad
    private lateinit var prosessinstans: Prosessinstans

    private val behandlingId = 42L
    private val referanseId = "MEL-TEST123"

    private val skjema = UtsendtArbeidstakerSkjemaDto(
        id = UUID.randomUUID(),
        status = SkjemaStatus.SENDT,
        fnr = "12345678901",
        orgnr = "123456789",
        metadata = DegSelvMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "Test AS",
            juridiskEnhetOrgnr = "987654321"
        ),
        data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
    )

    private val søknadsdata = UtsendtArbeidstakerSkjemaM2MDto(
        skjema = skjema,
        kobletSkjema = null,
        tidligereInnsendteSkjema = emptyList(),
        referanseId = referanseId,
        innsendtTidspunkt = LocalDateTime.now(),
        innsenderFnr = "12345678901"
    )

    @BeforeEach
    fun setup() {
        lagreSaksopplysningerSøknad = LagreSaksopplysningerSøknad(mottatteOpplysningerService, objectMapper)

        prosessinstans = Prosessinstans.forTest {
            behandling { id = behandlingId }
            medData(ProsessDataKey.SØKNADSDATA, søknadsdata)
        }
    }

    @Test
    fun `inngangsSteg returnerer LAGRE_SAKSOPPLYSNINGER_SØKNAD`() {
        lagreSaksopplysningerSøknad.inngangsSteg() shouldBe ProsessSteg.LAGRE_SAKSOPPLYSNINGER_SØKNAD
    }

    @Test
    fun `utfør mapper søknadsdata og lagrer saksopplysninger`() {
        val originalDataJson = """{"referanseId":"MEL-TEST123"}"""
        val soeknadSlot = slot<UtsendtArbeidstakerSøknad>()

        every { objectMapper.writeValueAsString(søknadsdata) } returns originalDataJson
        every {
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
                eq(behandlingId),
                eq(originalDataJson),
                capture(soeknadSlot),
                eq(referanseId)
            )
        } returns mockk<MottatteOpplysninger>()

        lagreSaksopplysningerSøknad.utfør(prosessinstans)

        // Verify the Soeknad was mapped (non-null and correct type)
        soeknadSlot.captured.shouldNotBeNull()

        verify(exactly = 1) {
            objectMapper.writeValueAsString(søknadsdata)
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
                behandlingId, originalDataJson, any(), referanseId
            )
        }
    }

    @Test
    fun `utfør feiler når behandling mangler`() {
        val prosessinstansUtenBehandling = Prosessinstans.forTest {
            medData(ProsessDataKey.SØKNADSDATA, søknadsdata)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            lagreSaksopplysningerSøknad.utfør(prosessinstansUtenBehandling)
        }

        exception.message shouldContain "Behandling må være opprettet"
    }
}
