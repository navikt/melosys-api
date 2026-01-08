package no.nav.melosys.service.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.eessi.BucType
import no.nav.melosys.domain.eessi.Institusjon
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.event.ApplicationEventMulticaster
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class EosVedtakServiceKtTest {

    @RelaxedMockK
    private lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    private lateinit var prosessinstansService: ProsessinstansService

    @RelaxedMockK
    private lateinit var eessiService: EessiService

    @RelaxedMockK
    private lateinit var landvelgerService: LandvelgerService

    @RelaxedMockK
    private lateinit var avklartefaktaService: AvklartefaktaService

    @RelaxedMockK
    private lateinit var melosysEventMulticaster: ApplicationEventMulticaster

    @RelaxedMockK
    private lateinit var ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade

    @RelaxedMockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @RelaxedMockK
    private lateinit var behandling: Behandling

    private lateinit var vedtakService: EosVedtakService
    private val behandlingsresultat = Behandlingsresultat()

    @BeforeEach
    fun setUp() {
        vedtakService = EosVedtakService(
            behandlingService,
            behandlingsresultatService,
            oppgaveService,
            prosessinstansService,
            eessiService,
            landvelgerService,
            avklartefaktaService,
            melosysEventMulticaster,
            ferdigbehandlingKontrollFacade,
            saksbehandlingRegler
        )

        SpringSubjectHandler.set(TestSubjectHandler())

        every { behandling.id } returns BEHANDLING_ID
        every { behandling.status } returns Behandlingsstatus.AVSLUTTET
        every { behandling.type } returns Behandlingstyper.FØRSTEGANG
        every { behandling.tema } returns Behandlingstema.UTSENDT_ARBEIDSTAKER
        every { behandling.fagsak } returns Fagsak.forTest()
        every { behandling.toString() } returns "Behandling{id=1, fagsak=MEL-test, type=FØRSTEGANG, status=AVSLUTTET}"

        behandlingsresultat.apply {
            id = BEHANDLING_ID
            this.behandling = this@EosVedtakServiceKtTest.behandling
        }
    }

    @Test
    fun `fattVedtak - er innvilgelse - fatter vedtak og kontrollerer vedtak`() {
        val mottakerinstitusjoner = setOf("AB:CDEF123")
        mockBehandlingsresultat()
        mockEessiReady()
        leggTilLovvalgsperiode(InnvilgelsesResultat.INNVILGET)

        vedtakService.fattVedtak(
            behandling, lagRequest(
                Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
                Vedtakstyper.FØRSTEGANGSVEDTAK,
                BEHANDLINGSRESULTAT_FRITEKST,
                null,
                mottakerinstitusjoner
            )
        )

        behandlingsresultat.type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandlingsresultat.nyVurderingBakgrunn.shouldBeNull()
        behandlingsresultat.begrunnelseFritekst shouldBe BEHANDLINGSRESULTAT_FRITEKST

        behandlingsresultat.vedtakMetadata.shouldNotBeNull().run {
            vedtakstype shouldBe Vedtakstyper.FØRSTEGANGSVEDTAK
            vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong())
        }

        verify { behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify {
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(
                any(),
                eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND),
                eq(BEHANDLINGSRESULTAT_FRITEKST),
                isNull(),
                eq(mottakerinstitusjoner),
                eq(true)
            )
        }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        // Viktig: Behandling-objekt (ikke ID) sendes for å unngå entity reload og race condition
        verify {
            ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(
                any<Behandling>(),
                eq(Sakstyper.EU_EOS),
                any<Behandlingsresultattyper>(),
                isNull()
            )
        }
    }

    @Test
    fun `fattVedtak - land er eessi ready institusjon er satt - fatter vedtak`() {
        val mottakerinstitusjoner = setOf("AB:CDEF123")
        mockBehandlingsresultat()
        mockEessiReady()
        leggTilLovvalgsperiode()

        vedtakService.fattVedtak(
            behandling, lagRequest(
                Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
                Vedtakstyper.FØRSTEGANGSVEDTAK,
                BEHANDLINGSRESULTAT_FRITEKST,
                "FRITEKST_SED",
                mottakerinstitusjoner
            )
        )

        behandlingsresultat.type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandlingsresultat.nyVurderingBakgrunn.shouldBeNull()
        behandlingsresultat.begrunnelseFritekst shouldBe BEHANDLINGSRESULTAT_FRITEKST

        behandlingsresultat.vedtakMetadata.shouldNotBeNull().run {
            vedtakstype shouldBe Vedtakstyper.FØRSTEGANGSVEDTAK
            vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong())
        }

        verify { behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify {
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(
                any(),
                eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND),
                eq(BEHANDLINGSRESULTAT_FRITEKST),
                eq("FRITEKST_SED"),
                eq(mottakerinstitusjoner),
                eq(true)
            )
        }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
    }

    @Test
    fun `fattVedtak - uten mottaker land er ikke eessi ready - fatter vedtak`() {
        mockBehandlingsresultat()
        leggTilLovvalgsperiode()

        vedtakService.fattVedtak(
            behandling, lagRequest(
                Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
                Vedtakstyper.FØRSTEGANGSVEDTAK,
                BEHANDLINGSRESULTAT_FRITEKST,
                "FRITEKST_SED",
                null
            )
        )

        behandlingsresultat.type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandlingsresultat.nyVurderingBakgrunn.shouldBeNull()
        behandlingsresultat.begrunnelseFritekst shouldBe BEHANDLINGSRESULTAT_FRITEKST

        behandlingsresultat.vedtakMetadata.shouldNotBeNull().run {
            vedtakstype shouldBe Vedtakstyper.FØRSTEGANGSVEDTAK
            vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong())
        }

        verify { behandlingService.endreStatus(behandling, Behandlingsstatus.IVERKSETTER_VEDTAK) }
        verify {
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(
                any<Behandling>(),
                eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND),
                eq(BEHANDLINGSRESULTAT_FRITEKST),
                eq("FRITEKST_SED"),
                any<Set<String>>(),
                eq(true)
            )
        }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
    }

    @Test
    fun `fattVedtak - mottaker er null og er anmodning om unntak svar mottatt - fatter vedtak`() {
        mockBehandlingsresultat()
        leggTilLovvalgsperiode()

        val anmodningsperiode = Anmodningsperiode().apply {
            anmodningsperiodeSvar = AnmodningsperiodeSvar().apply {
                anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
            }
        }
        behandlingsresultat.anmodningsperioder = mutableSetOf(anmodningsperiode)

        vedtakService.fattVedtak(
            behandling, lagRequest(
                Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
                Vedtakstyper.FØRSTEGANGSVEDTAK,
                BEHANDLINGSRESULTAT_FRITEKST,
                "FRITEKST_SED",
                null
            )
        )

        behandlingsresultat.type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        behandlingsresultat.nyVurderingBakgrunn.shouldBeNull()
        behandlingsresultat.begrunnelseFritekst shouldBe BEHANDLINGSRESULTAT_FRITEKST

        behandlingsresultat.vedtakMetadata.shouldNotBeNull().run {
            vedtakstype shouldBe Vedtakstyper.FØRSTEGANGSVEDTAK
            vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong())
        }

        verify {
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(
                eq(behandling),
                eq(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND),
                eq(BEHANDLINGSRESULTAT_FRITEKST),
                eq("FRITEKST_SED"),
                any<Set<String>>(),
                eq(true)
            )
        }
    }

    @Test
    fun `fattVedtak - er avslag manglende opplysninger - fatter vedtak`() {
        mockBehandlingsresultat()
        behandlingsresultat.type = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
        // Even though it's avslag, it might still need a basic lovvalgsperiode for the service logic
        leggTilLovvalgsperiode()

        val resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
        val vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK

        vedtakService.fattVedtak(behandling, lagRequest(resultatType, vedtakstype, null, null, null))

        behandlingsresultat.type shouldBe resultatType
        behandlingsresultat.nyVurderingBakgrunn.shouldBeNull()
        behandlingsresultat.begrunnelseFritekst.shouldBeNull()

        behandlingsresultat.vedtakMetadata.shouldNotBeNull().run {
            this.vedtakstype shouldBe vedtakstype
            vedtakKlagefrist shouldBe LocalDate.now().plusWeeks(VedtaksfattingFasade.FRIST_KLAGE_UKER.toLong())
        }

        verify {
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(
                eq(behandling),
                eq(resultatType),
                isNull(),
                isNull(),
                any<Set<String>>(),
                eq(true)
            )
        }
    }

    @Test
    fun `fattVedtak - er avslag - fatter vedtak uten kall til eessi`() {
        mockBehandlingsresultat()
        behandlingsresultat.type = Behandlingsresultattyper.AVSLAG_SØKNAD
        leggTilLovvalgsperiode(InnvilgelsesResultat.AVSLAATT)
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.id) } returns mutableListOf(Land_iso2.SE)

        vedtakService.fattVedtak(
            behandling,
            lagRequest(Behandlingsresultattyper.AVSLAG_SØKNAD, Vedtakstyper.FØRSTEGANGSVEDTAK, null, null, null)
        )

        verify {
            prosessinstansService.opprettProsessinstansIverksettVedtakEos(
                eq(behandling),
                eq(Behandlingsresultattyper.AVSLAG_SØKNAD),
                isNull(),
                isNull(),
                any<Set<String>>(),
                eq(true)
            )
        }
        verify(exactly = 0) { eessiService.validerOgAvklarMottakerInstitusjonerForBuc(any(), any(), any()) }
    }

    @Test
    fun `fattVedtak - prosessinstans finnes - kaster exception`() {
        mockBehandlingsresultat()
        every { prosessinstansService.harVedtakInstans(BEHANDLING_ID) } returns true

        leggTilLovvalgsperiode(InnvilgelsesResultat.AVSLAATT)

        val fattVedtakRequest = lagRequest(
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            Vedtakstyper.FØRSTEGANGSVEDTAK,
            null,
            null,
            null
        )

        shouldThrow<FunksjonellException> {
            vedtakService.fattVedtak(behandling, fattVedtakRequest)
        }.message shouldBe "Det finnes allerede en vedtak-prosess for behandling $behandling"

        verify { prosessinstansService.harVedtakInstans(BEHANDLING_ID) }
        verify(exactly = 0) { prosessinstansService.opprettProsessinstansIverksettVedtakEos(any(), any(), any(), any(), any(), any()) }
    }

    private fun mockBehandlingsresultat() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { behandlingsresultatService.lagre(any()) } returns behandlingsresultat
        every { ferdigbehandlingKontrollFacade.kontrollerVedtakMedRegisteropplysninger(any(), any(), any(), any()) } returns emptyList()
        every { prosessinstansService.harVedtakInstans(BEHANDLING_ID) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling) } returns false
        every { saksbehandlingRegler.harIngenFlyt(behandling) } returns false
    }

    private fun mockEessiReady() {
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns mutableListOf(Land_iso2.SE)
        every { behandling.erNorgeUtpekt() } returns false
        every {
            eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
                any<Set<String>>(),
                any<Collection<Land_iso2>>(),
                any<BucType>()
            )
        } answers { firstArg<Set<String>>() }
        every {
            eessiService.hentEessiMottakerinstitusjoner(
                BucType.LA_BUC_04.name,
                setOf(Landkoder.SE.kode)
            )
        } returns listOf(Institusjon("AB:CDEF123", "inst", Landkoder.SE.kode))
    }

    private fun leggTilLovvalgsperiode(innvilgelsesResultat: InnvilgelsesResultat? = null) {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            this.innvilgelsesresultat = innvilgelsesResultat
            lovvalgsland = Land_iso2.NO
            medlPeriodeID = 123L
            fom = LocalDate.now()
        }
        behandlingsresultat.lovvalgsperioder = mutableSetOf(lovvalgsperiode)
    }

    private fun lagRequest(
        behandlingsresultattype: Behandlingsresultattyper,
        vedtakstype: Vedtakstyper,
        behandlingsresultatFritekst: String?,
        fritekstSed: String?,
        mottakerinstitusjoner: Set<String>?
    ): FattVedtakRequest =
        FattVedtakRequest.Builder()
            .medBehandlingsresultatType(behandlingsresultattype)
            .medVedtakstype(vedtakstype)
            .medFritekst(behandlingsresultatFritekst)
            .medFritekstSed(fritekstSed)
            .medMottakerInstitusjoner(mottakerinstitusjoner)
            .build()

    companion object {
        private const val BEHANDLING_ID = 1L
        private const val BEHANDLINGSRESULTAT_FRITEKST = "FRITEKST HEIHEI"
    }
}
