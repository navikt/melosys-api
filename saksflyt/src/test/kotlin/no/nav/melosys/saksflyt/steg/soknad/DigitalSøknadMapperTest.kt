package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DigitalSøknadMapperTest {

    @Nested
    inner class PeriodeMapping {

        @Test
        fun `mapper periode korrekt`() {
            val fom = LocalDate.of(2025, 1, 1)
            val tom = LocalDate.of(2025, 12, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(fom, tom)
                    )
                )
            }

            val (periode, _) = DigitalSøknadMapper.hentPeriodeOgLand(dto)

            periode.fom shouldBe fom
            periode.tom shouldBe tom
        }

        @Test
        fun `null periode gir tom Periode`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            }

            val (periode, _) = DigitalSøknadMapper.hentPeriodeOgLand(dto)

            periode.fom.shouldBeNull()
            periode.tom.shouldBeNull()
        }
    }

    @Nested
    inner class LandMapping {

        @Test
        fun `mapper land korrekt`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.SE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    )
                )
            }

            val (_, land) = DigitalSøknadMapper.hentPeriodeOgLand(dto)

            land.landkoder shouldHaveSize 1
            land.landkoder.first() shouldBe "SE"
        }

        @Test
        fun `null land gir tom Soeknadsland`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            }

            val (_, land) = DigitalSøknadMapper.hentPeriodeOgLand(dto)

            land.landkoder.shouldBeEmpty()
        }
    }

    @Nested
    inner class TilSoeknad {

        @Test
        fun `tilSoeknad mapper periode og land til Soeknad`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.FI,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 9, 30))
                    )
                )
            }

            val soeknad = DigitalSøknadMapper.tilSoeknad(dto)

            soeknad.periode.fom shouldBe LocalDate.of(2025, 3, 1)
            soeknad.periode.tom shouldBe LocalDate.of(2025, 9, 30)
            soeknad.soeknadsland.landkoder shouldBe listOf("FI")
        }
    }
}
