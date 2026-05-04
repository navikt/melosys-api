package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.sak.FullmektigDto
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
                juridiskEnhetOrgnr = juridiskEnhet
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            innsenderFnr = arbeidstakerFnr // arbeidstaker fyller selv
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige.shouldBeEmpty()
    }

    @Test
    fun `ARBEIDSGIVER gir kun arbeidsgiver, ingen fullmektige`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = ArbeidsgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet
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
                juridiskEnhetOrgnr = juridiskEnhet
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
    fun `ARBEIDSGIVER med koblet skjema med samme orgnr gir kun én arbeidsgiver`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = ArbeidsgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet
            )
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
            medKobletArbeidsgiverSkjema { orgnr = arbeidsgiverOrgnr }
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
    }

    @Test
    fun `ARBEIDSGIVER_MED_FULLMAKT gir to fullmektige - person og arbeidsgiver-orgnr`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = ArbeidsgiverMedFullmaktMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                fullmektigFnr = fullmektigFnr
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldContainExactlyInAnyOrder listOf(
            FullmektigDto(orgnr = null, personident = fullmektigFnr, fullmakter = listOf(Fullmaktstype.FULLMEKTIG_SØKNAD)),
            FullmektigDto(orgnr = arbeidsgiverOrgnr, personident = null, fullmakter = listOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER))
        )
    }

    @Test
    fun `RADGIVER (uten fullmakt) gir kun arbeidsgiver, radgiverfirma lagres ikke`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = RadgiverMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaOrgnr, navn = "Rådgiver AS")
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
    fun `RADGIVER_MED_FULLMAKT gir to fullmektige - person og radgiverfirma`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = RadgiverMedFullmaktMetadata(
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                fullmektigFnr = fullmektigFnr,
                radgiverfirma = RadgiverfirmaInfo(orgnr = radgiverfirmaOrgnr, navn = "Rådgiver AS")
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldContainExactlyInAnyOrder listOf(
            FullmektigDto(orgnr = null, personident = fullmektigFnr, fullmakter = listOf(Fullmaktstype.FULLMEKTIG_SØKNAD)),
            FullmektigDto(orgnr = radgiverfirmaOrgnr, personident = null, fullmakter = listOf(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER))
        )
    }

    @Test
    fun `ANNEN_PERSON gir én fullmektig - kun person med FULLMEKTIG_SØKNAD`() {
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            metadata = AnnenPersonMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "Test AS",
                juridiskEnhetOrgnr = juridiskEnhet,
                fullmektigFnr = fullmektigFnr
            )
            fnr = arbeidstakerFnr
            orgnr = arbeidsgiverOrgnr
        }

        val resultat = DigitalSøknadAktørerMapper.utled(søknadsdata)

        resultat.arbeidsgiverOrgnumre shouldBe listOf(arbeidsgiverOrgnr)
        resultat.fullmektige shouldBe listOf(
            FullmektigDto(orgnr = null, personident = fullmektigFnr, fullmakter = listOf(Fullmaktstype.FULLMEKTIG_SØKNAD))
        )
    }
}
