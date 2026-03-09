package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Innretningstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Familiemedlem
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Farvann
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OffshoreDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OmBordPaFlyDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaSkipDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.TypeInnretning
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.PeriodeDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtsendtArbeidstakerSøknadMapperTest {

    @Nested
    inner class Soeknadsland {
        @Test
        fun `mapper søknadsland fra arbeidstaker-del`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.soeknadsland.landkoder shouldHaveSize 1
            søknad.soeknadsland.landkoder.first() shouldBe "DE"
            søknad.soeknadsland.isFlereLandUkjentHvilke.shouldBeFalse()
        }

        @Test
        fun `mapper søknadsland fra arbeidsgiver-del når arbeidstaker-del mangler`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.SE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.soeknadsland.landkoder shouldHaveSize 1
            søknad.soeknadsland.landkoder.first() shouldBe "SE"
        }

        @Test
        fun `arbeidstaker-del har presedens over arbeidsgiver-del for søknadsland`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = lagArbeidstakerData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    )
                )
                medKobletArbeidsgiverSkjema {
                    data = lagArbeidsgiverData(
                        utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                            utsendelseLand = LandKode.SE,
                            utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                        )
                    )
                }
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.soeknadsland.landkoder.first() shouldBe "DE"
        }

        @Test
        fun `returnerer tom soeknadsland når ingen utenlandsoppdraget finnes`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData()
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.soeknadsland.landkoder.shouldBeEmpty()
        }
    }

    @Nested
    inner class PeriodeMapping {
        @Test
        fun `mapper periode fra arbeidstaker-del`() {
            val fom = LocalDate.of(2025, 3, 1)
            val tom = LocalDate.of(2025, 9, 30)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(fom, tom)
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.periode.fom shouldBe fom
            søknad.periode.tom shouldBe tom
        }

        @Test
        fun `mapper periode fra arbeidsgiver-del når arbeidstaker-del mangler`() {
            val fom = LocalDate.of(2025, 6, 1)
            val tom = LocalDate.of(2026, 5, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.FI,
                        utsendelsePeriode = PeriodeDto(fom, tom)
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.periode.fom shouldBe fom
            søknad.periode.tom shouldBe tom
        }

        @Test
        fun `arbeidstaker-del har presedens over arbeidsgiver-del for periode`() {
            val arbeidstakerFom = LocalDate.of(2025, 1, 1)
            val arbeidstakerTom = LocalDate.of(2025, 6, 30)
            val arbeidsgiverFom = LocalDate.of(2025, 3, 1)
            val arbeidsgiverTom = LocalDate.of(2025, 12, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = lagArbeidstakerData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(arbeidstakerFom, arbeidstakerTom)
                    )
                )
                medKobletArbeidsgiverSkjema {
                    data = lagArbeidsgiverData(
                        utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                            utsendelseLand = LandKode.DE,
                            utsendelsePeriode = PeriodeDto(arbeidsgiverFom, arbeidsgiverTom)
                        )
                    )
                }
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.periode.fom shouldBe arbeidstakerFom
            søknad.periode.tom shouldBe arbeidstakerTom
        }
    }

    @Nested
    inner class Personopplysninger {
        @Test
        fun `mapper familiemedlemmer`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData(
                    familiemedlemmer = FamiliemedlemmerDto(
                        skalHaMedFamiliemedlemmer = true,
                        familiemedlemmer = listOf(
                            Familiemedlem(
                                fornavn = "Ola",
                                etternavn = "Nordmann",
                                harNorskFodselsnummerEllerDnummer = true,
                                fodselsnummer = "01012012345",
                                fodselsdato = null
                            )
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.personOpplysninger.medfolgendeFamilie shouldHaveSize 1
            søknad.personOpplysninger.medfolgendeFamilie.first().fnr shouldBe "01012012345"
            søknad.personOpplysninger.medfolgendeFamilie.first().navn shouldBe "Ola Nordmann"
        }

        @Test
        fun `returnerer tomme familiemedlemmer når skalHaMedFamiliemedlemmer er false`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData(
                    familiemedlemmer = FamiliemedlemmerDto(
                        skalHaMedFamiliemedlemmer = false,
                        familiemedlemmer = emptyList()
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.personOpplysninger.medfolgendeFamilie.shouldBeEmpty()
        }

        @Test
        fun `personopplysninger har default verdier når kun arbeidsgiver-del finnes`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData()
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.personOpplysninger.medfolgendeFamilie.shouldBeEmpty()
        }
    }

    @Nested
    inner class Arbeidssteder {
        @Test
        fun `mapper arbeid på land med fast arbeidssted`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_LAND,
                        paLand = PaLandDto(
                            navnPaVirksomhet = "Berlin GmbH",
                            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                            fastArbeidssted = PaLandFastArbeidsstedDto(
                                vegadresse = "Hauptstraße",
                                nummer = "42",
                                postkode = "10115",
                                bySted = "Berlin"
                            ),
                            beskrivelseVekslende = null,
                            erHjemmekontor = false
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.erFastArbeidssted shouldBe true
            søknad.arbeidPaaLand.erHjemmekontor shouldBe false
            val arbeidssted = søknad.arbeidPaaLand.fysiskeArbeidssteder.first()
            arbeidssted.virksomhetNavn shouldBe "Berlin GmbH"
            arbeidssted.adresse.gatenavn shouldBe "Hauptstraße"
            arbeidssted.adresse.husnummerEtasjeLeilighet shouldBe "42"
            arbeidssted.adresse.postnummer shouldBe "10115"
            arbeidssted.adresse.poststed shouldBe "Berlin"
        }

        @Test
        fun `mapper arbeid på land med vekslende arbeidssted`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_LAND,
                        paLand = PaLandDto(
                            navnPaVirksomhet = "Reise AS",
                            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
                            fastArbeidssted = null,
                            beskrivelseVekslende = "Diverse steder",
                            erHjemmekontor = false
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.arbeidPaaLand.erFastArbeidssted shouldBe false
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.fysiskeArbeidssteder.first().virksomhetNavn shouldBe "Reise AS"
        }

        @Test
        fun `mapper offshore arbeidssted`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.OFFSHORE,
                        offshore = OffshoreDto(
                            navnPaVirksomhet = "Equinor",
                            navnPaInnretning = "Troll A",
                            typeInnretning = TypeInnretning.PLATTFORM_ELLER_ANNEN_FAST_INNRETNING,
                            sokkelLand = LandKode.GB
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.maritimtArbeid shouldHaveSize 1
            val maritimt = søknad.maritimtArbeid.first()
            maritimt.enhetNavn shouldBe "Troll A"
            maritimt.innretningstype shouldBe Innretningstyper.PLATTFORM
            maritimt.innretningLandkode shouldBe "GB"
        }

        @Test
        fun `mapper boreskip offshore`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.OFFSHORE,
                        offshore = OffshoreDto(
                            navnPaVirksomhet = "Borr Drilling",
                            navnPaInnretning = "Deepsea Bergen",
                            typeInnretning = TypeInnretning.BORESKIP_ELLER_ANNEN_FLYTTBAR_INNRETNING,
                            sokkelLand = LandKode.DK
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.maritimtArbeid.first().innretningstype shouldBe Innretningstyper.BORESKIP
        }

        @Test
        fun `mapper arbeid på skip`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_SKIP,
                        paSkip = PaSkipDto(
                            navnPaVirksomhet = "Hurtigruten",
                            navnPaSkip = "MS Nordnorge",
                            yrketTilArbeidstaker = "Matros",
                            seilerI = Farvann.INTERNASJONALT_FARVANN,
                            flaggland = LandKode.DK,
                            territorialfarvannLand = null
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.maritimtArbeid shouldHaveSize 1
            val maritimt = søknad.maritimtArbeid.first()
            maritimt.enhetNavn shouldBe "MS Nordnorge"
            maritimt.fartsomradeKode shouldBe Fartsomrader.UTENRIKS
            maritimt.flaggLandkode shouldBe "DK"
            maritimt.territorialfarvannLandkode.shouldBeNull()
        }

        @Test
        fun `mapper arbeid på skip i territorialfarvann`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_SKIP,
                        paSkip = PaSkipDto(
                            navnPaVirksomhet = "Fjord Line",
                            navnPaSkip = "MS Stavangerfjord",
                            yrketTilArbeidstaker = "Kaptein",
                            seilerI = Farvann.TERRITORIALFARVANN,
                            flaggland = LandKode.SE,
                            territorialfarvannLand = LandKode.SE
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.maritimtArbeid.first().fartsomradeKode shouldBe Fartsomrader.INNENRIKS
            søknad.maritimtArbeid.first().territorialfarvannLandkode shouldBe "SE"
        }

        @Test
        fun `mapper luftfart`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
                        omBordPaFly = OmBordPaFlyDto(
                            navnPaVirksomhet = "SAS",
                            hjemmebaseLand = LandKode.SE,
                            hjemmebaseNavn = "Arlanda",
                            erVanligHjemmebase = true,
                            vanligHjemmebaseLand = null,
                            vanligHjemmebaseNavn = null
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.luftfartBaser shouldHaveSize 1
            val luftfart = søknad.luftfartBaser.first()
            luftfart.hjemmebaseNavn shouldBe "Arlanda"
            luftfart.hjemmebaseLand shouldBe "SE"
        }
    }

    @Nested
    inner class UtenlandsoppdragetMapping {
        @Test
        fun `mapper utenlandsoppdraget`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    utenlandsoppdraget = lagUtenlandsoppdragetDto(
                        arbeidsgiverHarOppdrag = true,
                        ansattForOppdraget = false,
                        forblirAnsatt = true,
                        erstatterAnnen = false
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet shouldBe true
            søknad.utenlandsoppdraget.erAnsattForOppdragIUtlandet shouldBe false
            søknad.utenlandsoppdraget.erFortsattAnsattEtterOppdraget shouldBe true
            søknad.utenlandsoppdraget.erErstatningTidligereUtsendte shouldBe false
            søknad.utenlandsoppdraget.erDrattPaaEgetInitiativ.shouldBeNull()
        }

        @Test
        fun `mapper samlet utsendingsperiode når erstatter annen person`() {
            val forrigePeriode = PeriodeDto(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    utenlandsoppdraget = lagUtenlandsoppdragetDto(
                        erstatterAnnen = true,
                        forrigePeriode = forrigePeriode
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.utenlandsoppdraget.erErstatningTidligereUtsendte shouldBe true
            søknad.utenlandsoppdraget.samletUtsendingsperiode.fom shouldBe LocalDate.of(2024, 1, 1)
            søknad.utenlandsoppdraget.samletUtsendingsperiode.tom shouldBe LocalDate.of(2024, 12, 31)
        }

        @Test
        fun `samlet utsendingsperiode er tom når ikke erstatter`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    utenlandsoppdraget = lagUtenlandsoppdragetDto(erstatterAnnen = false)
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.utenlandsoppdraget.samletUtsendingsperiode.fom.shouldBeNull()
            søknad.utenlandsoppdraget.samletUtsendingsperiode.tom.shouldBeNull()
        }

        @Test
        fun `returnerer default utenlandsoppdraget når kun arbeidstaker-del finnes`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData()
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet.shouldBeNull()
        }
    }

    @Nested
    inner class JuridiskArbeidsgiverNorgeMapping {
        @Test
        fun `mapper offentlig virksomhet`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    virksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                        erArbeidsgiverenOffentligVirksomhet = true
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe true
        }

        @Test
        fun `mapper privat virksomhet`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    virksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                        erArbeidsgiverenOffentligVirksomhet = false
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe false
        }
    }

    @Nested
    inner class LoennOgGodtgjoerelseMapping {
        @Test
        fun `mapper lønn og godtgjørelse`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    lonn = ArbeidstakerensLonnDto(
                        arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                        virksomheterSomUtbetalerLonnOgNaturalytelser = null
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.loennOgGodtgjoerelse?.norskArbgUtbetalerLoenn shouldBe true
        }

        @Test
        fun `mapper lønn og godtgjørelse med false`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    lonn = ArbeidstakerensLonnDto(
                        arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
                        virksomheterSomUtbetalerLonnOgNaturalytelser = null
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.loennOgGodtgjoerelse?.norskArbgUtbetalerLoenn shouldBe false
        }
    }

    @Nested
    inner class ArbeidssituasjonOgOevrigMapping {
        @Test
        fun `mapper arbeidssituasjon og øvrig`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData(
                    arbeidssituasjon = ArbeidssituasjonDto(
                        harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                        aktivitetIMaanedenFoerUtsendingen = "Jobbet hos arbeidsgiver i Norge",
                        skalJobbeForFlereVirksomheter = false,
                        virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
                    ),
                    skatteforhold = SkatteforholdOgInntektDto(
                        erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
                        mottarPengestotteFraAnnetEosLandEllerSveits = false,
                        landSomUtbetalerPengestotte = null,
                        pengestotteSomMottasFraAndreLandBelop = null,
                        pengestotteSomMottasFraAndreLandBeskrivelse = null
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending shouldBe true
            søknad.arbeidssituasjonOgOevrig.beskrivelseArbeidSisteMnd shouldBe "Jobbet hos arbeidsgiver i Norge"
            søknad.arbeidssituasjonOgOevrig.harAndreArbeidsgivereIUtsendingsperioden shouldBe false
            søknad.arbeidssituasjonOgOevrig.erSkattepliktig shouldBe true
            søknad.arbeidssituasjonOgOevrig.mottarYtelserUtlandet shouldBe false
        }

        @Test
        fun `arbeidssituasjon har default verdier når kun arbeidsgiver-del finnes`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData()
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending.shouldBeNull()
            søknad.arbeidssituasjonOgOevrig.beskrivelseArbeidSisteMnd.shouldBeNull()
            søknad.arbeidssituasjonOgOevrig.harAndreArbeidsgivereIUtsendingsperioden.shouldBeNull()
            søknad.arbeidssituasjonOgOevrig.erSkattepliktig.shouldBeNull()
            søknad.arbeidssituasjonOgOevrig.mottarYtelserUtlandet.shouldBeNull()
        }
    }

    @Nested
    inner class TilSoeknadIntegrasjon {
        @Test
        fun `mapper komplett søknad med begge deler`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = lagArbeidstakerData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    ),
                    arbeidssituasjon = ArbeidssituasjonDto(
                        harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                        aktivitetIMaanedenFoerUtsendingen = "Ansatt i Norge",
                        skalJobbeForFlereVirksomheter = false,
                        virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
                    ),
                    skatteforhold = SkatteforholdOgInntektDto(
                        erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
                        mottarPengestotteFraAnnetEosLandEllerSveits = false,
                        landSomUtbetalerPengestotte = null,
                        pengestotteSomMottasFraAndreLandBelop = null,
                        pengestotteSomMottasFraAndreLandBeskrivelse = null
                    ),
                    familiemedlemmer = FamiliemedlemmerDto(
                        skalHaMedFamiliemedlemmer = true,
                        familiemedlemmer = listOf(
                            Familiemedlem("Barn", "Barnsen", true, "01012012345", null)
                        )
                    )
                )
                medKobletArbeidsgiverSkjema {
                    data = lagArbeidsgiverData(
                        utenlandsoppdraget = lagUtenlandsoppdragetDto(
                            arbeidsgiverHarOppdrag = true,
                            ansattForOppdraget = false,
                            forblirAnsatt = true,
                            erstatterAnnen = false
                        ),
                        virksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                            erArbeidsgiverenOffentligVirksomhet = false
                        ),
                        lonn = ArbeidstakerensLonnDto(
                            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                            virksomheterSomUtbetalerLonnOgNaturalytelser = null
                        ),
                        arbeidssted = ArbeidsstedIUtlandetDto(
                            arbeidsstedType = ArbeidsstedType.PA_LAND,
                            paLand = PaLandDto(
                                navnPaVirksomhet = "Berlin GmbH",
                                fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                                fastArbeidssted = PaLandFastArbeidsstedDto("Hauptstraße", "1", "10115", "Berlin"),
                                beskrivelseVekslende = null,
                                erHjemmekontor = false
                            )
                        )
                    )
                }
            }

            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            // Søknadsland fra arbeidstaker (presedens)
            søknad.soeknadsland.landkoder shouldBe listOf("DE")

            // Periode fra arbeidstaker (presedens)
            søknad.periode.fom shouldBe LocalDate.of(2025, 1, 1)
            søknad.periode.tom shouldBe LocalDate.of(2025, 12, 31)

            // Personopplysninger
            søknad.personOpplysninger.medfolgendeFamilie shouldHaveSize 1

            // Arbeidssteder fra arbeidsgiver
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.erFastArbeidssted shouldBe true

            // Utenlandsoppdraget fra arbeidsgiver
            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet shouldBe true
            søknad.utenlandsoppdraget.erFortsattAnsattEtterOppdraget shouldBe true

            // Juridisk arbeidsgiver
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe false

            // Lønn
            søknad.loennOgGodtgjoerelse?.norskArbgUtbetalerLoenn shouldBe true

            // Arbeidssituasjon fra arbeidstaker
            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending shouldBe true
            søknad.arbeidssituasjonOgOevrig.erSkattepliktig shouldBe true
        }

        @Test
        fun `mapper søknad med kun arbeidstaker-del`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = lagArbeidstakerData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.FI,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 12, 31))
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.soeknadsland.landkoder shouldBe listOf("FI")
            søknad.periode.fom shouldBe LocalDate.of(2025, 6, 1)
            // Arbeidsgiver-spesifikke felter forblir default
            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet.shouldBeNull()
        }

        @Test
        fun `mapper søknad med kun arbeidsgiver-del`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = lagArbeidsgiverData(
                    utenlandsoppdraget = lagUtenlandsoppdragetDto(
                        arbeidsgiverHarOppdrag = true,
                        erstatterAnnen = false
                    ),
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.SE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 8, 31))
                    ),
                    virksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(erArbeidsgiverenOffentligVirksomhet = true)
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.soeknadsland.landkoder shouldBe listOf("SE")
            søknad.periode.fom shouldBe LocalDate.of(2025, 3, 1)
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe true
            // Arbeidstaker-spesifikke felter forblir default
            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending.shouldBeNull()
        }
    }

    @Nested
    inner class KombinertSkjema {
        @Test
        fun `mapper søknadsland og periode fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.soeknadsland.landkoder shouldBe listOf("DE")
            søknad.soeknadsland.isFlereLandUkjentHvilke.shouldBeFalse()
            søknad.periode.fom shouldBe LocalDate.of(2025, 1, 1)
            søknad.periode.tom shouldBe LocalDate.of(2025, 12, 31)
        }

        @Test
        fun `mapper lønn og godtgjørelse fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                        arbeidstakerensLonn = ArbeidstakerensLonnDto(
                            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                            virksomheterSomUtbetalerLonnOgNaturalytelser = null
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.loennOgGodtgjoerelse?.norskArbgUtbetalerLoenn shouldBe true
        }

        @Test
        fun `mapper utenlandsoppdraget fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                        utenlandsoppdraget = lagUtenlandsoppdragetDto(
                            arbeidsgiverHarOppdrag = true,
                            ansattForOppdraget = false,
                            forblirAnsatt = true,
                            erstatterAnnen = false
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet shouldBe true
            søknad.utenlandsoppdraget.erAnsattForOppdragIUtlandet shouldBe false
            søknad.utenlandsoppdraget.erFortsattAnsattEtterOppdraget shouldBe true
            søknad.utenlandsoppdraget.erErstatningTidligereUtsendte shouldBe false
            søknad.utenlandsoppdraget.erDrattPaaEgetInitiativ.shouldBeNull()
        }

        @Test
        fun `mapper arbeidssituasjon fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    arbeidstakersData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData(
                        arbeidssituasjon = ArbeidssituasjonDto(
                            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                            aktivitetIMaanedenFoerUtsendingen = "Jobbet i Norge",
                            skalJobbeForFlereVirksomheter = false,
                            virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
                        ),
                        skatteforholdOgInntekt = SkatteforholdOgInntektDto(
                            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
                            mottarPengestotteFraAnnetEosLandEllerSveits = false,
                            landSomUtbetalerPengestotte = null,
                            pengestotteSomMottasFraAndreLandBelop = null,
                            pengestotteSomMottasFraAndreLandBeskrivelse = null
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending shouldBe true
            søknad.arbeidssituasjonOgOevrig.beskrivelseArbeidSisteMnd shouldBe "Jobbet i Norge"
            søknad.arbeidssituasjonOgOevrig.harAndreArbeidsgivereIUtsendingsperioden shouldBe false
            søknad.arbeidssituasjonOgOevrig.erSkattepliktig shouldBe true
            søknad.arbeidssituasjonOgOevrig.mottarYtelserUtlandet shouldBe false
        }

        @Test
        fun `mapper juridisk arbeidsgiver fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                        arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                            erArbeidsgiverenOffentligVirksomhet = true
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe true
        }

        @Test
        fun `mapper familiemedlemmer fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    arbeidstakersData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData(
                        familiemedlemmer = FamiliemedlemmerDto(
                            skalHaMedFamiliemedlemmer = true,
                            familiemedlemmer = listOf(
                                Familiemedlem(
                                    fornavn = "Kari",
                                    etternavn = "Nordmann",
                                    harNorskFodselsnummerEllerDnummer = true,
                                    fodselsnummer = "02022012345",
                                    fodselsdato = null
                                )
                            )
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.personOpplysninger.medfolgendeFamilie shouldHaveSize 1
            søknad.personOpplysninger.medfolgendeFamilie.first().fnr shouldBe "02022012345"
            søknad.personOpplysninger.medfolgendeFamilie.first().navn shouldBe "Kari Nordmann"
        }

        @Test
        fun `mapper arbeidssteder på land fra kombinert skjema`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                        arbeidsstedIUtlandet = ArbeidsstedIUtlandetDto(
                            arbeidsstedType = ArbeidsstedType.PA_LAND,
                            paLand = PaLandDto(
                                navnPaVirksomhet = "München GmbH",
                                fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                                fastArbeidssted = PaLandFastArbeidsstedDto(
                                    vegadresse = "Marienplatz",
                                    nummer = "1",
                                    postkode = "80331",
                                    bySted = "München"
                                ),
                                beskrivelseVekslende = null,
                                erHjemmekontor = false
                            )
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.erFastArbeidssted shouldBe true
            søknad.arbeidPaaLand.erHjemmekontor shouldBe false
            val arbeidssted = søknad.arbeidPaaLand.fysiskeArbeidssteder.first()
            arbeidssted.virksomhetNavn shouldBe "München GmbH"
            arbeidssted.adresse.gatenavn shouldBe "Marienplatz"
        }

        @Test
        fun `mapper komplett kombinert søknad med alle felter`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData(
                    utsendingsperiodeOgLand = UtsendingsperiodeOgLandDto(
                        utsendelseLand = LandKode.DE,
                        utsendelsePeriode = PeriodeDto(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
                    ),
                    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                        arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                            erArbeidsgiverenOffentligVirksomhet = false
                        ),
                        utenlandsoppdraget = lagUtenlandsoppdragetDto(
                            arbeidsgiverHarOppdrag = true,
                            forblirAnsatt = true,
                            erstatterAnnen = false
                        ),
                        arbeidstakerensLonn = ArbeidstakerensLonnDto(
                            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
                            virksomheterSomUtbetalerLonnOgNaturalytelser = null
                        ),
                        arbeidsstedIUtlandet = ArbeidsstedIUtlandetDto(
                            arbeidsstedType = ArbeidsstedType.PA_LAND,
                            paLand = PaLandDto(
                                navnPaVirksomhet = "Berlin GmbH",
                                fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                                fastArbeidssted = PaLandFastArbeidsstedDto("Hauptstraße", "1", "10115", "Berlin"),
                                beskrivelseVekslende = null,
                                erHjemmekontor = false
                            )
                        )
                    ),
                    arbeidstakersData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData(
                        arbeidssituasjon = ArbeidssituasjonDto(
                            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
                            aktivitetIMaanedenFoerUtsendingen = "Ansatt i Norge",
                            skalJobbeForFlereVirksomheter = false,
                            virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
                        ),
                        skatteforholdOgInntekt = SkatteforholdOgInntektDto(
                            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
                            mottarPengestotteFraAnnetEosLandEllerSveits = false,
                            landSomUtbetalerPengestotte = null,
                            pengestotteSomMottasFraAndreLandBelop = null,
                            pengestotteSomMottasFraAndreLandBeskrivelse = null
                        ),
                        familiemedlemmer = FamiliemedlemmerDto(
                            skalHaMedFamiliemedlemmer = true,
                            familiemedlemmer = listOf(
                                Familiemedlem("Barn", "Barnsen", true, "01012012345", null)
                            )
                        )
                    )
                )
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            // Søknadsland og periode
            søknad.soeknadsland.landkoder shouldBe listOf("DE")
            søknad.periode.fom shouldBe LocalDate.of(2025, 1, 1)
            søknad.periode.tom shouldBe LocalDate.of(2025, 12, 31)

            // Arbeidsgiver-data
            søknad.loennOgGodtgjoerelse?.norskArbgUtbetalerLoenn shouldBe true
            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet shouldBe true
            søknad.utenlandsoppdraget.erFortsattAnsattEtterOppdraget shouldBe true
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe false
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.erFastArbeidssted shouldBe true

            // Arbeidstaker-data
            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending shouldBe true
            søknad.arbeidssituasjonOgOevrig.beskrivelseArbeidSisteMnd shouldBe "Ansatt i Norge"
            søknad.arbeidssituasjonOgOevrig.erSkattepliktig shouldBe true
            søknad.personOpplysninger.medfolgendeFamilie shouldHaveSize 1
        }

        @Test
        fun `kombinert skjema med tomme data gir default-verdier`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                data = lagKombinertData()
            }
            val søknad = UtsendtArbeidstakerSøknadMapper.tilSoeknad(dto)

            søknad.soeknadsland.landkoder.shouldBeEmpty()
            søknad.periode.fom.shouldBeNull()
            søknad.periode.tom.shouldBeNull()
            søknad.loennOgGodtgjoerelse.shouldBeNull()
            søknad.utenlandsoppdraget.erUtsendelseForOppdragIUtlandet.shouldBeNull()
            søknad.arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending.shouldBeNull()
            søknad.personOpplysninger.medfolgendeFamilie.shouldBeEmpty()
        }
    }

    // --- Test data helpers (mapper-specific) ---

    private fun lagArbeidstakerData(
        utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
        arbeidssituasjon: ArbeidssituasjonDto? = null,
        skatteforhold: SkatteforholdOgInntektDto? = null,
        familiemedlemmer: FamiliemedlemmerDto? = null
    ) = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
        utsendingsperiodeOgLand = utsendingsperiodeOgLand,
        arbeidssituasjon = arbeidssituasjon,
        skatteforholdOgInntekt = skatteforhold,
        familiemedlemmer = familiemedlemmer
    )

    private fun lagArbeidsgiverData(
        utenlandsoppdraget: UtenlandsoppdragetDto? = null,
        virksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
        lonn: ArbeidstakerensLonnDto? = null,
        arbeidssted: ArbeidsstedIUtlandetDto? = null,
        utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null
    ) = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
        utenlandsoppdraget = utenlandsoppdraget,
        arbeidsgiverensVirksomhetINorge = virksomhetINorge,
        arbeidstakerensLonn = lonn,
        arbeidsstedIUtlandet = arbeidssted,
        utsendingsperiodeOgLand = utsendingsperiodeOgLand
    )

    private fun lagUtenlandsoppdragetDto(
        arbeidsgiverHarOppdrag: Boolean = true,
        ansattForOppdraget: Boolean = false,
        forblirAnsatt: Boolean = true,
        erstatterAnnen: Boolean = false,
        forrigePeriode: PeriodeDto? = null
    ) = UtenlandsoppdragetDto(
        arbeidsgiverHarOppdragILandet = arbeidsgiverHarOppdrag,
        arbeidstakerBleAnsattForUtenlandsoppdraget = ansattForOppdraget,
        arbeidstakerForblirAnsattIHelePerioden = forblirAnsatt,
        arbeidstakerErstatterAnnenPerson = erstatterAnnen,
        arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget = null,
        utenlandsoppholdetsBegrunnelse = null,
        ansettelsesforholdBeskrivelse = null,
        forrigeArbeidstakerUtsendelsePeriode = forrigePeriode
    )

    private fun lagKombinertData(
        arbeidsgiversData: UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData =
            UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(),
        arbeidstakersData: UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData =
            UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData(),
        utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null
    ) = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
        arbeidsgiversData = arbeidsgiversData,
        arbeidstakersData = arbeidstakersData,
        utsendingsperiodeOgLand = utsendingsperiodeOgLand
    )
}
