package no.nav.melosys.saksflyt

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.anmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.soeknad
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class DSLTest {

    @Test
    fun `Fagsak med behandlinger`() {
        val fagsak = Fagsak.forTest {
            tema = Sakstemaer.TRYGDEAVGIFT
            behandling {
                id = 1L
            }
            behandling {
                id = 2L
            }
        }

        fagsak.run {
            saksnummer shouldBe FagsakTestFactory.SAKSNUMMER
            tema shouldBe Sakstemaer.TRYGDEAVGIFT
            behandlinger.shouldHaveSize(2).run {
                elementAt(0).also { behandling ->
                    behandling.id shouldBe 1L
                    behandling.fagsak shouldBe fagsak
                }
                elementAt(1).also { behandling ->
                    behandling.id shouldBe 2L
                    behandling.fagsak shouldBe fagsak
                }
            }
        }
    }

    @Test
    fun `Behandling med fagsak`() {
        val behandling = Behandling.forTest {
            fagsak {
                medBruker()
                medVirksomhet()
            }
        }

        behandling.run {
            fagsak.run {
                saksnummer shouldBe FagsakTestFactory.SAKSNUMMER
                aktører.shouldHaveSize(2).run {
                    elementAt(0).run {
                        aktørId shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
                        rolle.name shouldBe Aktoersroller.BRUKER.name
                    }
                    elementAt(1).run {
                        orgnr shouldBe FagsakTestFactory.ORGNR
                        rolle.name shouldBe Aktoersroller.VIRKSOMHET.name
                    }
                }
            }
        }
    }

    @Test
    fun `Prosessinstans med behandling og data`() {
        val prosessInstansID = UUID.fromString("da6a548b-59a8-4f19-9788-434254728307")
        val prosessinstans = Prosessinstans.forTest {
            id = prosessInstansID
            behandling {
                fagsak {
                    medBruker()
                }
            }
            medData(ProsessDataKey.SAKSBEHANDLER, "Z123456")
        }

        prosessinstans.run {
            id shouldBe prosessInstansID
            getData(ProsessDataKey.SAKSBEHANDLER) shouldBe "Z123456"
            hentBehandling.run {
                fagsak.run {
                    saksnummer shouldBe FagsakTestFactory.SAKSNUMMER
                    aktører shouldHaveSize 1
                    aktører.elementAt(0).run {
                        aktørId shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
                        rolle.name shouldBe Aktoersroller.BRUKER.name
                    }
                }
            }
        }
    }

    @Test
    fun `Behandling med mottatteOpplysninger og søknadsdata`() {
        val behandling = Behandling.forTest {
            fagsak {
                medBruker()
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            }
            mottatteOpplysninger {
                type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
                eksternReferanseID = "JOARK-12345"
                soeknad {
                    landkoder("BE", "NL")
                    fysiskeArbeidssted {
                        landkode = "BE"
                        poststed = "Brussel"
                        virksomhetNavn = "Acme Corp"
                    }
                    fysiskeArbeidssted {
                        landkode = "NL"
                        poststed = "Amsterdam"
                    }
                    bostedLandkode = "NO"
                    bostedPoststed = "Oslo"
                }
            }
        }

        behandling.run {
            fagsak.tema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
            mottatteOpplysninger.shouldNotBeNull().run {
                type shouldBe Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
                eksternReferanseID shouldBe "JOARK-12345"
                mottatteOpplysningerData.shouldNotBeNull().shouldBeInstanceOf<Soeknad>().run {
                    soeknadsland.run {
                        landkoder shouldHaveSize 2
                        landkoder[0] shouldBe "BE"
                        landkoder[1] shouldBe "NL"
                    }
                    arbeidPaaLand.fysiskeArbeidssteder.shouldHaveSize(2).run {
                        elementAt(0).run {
                            virksomhetNavn shouldBe "Acme Corp"
                            adresse.run {
                                landkode shouldBe "BE"
                                poststed shouldBe "Brussel"

                            }
                        }
                        elementAt(1).run {
                            adresse.run {
                                landkode shouldBe "NL"
                                poststed shouldBe "Amsterdam"

                            }
                        }
                    }
                    bosted.oppgittAdresse.landkode shouldBe "NO"
                    bosted.oppgittAdresse.poststed shouldBe "Oslo"
                }
            }
        }
    }

    @Test
    fun `Behandling med AnmodningEllerAttest data`() {
        val behandling = Behandling.forTest {
            fagsak {
                medBruker()
                type = Sakstyper.FTRL
            }
            mottatteOpplysninger {
                type = Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST
                anmodningEllerAttest {
                    avsenderland = Land_iso2.SE
                    lovvalgsland = Land_iso2.NO
                }
            }
        }

        behandling.run {
            fagsak.type shouldBe Sakstyper.FTRL
            mottatteOpplysninger.shouldNotBeNull().run {
                type shouldBe Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST
                mottatteOpplysningerData.shouldBeInstanceOf<AnmodningEllerAttest>().run {
                    avsenderland shouldBe Land_iso2.SE
                    lovvalgsland shouldBe Land_iso2.NO
                }
            }
        }
    }

    @Test
    fun `Behandlingsresultat med trygdeavgiftsperiode`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperiode {
                    grunnlagInntekstperiode {
                        type = Inntektskildetype.ARBEIDSINNTEKT
                        arbeidsgiversavgiftBetalesTilSkatt = false
                        avgiftspliktigMndInntekt = Penger(15000.0)
                    }
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                    trygdesats = 6.8.toBigDecimal()
                }
            }
        }

        behandlingsresultat.run {
            medlemskapsperioder.shouldHaveSize(1).single().run {
                fom shouldBe LocalDate.of(2023, 1, 1)
                tom shouldBe LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder.shouldHaveSize(1).single().run {
                    periodeFra shouldBe LocalDate.of(2023, 1, 1)
                    periodeTil shouldBe LocalDate.of(2023, 12, 31)
                    trygdesats shouldBe 6.8.toBigDecimal()
                    grunnlagInntekstperiode.shouldNotBeNull().run {
                        type shouldBe Inntektskildetype.ARBEIDSINNTEKT
                        isArbeidsgiversavgiftBetalesTilSkatt shouldBe false
                        avgiftspliktigMndInntekt.verdi shouldBe 15000.0.toBigDecimal()
                    }
                    grunnlagSkatteforholdTilNorge.shouldNotBeNull().run {
                        skatteplikttype shouldBe Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }
    }

    @Test
    fun `trygdeavgiftsperiode med defaults`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            grunnlagInntekstperiode {
            }
            grunnlagSkatteforholdTilNorge {
            }
        }

        trygdeavgiftsperiode.run {
            periodeFra shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_FRA
            periodeTil shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_TIL
            trygdesats shouldBe TrygdeavgiftsperiodeTestFactory.TRYGDESATS
            grunnlagInntekstperiode.shouldNotBeNull().run {
                fom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_FRA
                tom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_TIL
                type shouldBe TrygdeavgiftsperiodeTestFactory.INNTEKTSKILDETYPE
                isArbeidsgiversavgiftBetalesTilSkatt shouldBe TrygdeavgiftsperiodeTestFactory.ARBEIDSGIVERSAVGIFT_BETALES_TIL_SKATT
                avgiftspliktigMndInntekt.verdi shouldBe 15000.0.toBigDecimal()
            }
            grunnlagSkatteforholdTilNorge.shouldNotBeNull().run {
                fom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_FRA
                tom shouldBe TrygdeavgiftsperiodeTestFactory.PERIODE_TIL
                skatteplikttype shouldBe Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        }
    }

    @Test
    fun `trygdeavgiftsperiode med dsl scoping`() {
        // Denne testen demonstrerer at @MelosysTestDsl forhindrer scope leakage
        // Vi bør bruke fomDato/tomDato, ikke periodeFra/periodeTil fra ytre Builder
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.of(2024, 1, 1) // Dette er fra Builder
            periodeTil = LocalDate.of(2024, 12, 31)

            grunnlagInntekstperiode {
                // Vi kan IKKE aksessere periodeFra direkte her - det ville gitt kompileringsfeil
                // Dette bør ikke brukes, men demonstrerer hvordan scoping fungerer
                this@forTest.periodeFra = LocalDate.of(2023, 1, 1)
            }
            grunnlagSkatteforholdTilNorge {
                fomDato = LocalDate.of(2024, 1, 1)
            }
        }

        trygdeavgiftsperiode.run {
            periodeFra shouldBe LocalDate.of(2023, 1, 1)
            periodeTil shouldBe LocalDate.of(2024, 12, 31)
            grunnlagInntekstperiode.shouldNotBeNull().run {
                fom shouldBe LocalDate.of(2023, 1, 1)
                tom shouldBe LocalDate.of(2024, 12, 31)
            }
        }
    }

    @Test
    fun `Behandling med saksopplysninger`() {
        val behandling = Behandling.forTest {
            fagsak {
                medBruker()
            }
            saksopplysning {
                type = SaksopplysningType.PDL_PERSOPL
                personDokument {
                    fnr = "12345678901"
                    fornavn = "Test"
                    etternavn = "Testesen"
                    fødselsdato = LocalDate.of(1985, 6, 15)
                }
            }
            saksopplysning {
                type = SaksopplysningType.ORG
                organisasjonDokument {
                    orgnummer = "987654321"
                    navn = "Acme AS"
                }
            }
        }

        behandling.run {
            saksopplysninger.shouldHaveSize(2)
            saksopplysninger.forEach { saksopplysning ->
                saksopplysning.behandling shouldBe behandling
            }

            saksopplysninger.find { it.type == SaksopplysningType.PDL_PERSOPL }
                .shouldNotBeNull()
                .run {
                    versjon shouldBe "1.0"
                    dokument.shouldBeInstanceOf<PersonDokument>().run {
                        fnr shouldBe "12345678901"
                        fornavn shouldBe "Test"
                        etternavn shouldBe "Testesen"
                        fødselsdato shouldBe LocalDate.of(1985, 6, 15)
                    }
                }

            saksopplysninger.find { it.type == SaksopplysningType.ORG }
                .shouldNotBeNull()
                .run {
                    dokument.shouldBeInstanceOf<OrganisasjonDokument>().run {
                        orgnummer shouldBe "987654321"
                        navn shouldBe "Acme AS"
                    }
                }
        }
    }

    @Test
    fun `Saksopplysning med PersonDokument`() {
        val saksopplysning = saksopplysningForTest {
            type = SaksopplysningType.PDL_PERSOPL
            personDokument {
                fnr = "12345678901"
                fornavn = "Test"
                etternavn = "Testesen"
                fødselsdato = LocalDate.of(1985, 6, 15)
            }
        }

        saksopplysning.run {
            type shouldBe SaksopplysningType.PDL_PERSOPL
            versjon shouldBe "1.0"
            dokument.shouldBeInstanceOf<PersonDokument>().run {
                fnr shouldBe "12345678901"
                fornavn shouldBe "Test"
                etternavn shouldBe "Testesen"
                fødselsdato shouldBe LocalDate.of(1985, 6, 15)
            }
        }
    }

    @Test
    fun `Saksopplysning med OrganisasjonDokument`() {
        val saksopplysning = saksopplysningForTest {
            type = SaksopplysningType.ORG
            organisasjonDokument {
                orgnummer = "987654321"
                navn = "Acme AS"
                sektorkode = "2100"
            }
        }

        saksopplysning.run {
            type shouldBe SaksopplysningType.ORG
            versjon shouldBe "1.0"
            dokument.shouldBeInstanceOf<OrganisasjonDokument>().run {
                orgnummer shouldBe "987654321"
                navn shouldBe "Acme AS"
                sektorkode shouldBe "2100"
            }
        }
    }

    @Test
    fun `Behandlingsresultat med lovvalgsperioder`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandling {
                fagsak {
                    type = Sakstyper.EU_EOS
                }
            }
            lovvalgsperiode {
                fom = LocalDate.of(2024, 1, 1)
                tom = LocalDate.of(2024, 12, 31)
                lovvalgsland = Land_iso2.NO
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
            lovvalgsperiode {
                fom = LocalDate.of(2025, 1, 1)
                tom = LocalDate.of(2025, 12, 31)
                lovvalgsland = Land_iso2.SE
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }
        }

        behandlingsresultat.run {
            type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            lovvalgsperioder.shouldHaveSize(2)

            lovvalgsperioder.forEach { lovvalgsperiode ->
                lovvalgsperiode.behandlingsresultat shouldBe behandlingsresultat
            }

            lovvalgsperioder.find { it.lovvalgsland == Land_iso2.NO }
                .shouldNotBeNull()
                .run {
                    fom shouldBe LocalDate.of(2024, 1, 1)
                    tom shouldBe LocalDate.of(2024, 12, 31)
                    bestemmelse shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
                    innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
                }

            lovvalgsperioder.find { it.lovvalgsland == Land_iso2.SE }
                .shouldNotBeNull()
                .run {
                    fom shouldBe LocalDate.of(2025, 1, 1)
                    tom shouldBe LocalDate.of(2025, 12, 31)
                    bestemmelse shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
                    innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
                }
        }
    }
}
