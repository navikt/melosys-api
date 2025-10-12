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
    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var lovvalgsperiode: Lovvalgsperiode
    private lateinit var anmodningsperiode: Anmodningsperiode
    private lateinit var utpekingsperiode: Utpekingsperiode
    private val fakeUnleash = FakeUnleash()

    @BeforeEach
    fun setup() {
        behandling = DataByggerStubs.hentBehandlingStub()
        every { organisasjonOppslagService.hentOrganisasjoner(any()) } returns DataByggerStubs.hentOrganisasjonDokumentSetStub()

        lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.NO
            fom = LocalDate.now()
            tom = LocalDate.now().plusYears(1L)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
        }

        behandlingsresultat = Behandlingsresultat().apply {
            id = 0L
        }

        val vilkaarsresultat = Vilkaarsresultat()
        val vilkaarBegrunnelse = VilkaarBegrunnelse().apply {
            kode = "SOEKT_FOR_SENT"
        }
        vilkaarsresultat.begrunnelser = hashSetOf(vilkaarBegrunnelse)
        behandlingsresultat.vilkaarsresultater = mutableSetOf(vilkaarsresultat)
        lovvalgsperiode.behandlingsresultat = behandlingsresultat

        anmodningsperiode = Anmodningsperiode(
            LocalDate.now(),
            LocalDate.now().plusYears(2),
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
            null,
            Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A,
            Trygdedekninger.FULL_DEKNING_EOSFO
        )
        behandlingsresultat.anmodningsperioder = mutableSetOf(anmodningsperiode)
        behandlingsresultat.lovvalgsperioder = mutableSetOf(lovvalgsperiode)

        utpekingsperiode = Utpekingsperiode(
            LocalDate.now(),
            LocalDate.now().plusYears(3),
            Land_iso2.DK,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_4
        )
        behandlingsresultat.utpekingsperioder.add(utpekingsperiode)

        behandling = DataByggerStubs.hentBehandlingStub()
        behandlingsresultat.behandling = behandling
        dataBygger = SedDataBygger(
            behandlingsresultatService,
            landvelgerService,
            lovvalgsperiodeService,
            saksbehandlingRegler,
            fakeUnleash
        )

        val tidligereLovvalgsperiode = Lovvalgsperiode().apply {
            fom = LocalDate.now()
            tom = LocalDate.now().plusMonths(2L)
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2
        }
        every { lovvalgsperiodeService.hentTidligereLovvalgsperioder(any()) } returns listOf(tidligereLovvalgsperiode)
    }

    private fun lagSedData(
        periodeType: PeriodeType = PeriodeType.LOVVALGSPERIODE,
        behandlingsresultat: Behandlingsresultat = this.behandlingsresultat,
        customizePersonDokument: (PersonDokument.() -> Unit)? = null
    ): SedDataDto {
        val grunnlag = if (customizePersonDokument != null) {
            lagGrunnlagMedSøknad(customizePersonDokument)
        } else {
            lagGrunnlagMedSøknad()
        }
        return dataBygger.lag(grunnlag, behandlingsresultat, periodeType)
    }

    private fun lagSedDataUtkast(
        periodeType: PeriodeType = PeriodeType.LOVVALGSPERIODE,
        behandlingsresultat: Behandlingsresultat = this.behandlingsresultat,
        customizePersonDokument: (PersonDokument.() -> Unit)? = null
    ): SedDataDto {
        val grunnlag = if (customizePersonDokument != null) {
            lagGrunnlagMedSøknad(customizePersonDokument)
        } else {
            lagGrunnlagMedSøknad()
        }
        return dataBygger.lagUtkast(grunnlag, behandlingsresultat, periodeType)
    }

    private fun lagGrunnlagMedSøknad(persondata: Persondata = DataByggerStubs.lagPersonDokument()): SedDataGrunnlagMedSoknad {
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
        customizePersonDokument: PersonDokument.() -> Unit
    ): SedDataGrunnlagMedSoknad {
        val personDokument = DataByggerStubs.lagPersonDokument().apply(customizePersonDokument)
        return lagGrunnlagMedSøknad(personDokument)
    }

    private fun lagGrunnlagUtenSøknad(persondata: Persondata = DataByggerStubs.lagPersonDokument()): SedDataGrunnlagUtenSoknad {
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
        val sedData = lagSedData()

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
        anmodningsperiode.run {
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            unntakFraBestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4
            tilleggsbestemmelse = Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1
        }

        val sedData = lagSedData(PeriodeType.ANMODNINGSPERIODE)

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
        val sedData = lagSedData(PeriodeType.ANMODNINGSPERIODE)

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
        val sedData = lagSedData(PeriodeType.UTPEKINGSPERIODE)

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
        val sedData = dataBygger.lag(lagGrunnlagUtenSøknad(), behandlingsresultat, PeriodeType.ANMODNINGSPERIODE)

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
        val sedData = lagSedData {
            this.bostedsadresse = Bostedsadresse(
                land = Land(Land.SVERIGE)
            )
        }

        sedData.bostedsadresse.shouldNotBeNull()
            .gateadresse shouldBe IKKE_TILGJENGELIG
    }

    @Test
    fun `lag bostedsadresseUtenGatenavn gatenavnBlirNA`() {
        val sedData = lagSedData {
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
        val sedData = lagSedData {
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
        val sedData = lagSedData {
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
        val persondataMedKontakadresse = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(persondataMedKontakadresse)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.kontaktadresse.shouldNotBeNull()
            .gateadresse shouldBe "gatenavnKontaktadresseFreg"
    }

    @Test
    fun `lag utenKontaktadresse kontadresseErNull`() {
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenKontaktadresse()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(persondata)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.kontaktadresse.shouldBeNull()
    }

    @Test
    fun `lag medOppholdsadresse oppholdsadresseMappes`() {
        val persondataMedOppholdsadresse = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(persondataMedOppholdsadresse)

        val sedData = dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)

        sedData.oppholdsadresse.shouldNotBeNull()
            .gateadresse shouldBe "gatenavnOppholdsadresseFreg"
    }

    @Test
    fun `lag utenOppholdsadresse oppholdsadresseErNull`() {
        val persondata = PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresse()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(persondata)

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
        val sedData = lagSedDataUtkast {
            diskresjonskode = Diskresjonskode().apply {
                kode = "SPSF"
            }
        }

        sedData.shouldNotBeNull().bruker.shouldNotBeNull()
            .harSensitiveOpplysninger shouldBe true
    }

    @Test
    fun `lag brukerHarKode7 forventHarIkkeSensitiveOpplysninger`() {
        val sedData = lagSedDataUtkast {
            diskresjonskode = Diskresjonskode().apply {
                kode = "SPSO"
            }
        }

        sedData.shouldNotBeNull().bruker.shouldNotBeNull()
            .harSensitiveOpplysninger shouldBe false
    }

    @Test
    fun `lag brukerHarIngenDiskresjonskode forventHarIkkeSensitiveOpplysninger`() {
        val sedData = lagSedDataUtkast {
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
        val sedData = dataBygger.lagUtkast(lagGrunnlagUtenSøknad(), behandlingsresultat, PeriodeType.INGEN)

        sedData.run {
            bruker.shouldNotBeNull()
            bostedsadresse.shouldNotBeNull()
            lovvalgsperioder.shouldBeEmpty()
        }
    }

    @Test
    fun `lagUtkast medlemsperiodeTypeLovvalgsperiodeMedSøknad medLovvalgsperioder`() {
        val sedData = lagSedDataUtkast()

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

        val dataGrunnlag = lagGrunnlagMedSøknad()
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
        val dataGrunnlag = lagGrunnlagMedSøknad().apply {
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
        val behandlingsresultatMedVedtak = Behandlingsresultat().apply {
            id = 1L
        }
        val vedtakMetadata = VedtakMetadata().apply {
            vedtaksdato = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        }
        behandlingsresultatMedVedtak.vedtakMetadata = vedtakMetadata

        val avsluttetBehandling = DataByggerStubs.hentBehandlingStub().apply {
            status = Behandlingsstatus.AVSLUTTET
            id = 2L
        }
        behandlingsresultatMedVedtak.behandling = avsluttetBehandling

        behandling.fagsak.leggTilBehandling(behandling)
        behandling.fagsak.leggTilBehandling(avsluttetBehandling)
        behandlingsresultat.behandling = behandling

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedVedtak

        val sedDataDto = lagSedData()
        sedDataDto.shouldNotBeNull().vedtakDto.shouldNotBeNull().run {
            erFørstegangsvedtak shouldBe false
            datoForrigeVedtak shouldBe LocalDate.now()
        }
    }

    @Test
    fun `lagVedtakDto midlertidigLovvalgsbestemt settesSomIkkeFørstegangsvedtak`() {
        val behandlingsresultatMedVedtak = Behandlingsresultat()
        val vedtakMetadata = VedtakMetadata().apply {
            vedtaksdato = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        }
        behandlingsresultatMedVedtak.vedtakMetadata = vedtakMetadata

        val avsluttetBehandling = DataByggerStubs.hentBehandlingStub().apply {
            status = Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            id = 2L
        }
        behandlingsresultatMedVedtak.behandling = avsluttetBehandling

        behandling.fagsak.leggTilBehandling(behandling)
        behandling.fagsak.leggTilBehandling(avsluttetBehandling)
        behandlingsresultat.behandling = behandling

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultatMedVedtak

        val sedDataDto = lagSedData()
        sedDataDto.shouldNotBeNull().vedtakDto.shouldNotBeNull().run {
            erFørstegangsvedtak shouldBe false
            datoForrigeVedtak shouldBe LocalDate.now()
        }
    }

    @Test
    fun `lag arbeidsstedManglerLandkode kasterFeil`() {
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(true, false, false)
        shouldThrow<FunksjonellException> {
            dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)
        }.message.shouldNotBeNull() shouldContain "land er ikke utfylt for arbeidssted"
    }

    @Test
    fun `lag arbeidsgivendeVirksomhetManglerLandkode kasterFeil`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf("uuid")
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(false, true, false)
        shouldThrow<FunksjonellException> {
            dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)
        }.message.shouldNotBeNull() shouldContain "land er ikke utfylt for virksomhet"
    }

    @Test
    fun `lag selvstendigVirksomhetManglerLandkode kasterFeil`() {
        every { avklartefaktaService.hentAvklarteOrgnrOgUuid(any()) } returns setOf("uuid")
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedManglendeAdressefelter(false, false, true)
        shouldThrow<FunksjonellException> {
            dataBygger.lag(sedDataGrunnlagMedSoknad, behandlingsresultat, PeriodeType.LOVVALGSPERIODE)
        }.message.shouldNotBeNull() shouldContain "land er ikke utfylt for selvstendig virksomhet"
    }

    @Test
    fun `lagArbeidssted manglerObligatoriskeFelter blirUnknown`() {
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
        val fagsak = Fagsak.forTest()
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.fagsak = fagsak
        val søknad = behandling.mottatteOpplysninger.shouldNotBeNull().mottatteOpplysningerData
        val sedData = lagSedData()

        sedData.søknadsperiode.shouldNotBeNull().run {
            fom shouldBe søknad.periode.fom
            tom shouldBe søknad.periode.tom
        }
    }

    @Test
    fun `lag erIkkeEuEøs søknadsperiodeBlirIkkeSatt`() {
        val fagsak = Fagsak.forTest {
            type = Sakstyper.TRYGDEAVTALE
        }
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.fagsak = fagsak
        val sedData = lagSedData()

        sedData.søknadsperiode.shouldBeNull()
    }

    @Test
    fun `lag erSed søknadsperiodeBlirIkkeSatt`() {
        val fagsak = Fagsak.forTest()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        behandling.type = Behandlingstyper.FØRSTEGANG
        behandling.fagsak = fagsak
        val sedData = lagSedData()

        sedData.søknadsperiode.shouldBeNull()
    }

    @Test
    fun `lag harIkkeFlyt søknadsperiodeBlirIkkeSatt`() {
        val fagsak = Fagsak.forTest()
        behandling.tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        behandling.type = Behandlingstyper.HENVENDELSE
        behandling.fagsak = fagsak
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true

        val sedData = lagSedData()

        sedData.søknadsperiode.shouldBeNull()
    }

    @Test
    fun `lag medFlereStatsborgerskap alleStatsborgerSkapMappes`() {
        val personDataFraPDL = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val sedDataGrunnlagMedSoknad = lagGrunnlagMedSøknad(personDataFraPDL)

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
