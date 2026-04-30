package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Innretningstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.skjema.types.felles.Ansettelsesform
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.NorskVirksomhet
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhet
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhetMedAnsettelsesform
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Farvann
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OffshoreDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OmBordPaFlyDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandFastArbeidsstedDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaSkipDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.TypeInnretning
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DigitalSøknadMapperTest {

    @Nested
    inner class PeriodeOgLand {

        @Test
        fun `mapper periode fra arbeidstaker-del`() {
            val fom = LocalDate.of(2025, 1, 1)
            val tom = LocalDate.of(2025, 12, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = arbeidstakerData(utsendingsperiodeOgLand = landOgPeriode(LandKode.DE, fom, tom))
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.periode.fom shouldBe fom
            søknad.periode.tom shouldBe tom
            søknad.soeknadsland.landkoder shouldBe listOf("DE")
        }

        @Test
        fun `mapper periode fra arbeidsgiver-del når arbeidstaker-del mangler`() {
            val fom = LocalDate.of(2025, 6, 1)
            val tom = LocalDate.of(2026, 5, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(utsendingsperiodeOgLand = landOgPeriode(LandKode.FI, fom, tom))
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.periode.fom shouldBe fom
            søknad.periode.tom shouldBe tom
            søknad.soeknadsland.landkoder shouldBe listOf("FI")
        }

        @Test
        fun `arbeidstaker-del har presedens over arbeidsgiver-del`() {
            val atFom = LocalDate.of(2025, 1, 1)
            val atTom = LocalDate.of(2025, 6, 30)
            val agFom = LocalDate.of(2025, 3, 1)
            val agTom = LocalDate.of(2025, 12, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = arbeidstakerData(utsendingsperiodeOgLand = landOgPeriode(LandKode.DE, atFom, atTom))
                medKobletArbeidsgiverSkjema {
                    data = arbeidsgiverData(utsendingsperiodeOgLand = landOgPeriode(LandKode.SE, agFom, agTom))
                }
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.periode.fom shouldBe atFom
            søknad.periode.tom shouldBe atTom
            søknad.soeknadsland.landkoder shouldBe listOf("DE")
        }

        @Test
        fun `null periode og land gir tomme defaultverdier`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = arbeidstakerData()
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.periode.fom.shouldBeNull()
            søknad.periode.tom.shouldBeNull()
            søknad.soeknadsland.landkoder.shouldBeEmpty()
        }
    }

    @Nested
    inner class NorskArbeidsgiver {

        @Test
        fun `mapper hovedarbeidsgivers orgnr fra arbeidsgiver-del alene`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                orgnr = "999888777"
                data = arbeidsgiverData()
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("999888777")
        }

        @Test
        fun `mapper hovedarbeidsgivers orgnr fra arbeidstaker-del alene`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                orgnr = "111222333"
                data = arbeidstakerData()
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("111222333")
        }

        @Test
        fun `arbeidstaker-delens orgnr har presedens over arbeidsgiver-delens når begge er sendt`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                orgnr = "ATORGNR99"
                data = arbeidstakerData()
                medKobletArbeidsgiverSkjema {
                    orgnr = "AGORGNR11"
                    data = arbeidsgiverData()
                }
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("ATORGNR99")
        }

        @Test
        fun `arbeidsgiver-delens orgnr er fallback når arbeidstaker-del ikke er mottatt`() {
            // Hovedskjema er arbeidsgivers del, ingen koblet arbeidstaker-del
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                orgnr = "FALLBACK88"
                data = arbeidsgiverData()
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("FALLBACK88")
        }

        @Test
        fun `erOffentligVirksomhet mappes fra arbeidsgiver-delens virksomhet i Norge`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
                    virksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                        erArbeidsgiverenOffentligVirksomhet = true
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe true
        }

        @Test
        fun `erOffentligVirksomhet er null når arbeidsgiver-del mangler`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = arbeidstakerData()
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet.shouldBeNull()
        }
    }

    @Nested
    inner class UtenlandskArbeidsgiver {

        @Test
        fun `mapper utenlandske virksomheter fra arbeidsgiver-delens lønnsliste`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
                    lonn = lonnMedVirksomheter(
                        utenlandske = listOf(
                            UtenlandskVirksomhet(
                                navn = "Berlin GmbH",
                                organisasjonsnummer = "DE-123",
                                vegnavnOgHusnummer = "Hauptstraße 42",
                                bygning = "B",
                                postkode = "10115",
                                byStedsnavn = "Berlin",
                                region = "Berlin",
                                land = "DE",
                                tilhorerSammeKonsern = true
                            )
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland shouldHaveSize 1
            val foretak = søknad.foretakUtland.first()
            foretak.navn shouldBe "Berlin GmbH"
            foretak.orgnr shouldBe "DE-123"
            foretak.tilhorerSammeKonsern shouldBe true
            foretak.uuid.shouldNotBeNull()
            foretak.adresse.gatenavn shouldBe "Hauptstraße 42"
            foretak.adresse.tilleggsnavn shouldBe "B"
            foretak.adresse.postnummer shouldBe "10115"
            foretak.adresse.poststed shouldBe "Berlin"
            foretak.adresse.region shouldBe "Berlin"
            foretak.adresse.landkode shouldBe "DE"
        }

        @Test
        fun `mapper utenlandske virksomheter fra arbeidstaker-delens virksomhetsliste alene`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = arbeidstakerData(
                    arbeidssituasjon = arbeidssituasjonMedVirksomheter(
                        utenlandske = listOf(
                            utenlandskMedAnsettelsesform("AT-virksomhet", "DE", Ansettelsesform.SELVSTENDIG_NAERINGSDRIVENDE)
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland shouldHaveSize 1
            søknad.foretakUtland.first().navn shouldBe "AT-virksomhet"
            søknad.foretakUtland.first().selvstendigNæringsvirksomhet shouldBe true
        }

        @Test
        fun `dedupliserer identiske virksomheter fra AT- og AG-del — bare en oppfoering`() {
            val felles = utenlandskBase("Sammeselskap GmbH", "DE-100", "DE")
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = arbeidstakerData(
                    arbeidssituasjon = arbeidssituasjonMedVirksomheter(
                        utenlandske = listOf(felles.medAnsettelsesform(Ansettelsesform.ARBEIDSTAKER_ELLER_FRILANSER))
                    )
                )
                medKobletArbeidsgiverSkjema {
                    data = arbeidsgiverData(
                        lonn = lonnMedVirksomheter(utenlandske = listOf(felles))
                    )
                }
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland shouldHaveSize 1
            søknad.foretakUtland.first().navn shouldBe "Sammeselskap GmbH"
        }

        @Test
        fun `to ulike virksomheter fra AT- og AG-del gir to oppforinger`() {
            val agVirksomhet = utenlandskBase("AG-firma", "DE-1", "DE")
            val atVirksomhet = utenlandskBase("AT-firma", "SE-2", "SE")
                .medAnsettelsesform(Ansettelsesform.ARBEIDSTAKER_ELLER_FRILANSER)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = arbeidstakerData(
                    arbeidssituasjon = arbeidssituasjonMedVirksomheter(utenlandske = listOf(atVirksomhet))
                )
                medKobletArbeidsgiverSkjema {
                    data = arbeidsgiverData(lonn = lonnMedVirksomheter(utenlandske = listOf(agVirksomhet)))
                }
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland shouldHaveSize 2
            søknad.foretakUtland.map { it.navn }.toSet() shouldBe setOf("AT-firma", "AG-firma")
        }

        @Test
        fun `liten forskjell paa orgnr forhindrer dedup — gir to oppforinger`() {
            val ag = utenlandskBase("Likt navn", "DE-100", "DE")
            val at = utenlandskBase("Likt navn", "DE-101", "DE")
                .medAnsettelsesform(Ansettelsesform.ARBEIDSTAKER_ELLER_FRILANSER)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = arbeidstakerData(
                    arbeidssituasjon = arbeidssituasjonMedVirksomheter(utenlandske = listOf(at))
                )
                medKobletArbeidsgiverSkjema {
                    data = arbeidsgiverData(lonn = lonnMedVirksomheter(utenlandske = listOf(ag)))
                }
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland shouldHaveSize 2
        }

        @Test
        fun `arbeidsgiver-delen leverer utenlandske virksomheter selv når arbeidstaker også er mottatt`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                data = arbeidstakerData()
                medKobletArbeidsgiverSkjema {
                    data = arbeidsgiverData(
                        lonn = lonnMedVirksomheter(
                            utenlandske = listOf(
                                UtenlandskVirksomhet(
                                    navn = "AG-virksomhet", organisasjonsnummer = null,
                                    vegnavnOgHusnummer = "x", bygning = null, postkode = null,
                                    byStedsnavn = null, region = null, land = "DE",
                                    tilhorerSammeKonsern = false
                                )
                            )
                        )
                    )
                }
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland shouldHaveSize 1
            søknad.foretakUtland.first().navn shouldBe "AG-virksomhet"
            søknad.foretakUtland.first().tilhorerSammeKonsern shouldBe false
        }

        @Test
        fun `ingen utenlandske virksomheter gir tom liste`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData()
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.foretakUtland.shouldBeEmpty()
        }
    }

    @Nested
    inner class ArbeidsstedPaaLand {

        @Test
        fun `mapper fast arbeidssted på land`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_LAND,
                        paLand = PaLandDto(
                            navnPaVirksomhet = "Berlin GmbH",
                            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                            fastArbeidssted = PaLandFastArbeidsstedDto("Hauptstraße", "42", "10115", "Berlin"),
                            beskrivelseVekslende = null,
                            erHjemmekontor = false
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.arbeidPaaLand.erFastArbeidssted shouldBe true
            søknad.arbeidPaaLand.erHjemmekontor shouldBe false
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            val sted = søknad.arbeidPaaLand.fysiskeArbeidssteder.first()
            sted.virksomhetNavn shouldBe "Berlin GmbH"
            sted.adresse.gatenavn shouldBe "Hauptstraße"
            sted.adresse.husnummerEtasjeLeilighet shouldBe "42"
            sted.adresse.postnummer shouldBe "10115"
            sted.adresse.poststed shouldBe "Berlin"
        }

        @Test
        fun `mapper vekslende arbeidssted på land`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
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

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.arbeidPaaLand.erFastArbeidssted.shouldBeFalse()
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.fysiskeArbeidssteder.first().virksomhetNavn shouldBe "Reise AS"
        }

        @Test
        fun `mapper hjemmekontor-flagg`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_LAND,
                        paLand = PaLandDto(
                            navnPaVirksomhet = "Home Office AS",
                            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                            fastArbeidssted = PaLandFastArbeidsstedDto("Street", "1", "00000", "By"),
                            beskrivelseVekslende = null,
                            erHjemmekontor = true
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.arbeidPaaLand.erHjemmekontor.shouldBeTrue()
        }
    }

    @Nested
    inner class ArbeidsstedOffshore {

        @Test
        fun `mapper plattform offshore`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
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

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.maritimtArbeid shouldHaveSize 1
            val m = søknad.maritimtArbeid.first()
            m.enhetNavn shouldBe "Troll A"
            m.innretningstype shouldBe Innretningstyper.PLATTFORM
            m.innretningLandkode shouldBe "GB"
        }

        @Test
        fun `mapper boreskip offshore`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
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

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.maritimtArbeid.first().innretningstype shouldBe Innretningstyper.BORESKIP
        }
    }

    @Nested
    inner class ArbeidsstedPaaSkip {

        @Test
        fun `mapper skip i internasjonalt farvann`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
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

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            val m = søknad.maritimtArbeid.first()
            m.enhetNavn shouldBe "MS Nordnorge"
            m.fartsomradeKode shouldBe Fartsomrader.UTENRIKS
            m.flaggLandkode shouldBe "DK"
            m.territorialfarvannLandkode.shouldBeNull()
        }

        @Test
        fun `mapper skip i territorialfarvann`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
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

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            val m = søknad.maritimtArbeid.first()
            m.fartsomradeKode shouldBe Fartsomrader.INNENRIKS
            m.territorialfarvannLandkode shouldBe "SE"
        }

        @Test
        fun `mapper yrke på arbeidstaker på skip`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_SKIP,
                        paSkip = PaSkipDto(
                            navnPaVirksomhet = "Hurtigruten",
                            navnPaSkip = "MS Nordnorge",
                            yrketTilArbeidstaker = "Maskinsjef",
                            seilerI = Farvann.INTERNASJONALT_FARVANN,
                            flaggland = null,
                            territorialfarvannLand = null
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.maritimtArbeid.first().yrke shouldBe "Maskinsjef"
        }
    }

    @Nested
    inner class ArbeidsstedPaaFly {

        @Test
        fun `mapper hjemmebase for fly der dette er den vanlige hjemmebasen`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
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

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.luftfartBaser shouldHaveSize 1
            val base = søknad.luftfartBaser.first()
            base.hjemmebaseNavn shouldBe "Arlanda"
            base.hjemmebaseLand shouldBe "SE"
            base.erVanligHjemmebase shouldBe true
            base.vanligHjemmebaseNavn.shouldBeNull()
            base.vanligHjemmebaseLand.shouldBeNull()
        }

        @Test
        fun `mapper hjemmebase for fly der vanlig hjemmebase er en annen`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                data = arbeidsgiverData(
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
                        omBordPaFly = OmBordPaFlyDto(
                            navnPaVirksomhet = "Norwegian",
                            hjemmebaseLand = LandKode.ES,
                            hjemmebaseNavn = "Malaga",
                            erVanligHjemmebase = false,
                            vanligHjemmebaseLand = LandKode.NO,
                            vanligHjemmebaseNavn = "Oslo Gardermoen"
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            val base = søknad.luftfartBaser.first()
            base.erVanligHjemmebase shouldBe false
            base.vanligHjemmebaseLand shouldBe "NO"
            base.vanligHjemmebaseNavn shouldBe "Oslo Gardermoen"
        }
    }

    @Nested
    inner class KombinertOgFullflyt {

        @Test
        fun `komplett kombinert søknad mapper alle sidemeny-seksjoner`() {
            val fom = LocalDate.of(2025, 1, 1)
            val tom = LocalDate.of(2025, 12, 31)
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
                orgnr = "KOMBI-123"
                data = kombinertData(
                    utsendingsperiodeOgLand = landOgPeriode(LandKode.DE, fom, tom),
                    arbeidsgiversData = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(
                        arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                            erArbeidsgiverenOffentligVirksomhet = false
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
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.periode.fom shouldBe fom
            søknad.periode.tom shouldBe tom
            søknad.soeknadsland.landkoder shouldBe listOf("DE")
            søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("KOMBI-123")
            søknad.juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe false
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.arbeidPaaLand.fysiskeArbeidssteder.first().virksomhetNavn shouldBe "Berlin GmbH"
        }

        @Test
        fun `kun arbeidstaker-del uten koblet skjema gir defaultverdier for arbeidssted`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
                data = arbeidstakerData(
                    utsendingsperiodeOgLand = landOgPeriode(LandKode.FI, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 12, 31))
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.soeknadsland.landkoder shouldBe listOf("FI")
            søknad.arbeidPaaLand.fysiskeArbeidssteder.shouldBeEmpty()
            søknad.maritimtArbeid.shouldBeEmpty()
            søknad.luftfartBaser.shouldBeEmpty()
        }

        @Test
        fun `kun arbeidsgiver-del gir arbeidssted og orgnr fra arbeidsgiver-del`() {
            val dto = lagUtsendtArbeidstakerSkjemaM2MDto {
                skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
                orgnr = "AG-KUN"
                data = arbeidsgiverData(
                    utsendingsperiodeOgLand = landOgPeriode(LandKode.SE, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 8, 31)),
                    arbeidssted = ArbeidsstedIUtlandetDto(
                        arbeidsstedType = ArbeidsstedType.PA_LAND,
                        paLand = PaLandDto(
                            navnPaVirksomhet = "Stockholm AB",
                            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                            fastArbeidssted = PaLandFastArbeidsstedDto("Drottninggatan", "5", "11151", "Stockholm"),
                            beskrivelseVekslende = null,
                            erHjemmekontor = false
                        )
                    )
                )
            }

            val søknad = DigitalSøknadMapper.tilSoeknad(dto)

            søknad.soeknadsland.landkoder shouldBe listOf("SE")
            søknad.arbeidPaaLand.fysiskeArbeidssteder shouldHaveSize 1
            søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere shouldContainExactly listOf("AG-KUN")
            søknad.foretakUtland.shouldBeEmpty()
        }
    }

    // --- Testdata-hjelpere ---

    private fun landOgPeriode(land: LandKode, fom: LocalDate, tom: LocalDate) =
        UtsendingsperiodeOgLandDto(utsendelseLand = land, utsendelsePeriode = PeriodeDto(fom, tom))

    private fun arbeidstakerData(
        utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
        arbeidssituasjon: ArbeidssituasjonDto? = null
    ) = UtsendtArbeidstakerArbeidstakersSkjemaDataDto(
        utsendingsperiodeOgLand = utsendingsperiodeOgLand,
        arbeidssituasjon = arbeidssituasjon
    )

    private fun arbeidsgiverData(
        utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
        lonn: ArbeidstakerensLonnDto? = null,
        arbeidssted: ArbeidsstedIUtlandetDto? = null,
        virksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null
    ) = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
        utsendingsperiodeOgLand = utsendingsperiodeOgLand,
        arbeidsgiverensVirksomhetINorge = virksomhetINorge,
        arbeidstakerensLonn = lonn,
        arbeidsstedIUtlandet = arbeidssted
    )

    private fun kombinertData(
        utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto? = null,
        arbeidsgiversData: UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData =
            UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidsgiversData(),
        arbeidstakersData: UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData =
            UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto.ArbeidstakersData()
    ) = UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(
        utsendingsperiodeOgLand = utsendingsperiodeOgLand,
        arbeidsgiversData = arbeidsgiversData,
        arbeidstakersData = arbeidstakersData
    )

    private fun lonnMedVirksomheter(
        norske: List<NorskVirksomhet> = emptyList(),
        utenlandske: List<UtenlandskVirksomhet> = emptyList()
    ) = ArbeidstakerensLonnDto(
        arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
        virksomheterSomUtbetalerLonnOgNaturalytelser = NorskeOgUtenlandskeVirksomheter(norske, utenlandske)
    )

    private fun arbeidssituasjonMedVirksomheter(
        norske: List<NorskVirksomhet> = emptyList(),
        utenlandske: List<UtenlandskVirksomhetMedAnsettelsesform> = emptyList()
    ) = ArbeidssituasjonDto(
        harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
        aktivitetIMaanedenFoerUtsendingen = null,
        skalJobbeForFlereVirksomheter = true,
        virksomheterArbeidstakerJobberForIutsendelsesPeriode =
            NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(norske, utenlandske)
    )

    private fun utenlandskMedAnsettelsesform(navn: String, land: String, ansettelsesform: Ansettelsesform) =
        UtenlandskVirksomhetMedAnsettelsesform(
            navn = navn,
            organisasjonsnummer = null,
            vegnavnOgHusnummer = "Testvei 1",
            bygning = null,
            postkode = "0000",
            byStedsnavn = "Testby",
            region = null,
            land = land,
            tilhorerSammeKonsern = false,
            ansettelsesform = ansettelsesform
        )

    private fun utenlandskBase(navn: String, orgnr: String, land: String) = UtenlandskVirksomhet(
        navn = navn,
        organisasjonsnummer = orgnr,
        vegnavnOgHusnummer = "Hauptstraße 1",
        bygning = null,
        postkode = "10115",
        byStedsnavn = "Berlin",
        region = null,
        land = land,
        tilhorerSammeKonsern = false
    )

    private fun UtenlandskVirksomhet.medAnsettelsesform(ansettelsesform: Ansettelsesform) =
        UtenlandskVirksomhetMedAnsettelsesform(
            navn = navn,
            organisasjonsnummer = organisasjonsnummer,
            vegnavnOgHusnummer = vegnavnOgHusnummer,
            bygning = bygning,
            postkode = postkode,
            byStedsnavn = byStedsnavn,
            region = region,
            land = land,
            tilhorerSammeKonsern = tilhorerSammeKonsern,
            ansettelsesform = ansettelsesform
        )
}
