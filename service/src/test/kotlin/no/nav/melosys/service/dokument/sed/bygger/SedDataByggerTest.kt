package no.nav.melosys.service.dokument.sed.bygger

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.*
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.person.Diskresjonskode
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse
import no.nav.melosys.domain.dokument.person.adresse.Gateadresse
import no.nav.melosys.domain.eessi.sed.Adresse.Companion.IKKE_TILGJENGELIG
import no.nav.melosys.domain.eessi.sed.Adresse.Companion.INGEN_FAST_ADRESSE
import no.nav.melosys.domain.eessi.sed.Adresse.Companion.UKJENT
import no.nav.melosys.domain.eessi.sed.SedDataDto
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagMedSoknad
import no.nav.melosys.service.dokument.sed.datagrunnlag.SedDataGrunnlagUtenSoknad
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
class SedDataByggerTest {

    @MockK(relaxed = true)
    private lateinit var kodeverkService: KodeverkService

    @MockK(relaxed = true)
    private lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockK(relaxed = true)
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK(relaxed = true)
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockK(relaxed = true)
    private lateinit var landvelgerService: LandvelgerService

    @MockK(relaxed = true)
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK(relaxed = true)
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private lateinit var dataBygger: SedDataBygger
    private val fakeUnleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns DataByggerStubs.hentOrganisasjonDokumentSetStub()

        val tidligereLovvalgsperiode = lovvalgsperiodeForTest {
            fom = LocalDate.now()
            tom = LocalDate.now().plusMonths(2L)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
        }
        every { lovvalgsperiodeService.hentTidligereLovvalgsperioder(any()) } returns listOf(tidligereLovvalgsperiode)

