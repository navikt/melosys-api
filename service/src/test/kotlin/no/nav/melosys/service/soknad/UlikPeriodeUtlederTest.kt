package no.nav.melosys.service.soknad

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Instant
import java.time.LocalDate

internal class UlikPeriodeUtlederTest {

    private val repository = mockk<SkjemaSakMappingRepository>()
    private val jsonMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val utleder = UlikPeriodeUtleder(repository, jsonMapper)

    @Test
    fun `ulike perioder fra partene gir avvik`() {
        val dto = medKobletSkjema(
            egen = periode("2025-01-01", "2025-06-30"),
            motpart = periode("2025-03-01", "2025-12-31")
        )

        utleder.harUlikPeriode(mottatteOpplysningerMed(dto)).shouldBeTrue()
    }

    @Test
    fun `like perioder fra partene gir ikke avvik`() {
        val dto = medKobletSkjema(
            egen = periode("2025-01-01", "2025-06-30"),
            motpart = periode("2025-01-01", "2025-06-30")
        )

        utleder.harUlikPeriode(mottatteOpplysningerMed(dto)).shouldBeFalse()
    }

    @Test
    fun `innsending uten koblet skjema gir ikke avvik`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                utsendingsperiodeOgLand = periode("2025-01-01", "2025-06-30")
            )
        }

        utleder.harUlikPeriode(mottatteOpplysningerMed(dto)).shouldBeFalse()
    }

    @Test
    fun `manglende periode hos motparten gir ikke avvik`() {
        val dto = medKobletSkjema(egen = periode("2025-01-01", "2025-06-30"), motpart = null)

        utleder.harUlikPeriode(mottatteOpplysningerMed(dto)).shouldBeFalse()
    }

    @Test
    fun `nyeste skjemamapping er avgjoerende`() {
        val gammel = mapping(
            medKobletSkjema(periode("2025-01-01", "2025-06-30"), periode("2025-01-01", "2025-06-30")),
            Instant.parse("2025-01-01T00:00:00Z")
        )
        val ny = mapping(
            medKobletSkjema(periode("2025-01-01", "2025-06-30"), periode("2025-03-01", "2025-12-31")),
            Instant.parse("2025-02-01T00:00:00Z")
        )
        every { repository.findByMottatteOpplysninger_Id(any()) } returns listOf(gammel, ny)

        utleder.harUlikPeriode(mottatteOpplysninger()).shouldBeTrue()
    }

    @Test
    fun `ugyldig skjemadata gir ikke avvik`() {
        every { repository.findByMottatteOpplysninger_Id(any()) } returns
            listOf(mockk<SkjemaSakMapping>(relaxed = true).also {
                every { it.originalData } returns "{ ikke gyldig json"
                every { it.opprettetDato } returns Instant.now()
            })

        utleder.harUlikPeriode(mottatteOpplysninger()).shouldBeFalse()
    }

    @Test
    fun `mottatte opplysninger uten id gir ikke avvik`() {
        utleder.harUlikPeriode(null).shouldBeFalse()
    }

    private fun periode(fom: String, tom: String) = UtsendingsperiodeOgLandDto(
        utsendelseLand = LandKode.DE,
        utsendelsePeriode = PeriodeDto(LocalDate.parse(fom), LocalDate.parse(tom))
    )

    private fun medKobletSkjema(egen: UtsendingsperiodeOgLandDto?, motpart: UtsendingsperiodeOgLandDto?) =
        lagUtsendtArbeidstakerSkjemaM2MDto {
            data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(utsendingsperiodeOgLand = egen)
            medKobletArbeidsgiverSkjema {
                data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(utsendingsperiodeOgLand = motpart)
            }
        }

    private fun mapping(dto: UtsendtArbeidstakerSkjemaM2MDto, opprettet: Instant) =
        mockk<SkjemaSakMapping>(relaxed = true).also {
            every { it.originalData } returns jsonMapper.writeValueAsString(dto)
            every { it.opprettetDato } returns opprettet
        }

    private fun mottatteOpplysninger() = mockk<MottatteOpplysninger>().also {
        every { it.id } returns 1L
    }

    private fun mottatteOpplysningerMed(dto: UtsendtArbeidstakerSkjemaM2MDto): MottatteOpplysninger {
        every { repository.findByMottatteOpplysninger_Id(any()) } returns
            listOf(mapping(dto, Instant.now()))
        return mottatteOpplysninger()
    }
}
