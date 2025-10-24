package no.nav.melosys.service.unntak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.arkiv.DokumentReferanse
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.AnmodningUnntakKontrollService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AnmodningUnntakServiceTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    @MockK
    private lateinit var landvelgerService: LandvelgerService

    @MockK
    private lateinit var eessiService: EessiService

    @MockK
    private lateinit var anmodningUnntakKontrollService: AnmodningUnntakKontrollService

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    private lateinit var anmodningUnntakService: AnmodningUnntakService

    @BeforeEach
    fun setUp() {
        anmodningUnntakService = AnmodningUnntakService(
            behandlingService,
            behandlingsresultatService,
            oppgaveService,
            prosessinstansService,
            anmodningsperiodeService,
            lovvalgsperiodeService,
            landvelgerService,
            eessiService,
            anmodningUnntakKontrollService,
            joarkFasade
        )

        SubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun `anmodningOmUnntak er EESSI klar med mottaker institusjon prosess opprettet`() {
        val dokumentReferanse = DokumentReferanse("jpID", "dokID")
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = Behandling.forTest {
            this.fagsak = fagsak
            mottatteOpplysninger = MottatteOpplysninger()
        }
        behandling.saksopplysninger.add(lagPersonSaksopplysning())

        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE)
        every { anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID) } returns emptyList()
        every { eessiService.validerOgAvklarMottakerInstitusjonerForBuc(any(), any(), any()) } returns setOf(MOTTAKER_INSTITUSJON)
        every { joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(any(), any()) } just Runs
        every { anmodningsperiodeService.oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007") } just Runs
        every { prosessinstansService.opprettProsessinstansAnmodningOmUnntak(any(), any(), any(), any(), any()) } just Runs
        every { oppgaveService.leggTilbakeBehandlingsoppgaveMedSaksnummer(any()) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK) } just Runs

        anmodningUnntakService.anmodningOmUnntak(
            BEHANDLING_ID,
            MOTTAKER_INSTITUSJON,
            setOf(dokumentReferanse),
            FRITEKST_SED,
            BEGRUNNELSE_FRITEKST
        )

        verify { anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID) }
        verify { anmodningsperiodeService.oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007") }
        verify {
            prosessinstansService.opprettProsessinstansAnmodningOmUnntak(
                any(),
                any(),
                eq(setOf(dokumentReferanse)),
                eq(FRITEKST_SED),
                eq(BEGRUNNELSE_FRITEKST)
            )
        }
        verify { oppgaveService.leggTilbakeBehandlingsoppgaveMedSaksnummer(any()) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK) }
    }

    @Test
    fun `anmodningOmUnntak ikke EESSI ready mottaker institusjon null prosess opprettet`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = Behandling.forTest {
            mottatteOpplysninger = MottatteOpplysninger()
            this.fagsak = fagsak
        }
        behandling.saksopplysninger.add(lagPersonSaksopplysning())

        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns behandling
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE)
        every { anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID) } returns emptyList()
        every { eessiService.validerOgAvklarMottakerInstitusjonerForBuc(any(), any(), any()) } returns emptySet()
        every { joarkFasade.validerDokumenterTilhørerSakOgHarTilgang(any(), any()) } just Runs
        every { anmodningsperiodeService.oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007") } just Runs
        every { prosessinstansService.opprettProsessinstansAnmodningOmUnntak(any(), any(), any(), any(), any()) } just Runs
        every { oppgaveService.leggTilbakeBehandlingsoppgaveMedSaksnummer(any()) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK) } just Runs

        anmodningUnntakService.anmodningOmUnntak(
            BEHANDLING_ID,
            null,
            emptySet(),
            FRITEKST_SED,
            BEGRUNNELSE_FRITEKST
        )

        verify { anmodningUnntakKontrollService.utførKontroller(BEHANDLING_ID) }
        verify { anmodningsperiodeService.oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007") }
        verify {
            prosessinstansService.opprettProsessinstansAnmodningOmUnntak(
                any(),
                any(),
                any(),
                eq(FRITEKST_SED),
                eq(BEGRUNNELSE_FRITEKST)
            )
        }
        verify { oppgaveService.leggTilbakeBehandlingsoppgaveMedSaksnummer(any()) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK) }
    }

    @Test
    fun `anmodningOmUnntakSvar validert forvent metodekall`() {
        val behandling = lagBehandling()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "55667788"
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(any()) } returns lagAnmodningsperiodeSvar()
        every { eessiService.kanOppretteSedTyperPåBuc(any(), any<SedType>()) } returns true
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } returns emptyList()
        every { prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(any(), any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs

        anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, FRITEKST_SED)

        verify { behandlingService.hentBehandling(BEHANDLING_ID) }
        verify { anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(BEHANDLING_ID) }

        val lovvalgsperioder = slot<Collection<Lovvalgsperiode>>()
        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(BEHANDLING_ID), capture(lovvalgsperioder)) }

        val expected = Lovvalgsperiode.av(lagAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG)
        lovvalgsperioder.captured.shouldHaveSize(1).single().run {
            fom shouldBe expected.fom
            tom shouldBe expected.tom
            lovvalgsland shouldBe expected.lovvalgsland
            bestemmelse shouldBe expected.bestemmelse
            tilleggsbestemmelse shouldBe expected.tilleggsbestemmelse
            innvilgelsesresultat shouldBe expected.innvilgelsesresultat
            medlemskapstype shouldBe expected.medlemskapstype
            dekning shouldBe expected.dekning
            medlPeriodeID shouldBe expected.medlPeriodeID
        }

        verify { prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling, FRITEKST_SED) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK) }
    }

    @Test
    fun `anmodningOmUnntakSvar ikke godkjent forvent behandlingsresultat ferdigbehandlet`() {
        val behandling = lagBehandling()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "55667788"
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(any()) } returns lagAnmodningsperiodeSvarNegativt()
        every { eessiService.kanOppretteSedTyperPåBuc(any(), any<SedType>()) } returns true
        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } returns emptyList()
        every { prosessinstansService.opprettProsessinstansAnmodningOmUnntakMottakSvar(any(), any()) } just Runs
        every { oppgaveService.ferdigstillOppgaveMedBehandlingID(any()) } just Runs
        every { behandlingsresultatService.oppdaterBehandlingsresultattype(any(), any()) } just Runs

        anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, FRITEKST_SED)

        verify { behandlingsresultatService.oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET) }
    }

    @Test
    fun `anmodningOmUnntakSvar feil behandlingstype forvent exception`() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandling(any()) } returns behandling

        val exception = shouldThrow<FunksjonellException> {
            anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null)
        }
        exception.message shouldContain "Behandling er ikke av tema ANMODNING_OM_UNNTAK_HOVEDREGEL"
    }

    @Test
    fun `anmodningOmUnntakSvar behandling er avsluttet forvent exception`() {
        val behandling = lagBehandling()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        behandling.status = Behandlingsstatus.AVSLUTTET
        every { behandlingService.hentBehandling(any()) } returns behandling

        val exception = shouldThrow<FunksjonellException> {
            anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null)
        }
        exception.message shouldContain "Behandlingen er avsluttet"
    }

    @Test
    fun `anmodningOmUnntakSvar avslag for lang fritekst forvent exception`() {
        val behandling = lagBehandling()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING

        val anmodningsperiodeSvar = lagAnmodningsperiodeSvar()
        anmodningsperiodeSvar.begrunnelseFritekst = RandomStringUtils.random(256)
        anmodningsperiodeSvar.anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(any()) } returns anmodningsperiodeSvar

        val exception = shouldThrow<FunksjonellException> {
            anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null)
        }
        exception.message shouldContain "Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak"
    }

    @Test
    fun `anmodningOmUnntakSvar kan ikke opprette SED på BUC forvent exception`() {
        val behandling = lagBehandling()
        behandling.tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.SEDOPPL
            dokument = SedDokument().apply {
                rinaSaksnummer = "55667788"
            }
        }
        behandling.saksopplysninger = mutableSetOf(saksopplysning)

        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(BEHANDLING_ID) } returns lagAnmodningsperiodeSvar()
        every { eessiService.kanOppretteSedTyperPåBuc("55667788", SedType.A011) } returns false

        val exception = shouldThrow<FunksjonellException> {
            anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null)
        }
        exception.message shouldContain "Kan ikke opprette SedType A011 på rinaSaknummer: 55667788"
    }

    private fun lagBehandling() = Behandling.forTest {
        id = BEHANDLING_ID
        fagsak = FagsakTestFactory.lagFagsak()
    }

    private fun lagPersonSaksopplysning() = Saksopplysning().apply {
        type = SaksopplysningType.PERSOPL
        dokument = PersonDokument().apply {
            bostedsadresse?.postnr = "2123"
        }
    }

    private fun lagAnmodningsperiodeSvar() = AnmodningsperiodeSvar().apply {
        anmodningsperiodeSvarType = Anmodningsperiodesvartyper.INNVILGELSE
        anmodningsperiode = lagAnmodningsperiode()
    }

    private fun lagAnmodningsperiodeSvarNegativt() = AnmodningsperiodeSvar().apply {
        anmodningsperiodeSvarType = Anmodningsperiodesvartyper.AVSLAG
        anmodningsperiode = lagAnmodningsperiode()
    }

    private fun lagAnmodningsperiode() = Anmodningsperiode(
        LocalDate.EPOCH,
        LocalDate.MAX,
        Land_iso2.NO,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1,
        null,
        Land_iso2.SE,
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
        Trygdedekninger.FULL_DEKNING_EOSFO
    )
    companion object {
        private const val BEHANDLING_ID = 1L
        private const val FRITEKST_SED = "Ytterligere info som fritekst"
        private const val BEGRUNNELSE_FRITEKST = "FRITEKST"
        private const val MOTTAKER_INSTITUSJON = "SE:432"
    }
}