        dataBygger = SedDataBygger(
            behandlingsresultatService,
            landvelgerService,
            lovvalgsperiodeService,
            saksbehandlingRegler,
            fakeUnleash
        )
    }

    /**
     * Creates a standard test behandlingsresultat with lovvalgsperiode, anmodningsperiode, and utpekingsperiode.
     * Each test gets its own fresh copy.
     */
    private fun lagStandardBehandlingsresultat(
        behandling: Behandling = DataByggerStubs.hentBehandlingStub(),
        lovvalgsperiodeInit: LovvalgsperiodeTestFactory.Builder.() -> Unit = {},
        anmodningsperiodeInit: AnmodningsperiodeTestFactory.Builder.() -> Unit = {},
        utpekingsperiodeInit: UtpekingsperiodeTestFactory.Builder.() -> Unit = {}
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        id = 0L
        this.behandling = behandling

        vilkaarsresultat {
            begrunnelse("SOEKT_FOR_SENT")
        }

        lovvalgsperiode {
            lovvalgsland = Land_iso2.NO
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(1L)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsperiodeInit()
        }

        anmodningsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(2)
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1
            unntakFraLovvalgsland = Land_iso2.SE
            unntakFraBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            dekning = Trygdedekninger.FULL_DEKNING_EOSFO
            anmodningsperiodeInit()
        }

        utpekingsperiode {
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(3)
            lovvalgsland = Land_iso2.DK
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3
            tilleggsbestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4
            utpekingsperiodeInit()
        }
    }

    private fun lagSedData(
        periodeType: PeriodeType = PeriodeType.LOVVALGSPERIODE,
        behandlingsresultat: Behandlingsresultat = lagStandardBehandlingsresultat(),
        customizePersonDokument: (PersonDokument.() -> Unit)? = null
    ): SedDataDto {
        val grunnlag = if (customizePersonDokument != null) {
            lagGrunnlagMedSøknad(behandlingsresultat.behandling!!, customizePersonDokument)
        } else {
            lagGrunnlagMedSøknad(behandlingsresultat.behandling!!)
        }
        return dataBygger.lag(grunnlag, behandlingsresultat, periodeType)
    }

    private fun lagSedDataUtkast(
        periodeType: PeriodeType = PeriodeType.LOVVALGSPERIODE,
        behandlingsresultat: Behandlingsresultat = lagStandardBehandlingsresultat(),
        customizePersonDokument: (PersonDokument.() -> Unit)? = null
    ): SedDataDto {
        val grunnlag = if (customizePersonDokument != null) {
            lagGrunnlagMedSøknad(behandlingsresultat.behandling!!, customizePersonDokument)
        } else {
            lagGrunnlagMedSøknad(behandlingsresultat.behandling!!)
        }
        return dataBygger.lagUtkast(grunnlag, behandlingsresultat, periodeType)
    }

    private fun lagGrunnlagMedSøknad(
        behandling: Behandling = DataByggerStubs.hentBehandlingStub(),
        persondata: Persondata = DataByggerStubs.lagPersonDokument()
    ): SedDataGrunnlagMedSoknad {
        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            mockk<BehandlingService>(),
            kodeverkService
        )
        return SedDataGrunnlagMedSoknad(
            behandling,
            kodeverkService,
            avklarteVirksomheterService,
            avklartefaktaService,
            persondata
        )
    }

    private fun lagGrunnlagMedSøknad(
        behandling: Behandling,
        customizePersonDokument: PersonDokument.() -> Unit
    ): SedDataGrunnlagMedSoknad {
        val personDokument = DataByggerStubs.lagPersonDokument().apply(customizePersonDokument)
        return lagGrunnlagMedSøknad(behandling, personDokument)
    }

    private fun lagGrunnlagUtenSøknad(
        behandling: Behandling = DataByggerStubs.hentBehandlingStub(),
        persondata: Persondata = DataByggerStubs.lagPersonDokument()
    ): SedDataGrunnlagUtenSoknad {
        return SedDataGrunnlagUtenSoknad(behandling, kodeverkService, persondata)
    }

    private fun lagGrunnlagMedManglendeAdressefelter(
        arbeidsstedManglerLandkode: Boolean,
        arbeidsgivendeForetakUtlandManglerLandkode: Boolean,
        selvstendigForetakUtlandManglerLandkode: Boolean,
        persondata: Persondata = DataByggerStubs.lagPersonDokument()
    ): SedDataGrunnlagMedSoknad {
        val avklarteVirksomheterService = AvklarteVirksomheterService(
            avklartefaktaService,
            organisasjonOppslagService,
            mockk<BehandlingService>(),
            kodeverkService
        )
        return SedDataGrunnlagMedSoknad(
            DataByggerStubs.hentBehandlingMedManglendeAdressefelterStub(
                arbeidsstedManglerLandkode,
                arbeidsgivendeForetakUtlandManglerLandkode,
                selvstendigForetakUtlandManglerLandkode
            ),
            kodeverkService,
            avklarteVirksomheterService,
            avklartefaktaService,
            persondata
        )
    }

    @Test
    fun `lag medlemsperiodeTypeLovvalgsperiodeMedSøknad forventLovvalgsperiodeBrukt`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val lovvalgsperiode = behandlingsresultat.lovvalgsperioder.first()

        val sedData = lagSedData(behandlingsresultat = behandlingsresultat)

        sedData.shouldNotBeNull().run {
            arbeidsgivendeVirksomheter.shouldNotBeEmpty()
            arbeidssteder.shouldNotBeNull()
            bruker.shouldNotBeNull()
            bostedsadresse.shouldNotBeNull()
            familieMedlem.shouldNotBeNull()
            selvstendigeVirksomheter.shouldNotBeNull()
            utenlandskIdent.shouldNotBeNull()

            lovvalgsperioder.shouldHaveSize(1).single().run {
                fom shouldBe lovvalgsperiode.fom
                tom shouldBe lovvalgsperiode.tom
                lovvalgsland shouldBe lovvalgsperiode.lovvalgsland.shouldNotBeNull().kode
            }

            arbeidsgivendeVirksomheter.shouldNotBeEmpty()
        }
    }

    @Test
    fun `lag storbritanniaAnmodningsperiode forventKorrektMapping`() {
        val behandlingsresultat = lagStandardBehandlingsresultat(
            anmodningsperiodeInit = {
                bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
                unntakFraBestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4
                tilleggsbestemmelse = Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1
            }
        )

        val sedData = lagSedData(PeriodeType.ANMODNINGSPERIODE, behandlingsresultat)

        sedData.shouldNotBeNull().run {
            lovvalgsperioder.shouldNotBeEmpty().first().run {
                bestemmelse shouldBe no.nav.melosys.domain.eessi.sed.Bestemmelse.ART_16_1
                unntakFraBestemmelse shouldBe no.nav.melosys.domain.eessi.sed.Bestemmelse.ART_11_4
                tilleggsBestemmelse shouldBe no.nav.melosys.domain.eessi.sed.Bestemmelse.ART_11_4
            }
        }
    }

    @Test
    fun `lag medlemsperiodeTypeAnmodningsperiodeMedSøknad forventAnmodningsperiode`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val anmodningsperiode = behandlingsresultat.anmodningsperioder.first()

        val sedData = lagSedData(PeriodeType.ANMODNINGSPERIODE, behandlingsresultat)

        sedData.shouldNotBeNull().run {
            lovvalgsperioder.shouldNotBeEmpty().first().run {
                fom shouldBe anmodningsperiode.fom
                tom shouldBe anmodningsperiode.tom
                lovvalgsland shouldBe anmodningsperiode.lovvalgsland.shouldNotBeNull().kode
            }
        }
    }

    @Test
    fun `lag medlemsperiodeTypeUtpekingsperiodeMedSøknad forventUtpekingsperiode`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val utpekingsperiode = behandlingsresultat.utpekingsperioder.first()

        val sedData = lagSedData(PeriodeType.UTPEKINGSPERIODE, behandlingsresultat)

        sedData.shouldNotBeNull().run {
            lovvalgsperioder.shouldNotBeEmpty().first().run {
                fom shouldBe utpekingsperiode.fom
                tom shouldBe utpekingsperiode.tom
                lovvalgsland shouldBe utpekingsperiode.lovvalgsland.shouldNotBeNull().kode
            }
        }
    }

    @Test
    fun `lag medlemsperiodeTypeAnmodningsperiodeUtenSøknad forventAnmodningsperiode`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val anmodningsperiode = behandlingsresultat.anmodningsperioder.first()

        val sedData = dataBygger.lag(lagGrunnlagUtenSøknad(behandling), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE)

        sedData.shouldNotBeNull().run {
            lovvalgsperioder.shouldNotBeEmpty().first().run {
                fom shouldBe anmodningsperiode.fom
            }
        }
    }

    @Test
    fun `lag medlemsperiodeTypeIngenMedSøknad forventAnmodningsperiode`() {
        val sedData = lagSedData(PeriodeType.INGEN)

        sedData.shouldNotBeNull().run {
            lovvalgsperioder.shouldBeEmpty()
        }
    }

    @Test
    fun `lag bostedsadresseUtenGateadresse gatenavnBlirNA`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedData(behandlingsresultat = behandlingsresultat) {
            this.bostedsadresse = Bostedsadresse(
                land = Land(Land.SVERIGE)
            )
        }

        sedData.bostedsadresse.shouldNotBeNull()
            .gateadresse shouldBe IKKE_TILGJENGELIG
    }

    @Test
    fun `lag bostedsadresseUtenGatenavn gatenavnBlirNA`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedData(behandlingsresultat = behandlingsresultat) {
            bostedsadresse = Bostedsadresse(
                gateadresse = Gateadresse(
                    gatenavn = "",
                    husnummer = 123
                ),
                land = Land(Land.SVERIGE)
            )
        }

        sedData.bostedsadresse.shouldNotBeNull()
            .gateadresse shouldBe IKKE_TILGJENGELIG
    }

    @Test
    fun `lag bostedsadresseMedBlanktGatenavn gatenavnBlirNA`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedData(behandlingsresultat = behandlingsresultat) {
            bostedsadresse = Bostedsadresse(
                gateadresse = Gateadresse(
                    gatenavn = " ",
                    husnummer = 123
                ),
                land = Land(Land.SVERIGE)
            )
        }

        sedData.bostedsadresse.shouldNotBeNull()
            .gateadresse shouldBe IKKE_TILGJENGELIG
    }

    @Test
    fun `lag bostedsadresseMedGatenavnOgHusnummer rettFormatertGateadresse`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedData(behandlingsresultat = behandlingsresultat) {
            this.bostedsadresse = Bostedsadresse(
                gateadresse = Gateadresse(
                    gatenavn = "gate",
                    husnummer = 123
                ),
                land = Land(Land.SVERIGE)
            )
        }

        sedData.bostedsadresse.shouldNotBeNull()
            .gateadresse shouldBe "gate 123"
    }

    @Test
    fun `lag medKontaktadresse kontadresseMappes`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val persondataMedKontakadresse = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(behandling, persondataMedKontakadresse)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.kontaktadresse.shouldNotBeNull()
            .gateadresse shouldBe "gatenavnKontaktadresseFreg"
    }

    @Test
    fun `lag utenKontaktadresse kontadresseErNull`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenKontaktadresse()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(behandling, persondata)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.kontaktadresse.shouldBeNull()
    }

    @Test
    fun `lag medOppholdsadresse oppholdsadresseMappes`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val persondataMedOppholdsadresse = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(behandling, persondataMedOppholdsadresse)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.oppholdsadresse.shouldNotBeNull()
            .gateadresse shouldBe "gatenavnOppholdsadresseFreg"
    }

    @Test
    fun `lag utenOppholdsadresse oppholdsadresseErNull`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresse()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(behandling, persondata)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.oppholdsadresse.shouldBeNull()
    }

    @Test
    fun `lag medMaritimtArbeid gatenavnBlirNA`() {
        every { avklartefaktaService.hentMaritimeAvklartfaktaEtterSubjekt(any()) } returns mutableMapOf(
            "enhet" to AvklartMaritimtArbeid(
                "navn",
                listOf(Avklartefakta().apply {
                    this.fakta = "SE"
                    this.type = Avklartefaktatyper.ARBEIDSLAND
                })
            )
        )

        val sedData = lagSedData()

        sedData.arbeidssteder.map { it.adresse.shouldNotBeNull().gateadresse } shouldContain IKKE_TILGJENGELIG
    }

    @Test
    fun `lag brukerErKode6 forventHarSensitiveOpplysninger`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedDataUtkast(behandlingsresultat = behandlingsresultat) {
            diskresjonskode = Diskresjonskode().apply {
                kode = "SPSF"
            }
        }

        sedData.shouldNotBeNull().bruker.shouldNotBeNull()
            .harSensitiveOpplysninger shouldBe true
    }

    @Test
    fun `lag brukerHarKode7 forventHarIkkeSensitiveOpplysninger`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedDataUtkast(behandlingsresultat = behandlingsresultat) {
            diskresjonskode = Diskresjonskode().apply {
                kode = "SPSO"
            }
        }

        sedData.shouldNotBeNull().bruker.shouldNotBeNull()
            .harSensitiveOpplysninger shouldBe false
    }

    @Test
    fun `lag brukerHarIngenDiskresjonskode forventHarIkkeSensitiveOpplysninger`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = lagSedDataUtkast(behandlingsresultat = behandlingsresultat) {
            diskresjonskode = null
        }

        sedData.shouldNotBeNull().bruker.shouldNotBeNull()
            .harSensitiveOpplysninger shouldBe false
    }

    @Test
    fun `lagUtkast medlemsperiodeTypeIngenMedSøknad forventAnmodningsperiode`() {
        val sedData = lagSedDataUtkast(PeriodeType.ANMODNINGSPERIODE)

        sedData.shouldNotBeNull().run {
            lovvalgsperioder.shouldNotBeEmpty()
            lovvalgsperioder[0].unntakFraLovvalgsland.shouldNotBeEmpty()
        }
    }

    @Test
    fun `lagUtkast medlemsperiodeTypeIngenMedSøknad utenLovvalgsperioder`() {
        val sedData = lagSedDataUtkast(PeriodeType.INGEN)

        lagUtkastAssertions(sedData, true)
        sedData.lovvalgsperioder.shouldBeEmpty()
    }

    @Test
    fun `lagUtkast medlemsperiodeTypeIngenUtenSøknad utenLovvalgsperioder`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)

        val sedData = dataBygger.lagUtkast(lagGrunnlagUtenSøknad(behandling), behandlingsresultat, PeriodeType.INGEN)

        sedData.run {
            bruker.shouldNotBeNull()
            bostedsadresse.shouldNotBeNull()
            lovvalgsperioder.shouldBeEmpty()
        }
    }

    @Test
    fun `lagUtkast medlemsperiodeTypeLovvalgsperiodeMedSøknad medLovvalgsperioder`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val lovvalgsperiode = behandlingsresultat.lovvalgsperioder.first()

        val sedData = lagSedDataUtkast(behandlingsresultat = behandlingsresultat)

        lagUtkastAssertions(sedData, true)
        sedData.run {
            lovvalgsperioder.shouldNotBeEmpty()
            lovvalgsperioder[0].fom shouldBe lovvalgsperiode.fom
        }
    }

    @Test
    fun `lagUtkast harIkkeFastArbeidsstedForArbeidsland arbeidsstedBlirSatt`() {
        every { landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(any()) } returns listOf(Land_iso2.SE)
        val sedData = lagSedData()

        sedData.run {
            arbeidssteder.shouldHaveSize(2).last().run {
                navn shouldBe INGEN_FAST_ADRESSE
                adresse.shouldNotBeNull().poststed shouldBe INGEN_FAST_ADRESSE
            }
        }
    }

    @Test
    fun `lagUtkast medLuftfartBase arbeidsstedBlirSatt`() {
        val luftfartBase = LuftfartBase(
            hjemmebaseNavn = "hjemmebaseNavn",
            hjemmebaseLand = "GB"
        )
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)

        val dataGrunnlag = lagGrunnlagMedSøknad(behandling)
        dataGrunnlag.mottatteOpplysningerData.luftfartBaser = listOf(luftfartBase)
        val sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.run {
            arbeidssteder.shouldHaveSize(2).last().run {
                navn shouldBe luftfartBase.hjemmebaseNavn
                adresse.shouldNotBeNull().run {
                    gateadresse shouldBe "N/A"
                    land shouldBe luftfartBase.hjemmebaseLand
                }
            }
        }
    }

    @Test
    fun `lagUtkast medUtenlandskSelvstendigForetak forventAtUtenlandskSelvstendigForetakIkkeSendesSomArbeidsgivendeVirksomhet`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val dataGrunnlag = lagGrunnlagMedSøknad(behandling).apply {
            mottatteOpplysningerData.foretakUtland = listOf(ForetakUtland().apply {
                adresse = StrukturertAdresse()
                adresse.landkode = Landkoder.DE.kode
                selvstendigNæringsvirksomhet = true
                navn = "selvstendig"
                uuid = "123"
            })
        }

        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf("123")

        val sedData = dataBygger.lag(dataGrunnlag, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.run {
            selvstendigeVirksomheter.map { it.navn } shouldContain "selvstendig"
            arbeidsgivendeVirksomheter.map { it.navn } shouldNotContain "selvstendig"
        }
    }

    @Test
    fun `lagVedtakDto ikkeOpprinneligVedtakMedDagensDato setterDatoOgVariablerISed`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val avsluttetBehandling = DataByggerStubs.hentBehandlingStub().apply {
            status = Behandlingsstatus.AVSLUTTET
            id = 2L
        }

        val behandlingsresultatMedVedtak = Behandlingsresultat.forTest {
            id = 1L
            this.behandling = avsluttetBehandling
            vedtakMetadata {
                vedtaksdato = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            }
        }

        // Link both behandlinger to the same fagsak
        behandling.fagsak.leggTilBehandling(behandling)
        behandling.fagsak.leggTilBehandling(avsluttetBehandling)

        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedVedtak

        val sedDataDto = lagSedData(behandlingsresultat = behandlingsresultat)
        sedDataDto.shouldNotBeNull().vedtakDto.shouldNotBeNull().run {
            erFørstegangsvedtak shouldBe false
            datoForrigeVedtak shouldBe LocalDate.now()
        }
    }

    @Test
    fun `lagVedtakDto midlertidigLovvalgsbestemt settesSomIkkeFørstegangsvedtak`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val avsluttetBehandling = DataByggerStubs.hentBehandlingStub().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            id = 2L
        }

        val behandlingsresultatMedVedtak = Behandlingsresultat.forTest {
            this.behandling = avsluttetBehandling
            vedtakMetadata {
                vedtaksdato = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            }
        }

        // Link both behandlinger to the same fagsak
        behandling.fagsak.leggTilBehandling(behandling)
        behandling.fagsak.leggTilBehandling(avsluttetBehandling)

        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedVedtak

        val sedDataDto = lagSedData(behandlingsresultat = behandlingsresultat)
        sedDataDto.shouldNotBeNull().vedtakDto.shouldNotBeNull().run {
            erFørstegangsvedtak shouldBe false
            datoForrigeVedtak shouldBe LocalDate.now()
        }
    }

    @Test
    fun `lag arbeidsstedManglerLandkode kasterFeil`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(true, false, false)
        shouldThrow<FunksjonellException> {
            dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)
        }.message.shouldNotBeNull() shouldContain "land er ikke utfylt for arbeidssted"
    }

    @Test
    fun `lag arbeidsgivendeVirksomhetManglerLandkode kasterFeil`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf("uuid")
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(false, true, false)
        shouldThrow<FunksjonellException> {
            dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)
        }.message.shouldNotBeNull() shouldContain "land er ikke utfylt for virksomhet"
    }

    @Test
    fun `lag selvstendigVirksomhetManglerLandkode kasterFeil`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf("uuid")
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(false, false, true)
        shouldThrow<FunksjonellException> {
            dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)
        }.message.shouldNotBeNull() shouldContain "land er ikke utfylt for selvstendig virksomhet"
    }

    @Test
    fun `lagArbeidssted manglerObligatoriskeFelter blirUnknown`() {
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = dataBygger.lag(
            lagGrunnlagMedManglendeAdressefelter(false, false, false),
            behandlingsresultat,
            PeriodeType.LOVVALGSPERIODE
        )

        sedData.arbeidssteder.map { it.adresse.shouldNotBeNull().poststed } shouldContain UKJENT
    }

    @Test
    fun `lagVirksomhet manglerObligatoriskeFelter blirUnknown`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf("uuid")
        val behandlingsresultat = lagStandardBehandlingsresultat()
        val sedData = dataBygger.lag(
            lagGrunnlagMedManglendeAdressefelter(false, false, false),
            behandlingsresultat,
            PeriodeType.LOVVALGSPERIODE
        )

        val virksomheterMedUkjentOrgnr = sedData.arbeidsgivendeVirksomheter.filter { it.orgnr == UKJENT }
        virksomheterMedUkjentOrgnr.map { it.adresse.shouldNotBeNull().poststed } shouldContain UKJENT
    }

    @Test
    fun `lagVirksomhet harObligatoriskeFelter blirSatt`() {
        val sedData = lagSedData()

        sedData.arbeidsgivendeVirksomheter.map { it.orgnr } shouldContain "orgnr"
    }

    @Test
    fun `lag harFlytErEøsErIkkeSed søknadsperiodeBlirSatt`() {
        val behandling = DataByggerStubs.hentBehandlingStub().apply {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            fagsak = Fagsak.forTest()
        }
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val søknad = behandling.mottatteOpplysninger.shouldNotBeNull().mottatteOpplysningerData

        val sedData = lagSedData(behandlingsresultat = behandlingsresultat)

        sedData.søknadsperiode.shouldNotBeNull().run {
            fom shouldBe søknad.periode.fom
            tom shouldBe søknad.periode.tom
        }
    }

    @Test
    fun `lag erIkkeEuEøs søknadsperiodeBlirIkkeSatt`() {
        val behandling = DataByggerStubs.hentBehandlingStub().apply {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            fagsak = Fagsak.forTest {
                type = Sakstyper.TRYGDEAVTALE
            }
        }
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)

        val sedData = lagSedData(behandlingsresultat = behandlingsresultat)

        sedData.søknadsperiode.shouldBeNull()
    }

    @Test
    fun `lag erSed søknadsperiodeBlirIkkeSatt`() {
        val behandling = DataByggerStubs.hentBehandlingStub().apply {
            tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            type = Behandlingstyper.FØRSTEGANG
            fagsak = Fagsak.forTest()
        }
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)

        val sedData = lagSedData(behandlingsresultat = behandlingsresultat)

        sedData.søknadsperiode.shouldBeNull()
    }

    @Test
    fun `lag harIkkeFlyt søknadsperiodeBlirIkkeSatt`() {
        val behandling = DataByggerStubs.hentBehandlingStub().apply {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.HENVENDELSE
            fagsak = Fagsak.forTest()
        }
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true

        val sedData = lagSedData(behandlingsresultat = behandlingsresultat)

        sedData.søknadsperiode.shouldBeNull()
    }

    @Test
    fun `lag medFlereStatsborgerskap alleStatsborgerSkapMappes`() {
        val behandling = DataByggerStubs.hentBehandlingStub()
        val behandlingsresultat = lagStandardBehandlingsresultat(behandling)
        val personDataFraPDL = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(behandling, personDataFraPDL)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.bruker.shouldNotBeNull().statsborgerskap.shouldNotBeNull().shouldHaveSize(3).let { list ->
            list shouldContain "NOR"
            list shouldContain "SWE"
            list shouldContain "DNK"
        }
    }

    @Test
    fun `lag behandlingMedUnntaksflytTema henterIkkeArbeidslandUtenMarginaltArbeid`() {
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true

        lagSedData()

        verify(exactly = 1) { landvelgerService.hentBostedsland(any(), any<MottatteOpplysningerData>()) }
        verify(exactly = 0) { landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(any()) }
    }

    private fun lagUtkastAssertions(sedData: SedDataDto, forventAdresse: Boolean) {
        sedData.shouldNotBeNull().run {
            arbeidsgivendeVirksomheter.shouldNotBeEmpty()
            arbeidssteder.shouldNotBeEmpty()
            bruker.shouldNotBeNull()
            if (forventAdresse) {
                bostedsadresse.shouldNotBeNull()
            }
            familieMedlem.shouldNotBeEmpty()
            utenlandskIdent.shouldNotBeEmpty()
            selvstendigeVirksomheter.shouldNotBeEmpty()
            tidligereLovvalgsperioder.shouldNotBeNull()
            arbeidsgivendeVirksomheter.shouldNotBeEmpty()
        }
    }
}
