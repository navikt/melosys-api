package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.FakeUnleash
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.brev.InnvilgelseEftaStorbritanniaBrevbestilling
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate


@ExtendWith(MockKExtension::class)
internal class InnvilgelseEftaStorbritanniaMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockVilkaarsresultatService: VilkaarsresultatService

    @MockK
    private lateinit var mockVirksomheterService: AvklarteVirksomheterService

    @MockK
    private lateinit var mockAvklartefaktaService: AvklartefaktaService

    @MockK
    private lateinit var mockLandvelgerService: LandvelgerService

    private lateinit var innvilgelseEftaStorbritanniaMapper: InnvilgelseEftaStorbritanniaMapper

    private val unleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        unleash.enableAll()
        innvilgelseEftaStorbritanniaMapper = InnvilgelseEftaStorbritanniaMapper(
            mockVilkaarsresultatService,
            mockDokgenMapperDatahenter,
            mockVirksomheterService,
            mockAvklartefaktaService,
            mockLandvelgerService,
            unleash
        )
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling, arbeid kun norge`() {
        val behandling = lagBehandling(behandlingstype = Behandlingstyper.FØRSTEGANG)
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsresultat(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, behandling)
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("NO")


        val brevbestilling =
            InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
                .medBehandling(behandling)
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"

                })
                .medPersonMottaker(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land.av("NOR")
                    }
                })
                .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            navnVirksomheter.shouldBe(listOf("Bedrift AS", "Bedrift Utenlandsk AS"))
            behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
            erArtikkel11_3_a_eller_13_3_a_arbeid_norge?.shouldBeTrue()
            erArtikkel13_3_a_eller_13_4?.shouldBeFalse()
            erArtikkel14_1_eller_14_2?.shouldBeFalse()
            erArtikkel16_1_eller_16_3?.shouldBeFalse()
            erArtikkel18_1?.shouldBeFalse()
            bosted.shouldBe("Norge")
            lovvalgsbestemmelse.shouldBe(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A.name)
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling`() {
        val behandling = lagBehandling(behandlingstype = Behandlingstyper.FØRSTEGANG)
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsresultat(
            Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1,
            behandling
        )
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("GB")


        val brevbestilling =
            InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
                .medBehandling(behandling)
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"

                })
                .medPersonMottaker(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land.av("GBR")
                    }
                })
                .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            navnVirksomheter.shouldBe(listOf("Bedrift AS", "Bedrift Utenlandsk AS"))
            behandlingstype.shouldBe(Behandlingstyper.FØRSTEGANG)
            erArtikkel13_3_a_eller_13_4?.shouldBeFalse()
            erArtikkel14_1_eller_14_2?.shouldBeFalse()
            erArtikkel16_1_eller_16_3?.shouldBeFalse()
            erArtikkel18_1?.shouldBeTrue()
            bosted.shouldBe("Storbritannia")
            lovvalgsbestemmelse.shouldBe(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1.name)
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling - flere arbeidsland med Artikkel 11_3_a`() {
        every { mockVilkaarsresultatService.harVilkaar(ofType(), listOf(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)) } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true
        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("SE")

        val behandling = lagBehandling(behandlingstype = Behandlingstyper.FØRSTEGANG)
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsresultat(
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
            behandling
        )

        val brevbestilling = InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
            .medBehandling(behandling)
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Test Person"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Test Person"
                bostedsadresse = Bostedsadresse().apply {
                    land = Land.av("SWE")
                }
            })
            .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            erArtikkel11_3_a_og_flereArbeidsland!!.shouldBeTrue()
            sedAvsenderland shouldBe "Sverige"
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling - ett arbeidsland med Artikkel 11_3_a`() {
        every { mockVilkaarsresultatService.harVilkaar(ofType(), listOf(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)) } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true
        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("SE")

        val behandling = lagBehandling(behandlingstype = Behandlingstyper.FØRSTEGANG, landkoder = listOf(Landkoder.SE.kode))
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsresultat(
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
            behandling
        )

        val brevbestilling = InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
            .medBehandling(behandling)
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Test Person"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Test Person"
                bostedsadresse = Bostedsadresse().apply {
                    land = Land.av("SWE")
                }
            })
            .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            erArtikkel11_3_a_og_flereArbeidsland!!.shouldBeFalse()
            sedAvsenderland shouldBe "Sverige"
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling - uten SED dokument`() {
        every { mockVilkaarsresultatService.harVilkaar(ofType(), listOf(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)) } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true
        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("SE")

        val behandling = lagBehandling(behandlingstype = Behandlingstyper.FØRSTEGANG, inkluderSed = false, landkoder = listOf(Landkoder.SE.kode))
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsresultat(
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
            behandling
        )

        val brevbestilling = InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
            .medBehandling(behandling)
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Test Person"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Test Person"
                bostedsadresse = Bostedsadresse().apply {
                    land = Land.av("SWE")
                }
            })
            .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            erArtikkel11_3_a_og_flereArbeidsland!!.shouldBeFalse()
            sedAvsenderland shouldBe null
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling - er11_3_a_eller_13_a_arbeid_norge med arbeid i Norge og Norge utpekt`() {
        every { mockVilkaarsresultatService.harVilkaar(ofType(), listOf(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)) } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true
        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("NO")

        val behandling = lagBehandling(
            landkoder = listOf(Landkoder.NO.kode),
            behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE,
            behandlingstype = Behandlingstyper.FØRSTEGANG, )
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns lagBehandlingsresultat(
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
            behandling
        )

        val brevbestilling = InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
            .medBehandling(behandling)
            .medPersonDokument(PersonDokument().apply {
                sammensattNavn = "Test Person"
            })
            .medPersonMottaker(PersonDokument().apply {
                sammensattNavn = "Test Person"
                bostedsadresse = Bostedsadresse().apply {
                    land = Land.av("NOR")
                }
            })
            .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            erArtikkel11_3_a_eller_13_3_a_arbeid_norge!!.shouldBeTrue()
            erArtikkel11_3_a_og_flereArbeidsland!!.shouldBeFalse()
            bosted shouldBe "Norge"
        }
    }

    @Test
    fun `Innvilgelse efta Storbritannia brevbestilling, arbeid kun norge - Nye opplysninger som nyVurderingBakgrunn`() {
        val behandling = lagBehandling(behandlingstype = Behandlingstyper.NY_VURDERING, sakstyper = Sakstyper.EU_EOS)

        every {
            mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType())
        } returns lagBehandlingsresultat(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A, behandling, nyVurderingBakgrunn = "NYE_OPPLYSNINGER")
        every {
            mockVilkaarsresultatService.harVilkaar(
                ofType(), listOf(
                    Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
                )
            )
        } returns true
        every { mockVilkaarsresultatService.finnVilkaarsresultat(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns null
        every { mockVilkaarsresultatService.oppfyllerVilkaar(ofType(), Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP) } returns true

        every { mockVirksomheterService.hentAlleNorskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagNorskVirksomhet())
        every { mockVirksomheterService.hentUtenlandskeVirksomheter(ofType()) } returns listOf(BrevDataTestUtils.lagUtenlandskVirksomhet())
        every { mockLandvelgerService.hentBostedsland(ofType()) } returns Bostedsland("NO")


        val brevbestilling =
            InnvilgelseEftaStorbritanniaBrevbestilling.Builder()
                .medBehandling(behandling)
                .medPersonDokument(PersonDokument().apply {
                    sammensattNavn = "Hei Test"

                })
                .medPersonMottaker(PersonDokument().apply {
                    sammensattNavn = "Hei Test"
                    bostedsadresse = Bostedsadresse().apply {
                        land = Land.av("NOR")
                    }
                })
                .build()

        innvilgelseEftaStorbritanniaMapper.mapInnvilgelseEftaStorbritannia(brevbestilling).run {
            behandlingstype.shouldBe(Behandlingstyper.NY_VURDERING)
            erArtikkel11_3_a_eller_13_3_a_arbeid_norge?.shouldBeTrue()
            nyVurderingBakgrunn.shouldBe("NYE_OPPLYSNINGER")
        }
    }

    private fun lagBehandling(
        landkoder: List<String> = listOf(Landkoder.SE.kode, Landkoder.FR.kode),
        inkluderSed: Boolean = true,
        behandlingstema: Behandlingstema = Behandlingstema.YRKESAKTIV,
        behandlingstype: Behandlingstyper? = null,
        sakstyper: Sakstyper = Sakstyper.FTRL,
    ): Behandling = Behandling.buildWithDefaults().apply {
        id = 1L
        val behandling = this
        fagsak = FagsakTestFactory.builder().apply {
            type = sakstyper
            leggTilBehandling(behandling)
        }.build()
        type = behandlingstype!!
        tema = behandlingstema

        mottatteOpplysninger = MottatteOpplysninger().apply {
            this.behandling = behandling
            mottatteOpplysningerData = MottatteOpplysningerData().apply {
                soeknadsland = Soeknadsland().apply {
                    this.landkoder = landkoder
                }
                periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))
            }
        }

        if (inkluderSed) {
            val saksopplysning = Saksopplysning().apply {
                this.behandling = behandling
                dokument = SedDokument().apply {
                    avsenderLandkode = Landkoder.SE
                }
                type = SaksopplysningType.SEDOPPL
            }
            saksopplysninger = mutableSetOf(saksopplysning)
        }
    }

    private fun lagBehandlingsresultat(
        lovvalgsbestemmelse: LovvalgBestemmelse,
        behandling: Behandling? = null,
        nyVurderingBakgrunn: String? = null
    ): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = 1L
            this.behandling = behandling ?: lagBehandling()
            avklartefakta = setOf(Avklartefakta().apply {
                fakta = AvklartYrkesgruppeType.ORDINAER.name
                type = Avklartefaktatyper.YRKESGRUPPE
            })
            lovvalgsperioder = setOf(no.nav.melosys.domain.Lovvalgsperiode().apply {
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                lovvalgsland = Land_iso2.NO
                bestemmelse = lovvalgsbestemmelse
            })

            this.nyVurderingBakgrunn = nyVurderingBakgrunn
        }
    }

}
