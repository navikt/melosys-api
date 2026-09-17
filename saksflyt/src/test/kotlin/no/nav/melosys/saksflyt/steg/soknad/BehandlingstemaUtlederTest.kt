package no.nav.melosys.saksflyt.steg.soknad

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto

class BehandlingstemaUtlederTest {

    @Test
    fun `ren arbeidstakerdel bruker offentlig-status fra metadata`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
            data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            metadata = DegSelvMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = arbeidsgiverNavn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn,
                erOffentligArbeidsgiver = true
            )
        }

        assertThat(BehandlingstemaUtleder.utled(dto))
            .isEqualTo(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
        assertThat(DigitalSøknadMapper.tilSoeknad(dto).juridiskArbeidsgiverNorge.erOffentligVirksomhet).isTrue()
    }

    @Test
    fun `metadata overstyrer gammelt brukersvar`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
                arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                    erArbeidsgiverenOffentligVirksomhet = true
                )
            )
            metadata = ArbeidsgiverMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = arbeidsgiverNavn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn,
                erOffentligArbeidsgiver = false
            )
        }

        assertThat(BehandlingstemaUtleder.utled(dto)).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER)
        assertThat(DigitalSøknadMapper.tilSoeknad(dto).juridiskArbeidsgiverNorge.erOffentligVirksomhet).isFalse()
    }

    @Test
    fun `manglende registerklassifisering stopper behandlingen uten fallback til gammelt brukersvar`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemaDefinisjonVersjon = "1"
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
                arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                    erArbeidsgiverenOffentligVirksomhet = false,
                    erArbeidsgiverenBemanningsEllerVikarbyraa = false,
                    opprettholderArbeidsgiverenVanligDrift = true
                )
            )
            metadata = ArbeidsgiverMetadata(
                skjemadel = skjemadel,
                arbeidsgiverNavn = arbeidsgiverNavn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn,
                erOffentligArbeidsgiver = null
            )
        }

        assertThatThrownBy { BehandlingstemaUtleder.utled(dto) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Mangler registerklassifisering fra EREG")
            .hasMessageContaining("V1")
        assertThatThrownBy { DigitalSøknadMapper.tilSoeknad(dto) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Mangler registerklassifisering fra EREG")
    }

    @ParameterizedTest
    @CsvSource("1,2", "2,1")
    fun `koblede parter på ulike versjoner bruker registermetadata uten V1-fallback`(
        arbeidstakerVersjon: String,
        arbeidsgiverVersjon: String
    ) {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemaDefinisjonVersjon = arbeidstakerVersjon
            metadata = DegSelvMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = arbeidsgiverNavn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn,
                erOffentligArbeidsgiver = true.takeIf { arbeidstakerVersjon == "2" }
            )
            medKobletArbeidsgiverSkjema {
                skjemaDefinisjonVersjon = arbeidsgiverVersjon
                erOffentligArbeidsgiver = true.takeIf { arbeidsgiverVersjon == "2" }
                data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
                    arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                        erArbeidsgiverenOffentligVirksomhet = false
                    )
                )
            }
        }

        assertThat(dto.erOffentligArbeidsgiver()).isTrue()
        assertThat(BehandlingstemaUtleder.utled(dto)).isEqualTo(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
        assertThat(DigitalSøknadMapper.tilSoeknad(dto).juridiskArbeidsgiverNorge.erOffentligVirksomhet).isTrue()

        val medArbeidsgiverSomHovedskjema = dto.copy(skjema = requireNotNull(dto.kobletSkjema), kobletSkjema = dto.skjema)
        assertThat(BehandlingstemaUtleder.utled(medArbeidsgiverSomHovedskjema))
            .isEqualTo(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
        assertThat(DigitalSøknadMapper.tilSoeknad(medArbeidsgiverSomHovedskjema).juridiskArbeidsgiverNorge.erOffentligVirksomhet)
            .isTrue()
    }

    @Test
    fun `motstridende registerklassifisering i koblede skjemaer feiler`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemaDefinisjonVersjon = "2"
            metadata = DegSelvMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = arbeidsgiverNavn,
                juridiskEnhetOrgnr = juridiskEnhetOrgnr,
                arbeidstakerNavn = arbeidstakerNavn,
                erOffentligArbeidsgiver = true
            )
            medKobletArbeidsgiverSkjema {
                skjemaDefinisjonVersjon = "2"
                erOffentligArbeidsgiver = false
            }
        }

        assertThatThrownBy { dto.erOffentligArbeidsgiver() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("motstridende klassifisering i EREG")
    }

}
