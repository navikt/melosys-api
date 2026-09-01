package no.nav.melosys.saksflyt.steg.soknad

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
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
    fun `metadata overstyrer gammelt manuelt svar`() {
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
    fun `eldre skjema uten registerklassifisering bruker manuelt svar`() {
        val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
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

        assertThat(BehandlingstemaUtleder.utled(dto)).isEqualTo(Behandlingstema.UTSENDT_ARBEIDSTAKER)
        assertThat(DigitalSøknadMapper.tilSoeknad(dto).juridiskArbeidsgiverNorge.erOffentligVirksomhet).isFalse()
    }

    @Test
    fun `koblede parter kan ha ulike versjoner og registerklassifisering er autoritativ`() {
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
                skjemaDefinisjonVersjon = "1"
                data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
                    arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                        erArbeidsgiverenOffentligVirksomhet = false
                    )
                )
            }
        }

        assertThat(dto.erOffentligArbeidsgiver()).isTrue()
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
            .hasMessageContaining("motstridende registerklassifisering")
    }

}
