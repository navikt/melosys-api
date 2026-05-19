package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverfirmaInfo
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import org.junit.jupiter.api.Test

internal class DigitalSøknadAktørerMapperTest {

    private val arbeidstakerFnr = "12345678901"
    private val arbeidsgiverOrgnr = "111111111"
    private val koblingsOrgnr = "222222222"
    private val juridiskEnhet = "999999999"
    private val fullmektigFnr = "10987654321"
    private val radgiverfirmaOrgnr = "333333333"

    @Test
    fun `DEG_SELV gir kun arbeidsgiver, ingen fullmektige`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = DegSelvMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                arbeidstakerNavn = "Test Arbeidstaker"
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            innsenderFnr = arbeidstakerFnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige.shouldBeEmpty()
        resultat.skjemadel shouldBe Skjemadel.ARBEIDSTAKERS_DEL
    }

    @Test
    fun `ARBEIDSGIVER (uten fullmakt) gir kun arbeidsgiver, ingen fullmektige`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = ArbeidsgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                arbeidstakerNavn = "Test Arbeidstaker"
            )
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige.shouldBeEmpty()
    }

    @Test
    fun `ARBEIDSGIVER med koblet skjema som har annet orgnr gir to arbeidsgivere`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = ArbeidsgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                arbeidstakerNavn = "Test Arbeidstaker"
            )
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            medKobletArbeidsgiverSkjema { orgnr = koblingsOrgnr }
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldContainExactlyInAnyOrder listOf(arbeidsgiverOrgnr, koblingsOrgnr)
        resultat.fullmektige.shouldBeEmpty()
    }

    @Test
    fun `ARBEIDSGIVER_MED_FULLMAKT gir én FULLMEKTIG-aktør med orgnr + personIdent + FULLMEKTIG_SØKNAD`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = ArbeidsgiverMedFullmaktMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                fullmektigFnr = fullmektigFnr,
                arbeidstakerNavn = "Test Arbeidstaker"
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            innsenderFnr = fullmektigFnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldBe listOf(
            FullmektigSpec(
                orgnr = arbeidsgiverOrgnr,
                personIdent = fullmektigFnr,
                kontaktpersonFnr = fullmektigFnr,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
            )
        )
    }

    @Test
    fun `RADGIVER (uten fullmakt) gir én FULLMEKTIG-aktør med orgnr + FULLMEKTIG_ARBEIDSGIVER`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = RadgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                arbeidstakerNavn = "Test Arbeidstaker",
                radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaOrgnr, navn = "Rådgiver AS")
            )
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            innsenderFnr = fullmektigFnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldBe listOf(
            FullmektigSpec(
                orgnr = radgiverfirmaOrgnr,
                personIdent = null,
                kontaktpersonFnr = fullmektigFnr,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            )
        )
    }

    @Test
    fun `RADGIVER_MED_FULLMAKT gir én FULLMEKTIG-aktør med orgnr + personIdent + begge fullmaktstyper`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = RadgiverMedFullmaktMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                fullmektigFnr = fullmektigFnr,
                arbeidstakerNavn = "Test Arbeidstaker",
                radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaOrgnr, navn = "Rådgiver AS")
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            innsenderFnr = fullmektigFnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldBe listOf(
            FullmektigSpec(
                orgnr = radgiverfirmaOrgnr,
                personIdent = fullmektigFnr,
                kontaktpersonFnr = fullmektigFnr,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD, Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
            )
        )
    }

    @Test
    fun `ANNEN_PERSON gir én FULLMEKTIG-aktør med kun personIdent + FULLMEKTIG_SØKNAD`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = AnnenPersonMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                fullmektigFnr = fullmektigFnr,
                arbeidstakerNavn = "Test Arbeidstaker"
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            innsenderFnr = fullmektigFnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldBe listOf(
            FullmektigSpec(
                orgnr = null,
                personIdent = fullmektigFnr,
                kontaktpersonFnr = null,
                fullmakter = setOf(Fullmaktstype.FULLMEKTIG_SØKNAD)
            )
        )
    }
}
