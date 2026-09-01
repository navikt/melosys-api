package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.aarsavregning.GjeldendeBehandlingsresultaterForÅrsavregning
import no.nav.melosys.service.avgift.aarsavregning.SkattepliktigAarsavregningOpprettelseService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.UUID

/**
 * Dekker kjøringsverktøyets eget lag: rapporten, taket, dedupliseringen og at løkka går videre
 * etter en feilet sak.
 *
 * Selve vurderingene ligger i [SkattepliktigAarsavregningOpprettelseService] og er dekket av
 * `SkattepliktigAarsavregningOpprettelseServiceTest`; her bygges den ekte, slik at testene også
 * viser hva verktøyet får ut av den. Transaksjonsgarantiene kan ikke bevises med mocks og ligger i
 * `SkattepliktigeAarsavregningSkarpIT`.
 */
class SkattepliktigeAarsavregningSkarpTest {

    private val prosessinstansService = mockk<ProsessinstansService>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val årsavregningService = mockk<ÅrsavregningService>()
    private val trygdeavgiftMottakerService = mockk<TrygdeavgiftMottakerService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val skarpUtfoerer = mockk<SkattepliktigeAarsavregningSkarpUtfoerer>()

    private val opprettelseService = SkattepliktigAarsavregningOpprettelseService(
        prosessinstansService,
        fagsakService,
        behandlingService,
        behandlingsresultatService,
        årsavregningService,
        trygdeavgiftMottakerService,
    )

    private val service = SkattepliktigeAarsavregningDryrunService(
        opprettelseService,
        årsavregningService,
        trygdeavgiftMottakerService,
        skarpUtfoerer,
    )

    @Test
    fun `skarp opprettelse ber om innhentingsbrev, som Kafka-flyten`() {
        every { prosessinstansService.opprettArsavregningsBehandlingProsessflyt(any(), any(), any(), any()) } returns UUID.randomUUID()

        utfoerer().opprettProsessinstans("MEL-1", "2023")

        verify {
            prosessinstansService.opprettArsavregningsBehandlingProsessflyt(
                "MEL-1",
                "2023",
                Behandlingsaarsaktyper.MELDING_FRA_SKATT,
                true,
            )
        }
    }

    @Test
    fun `skarp-utføreren sender statusen den fikk videre til re-lesingen`() {
        val behandling = Behandling.forTest { status = Behandlingsstatus.IVERKSETTER_VEDTAK }
        every { behandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val bump = utfoerer().settStatusVurderDokument(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT)

        bump.oppdatert shouldBe false
        bump.faktiskStatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK
        verify(exactly = 0) { behandlingService.lagre(any()) }
    }

    @Test
    fun `en feilet sak stopper ikke resten av kjøringen`() {
        val fagsakSomFeiler = lagFagsak("MEL-1")
        val fagsakSomGaarBra = lagFagsak("MEL-2")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns
            listOf(fagsakSomFeiler, fagsakSomGaarBra)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV

        every { skarpUtfoerer.opprettProsessinstans("MEL-1", "2023") } throws RuntimeException("oppslag feilet")
        every { skarpUtfoerer.opprettProsessinstans("MEL-2", "2023") } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
        )

        verify { skarpUtfoerer.opprettProsessinstans("MEL-2", "2023") }
        with(service.status()) {
            this["antallSakerFunnet"] shouldBe 2
            this["antallOpprettet"] shouldBe 1
            this["antallSkarpFeilet"] shouldBe 1
        }
        service.resultater.size shouldBe 2
    }

    @Test
    fun `hoppet over statusoppdatering rapporteres som hoppet over, ikke som feil`() {
        // Driver status-grenen i løkka: uten dette har hele grenen null dekning, og en hardkodet
        // forventetStatus i kallet ville passert alle testene.
        val fagsak = lagFagsakMedÅrsavregning(Behandlingsstatus.AVVENT_DOK_PART)
        val årsavregningBehandling = fagsak.behandlinger.first()
        val behandlingsresultat = Behandlingsresultat.forTest {
            this.behandling { id = årsavregningBehandling.id }
            årsavregning { aar = GJELDER_ÅR }
        }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(årsavregningBehandling.id) } returns behandlingsresultat
        stubTrygdeavgift(behandlingsresultat)
        every { skarpUtfoerer.settStatusVurderDokument(any(), any()) } returns
            SkattepliktigAarsavregningOpprettelseService.StatusBumpResultat(
                oppdatert = false,
                faktiskStatus = Behandlingsstatus.IVERKSETTER_VEDTAK,
            )

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
        )

        // Statusen løkka observerte skal sendes med — det er hele poenget med re-lesingen.
        verify {
            skarpUtfoerer.settStatusVurderDokument(årsavregningBehandling.id, Behandlingsstatus.AVVENT_DOK_PART)
        }
        with(service.resultater.single()) {
            statusOppdatert shouldBe false
            hoppetOverAarsak shouldNotBe null
            feilmelding shouldBe null
        }
        with(service.status()) {
            this["antallStatusHoppetOver"] shouldBe 1
            this["antallStatusOppdatert"] shouldBe 0
            this["antallSkarpFeilet"] shouldBe 0
        }
    }

    @Test
    fun `flere aktive årsavregninger stopper saken i stedet for å endre en vilkårlig`() {
        val fagsak = Fagsak.forTest {
            saksnummer = "MEL-1"
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status(Saksstatuser.OPPRETTET)
            behandling {
                id = 1
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.AVVENT_DOK_PART
            }
            behandling {
                id = 2
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.UNDER_BEHANDLING
            }
        }
        val behandlingsresultat = Behandlingsresultat.forTest { årsavregning { aar = GJELDER_ÅR } }

        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
        )

        verify(exactly = 0) { skarpUtfoerer.settStatusVurderDokument(any(), any()) }
        verify(exactly = 0) { skarpUtfoerer.opprettProsessinstans(any(), any()) }
        service.resultater.single().feilmelding shouldNotBe null
        service.status()["antallSakerFeilet"] shouldBe 1
    }

    /**
     * maksAntall er nullbar, og løkka håndhever taket kun når den ikke er null. `{"skarp": true}`
     * uten feltet startet dermed en kjøring helt uten tak. Asserten er at kallet avvises før jobben
     * startes.
     */
    @Test
    fun `ekte kjøring uten maksAntall avvises uten å starte jobben`() {
        val dryrunService = mockk<SkattepliktigeAarsavregningDryrunService>(relaxed = true)
        val controller = SkattepliktigeAarsavregningDryrunController(dryrunService)

        val utenTak = controller.run(
            SkattehendelseRunRequest(
                skattehendelser = listOf(SkattehendelseDryrunItem("2024", AKTØR_ID)),
                skarp = true,
            )
        )
        val nullTak = controller.run(
            SkattehendelseRunRequest(
                skattehendelser = listOf(SkattehendelseDryrunItem("2024", AKTØR_ID)),
                skarp = true,
                maksAntall = 0,
            )
        )

        utenTak.statusCode shouldBe HttpStatus.BAD_REQUEST
        nullTak.statusCode shouldBe HttpStatus.BAD_REQUEST
        verify(exactly = 0) { dryrunService.prosesserSkattehendelserAsynkront(any(), any(), any()) }
    }

    @Test
    fun `simulering uten maksAntall slipper gjennom, og ekte kjøring med tak starter jobben`() {
        val dryrunService = mockk<SkattepliktigeAarsavregningDryrunService>(relaxed = true)
        val controller = SkattepliktigeAarsavregningDryrunController(dryrunService)
        val hendelser = listOf(SkattehendelseDryrunItem("2024", AKTØR_ID))

        controller.run(SkattehendelseRunRequest(hendelser, skarp = false)).statusCode shouldBe HttpStatus.OK
        controller.run(SkattehendelseRunRequest(hendelser, skarp = true, maksAntall = 1)).statusCode shouldBe HttpStatus.OK

        verify(exactly = 1) { dryrunService.prosesserSkattehendelserAsynkront(hendelser, false, null) }
        verify(exactly = 1) { dryrunService.prosesserSkattehendelserAsynkront(hendelser, true, 1) }
    }

    /**
     * En aktiv ÅRSAVREGNING-behandling uten rad i aarsavregning stopper saken. Her pinnes at stoppen
     * havner i rapporten med behandlings-id-en, slik at den som kjører finner behandlingen som må
     * lukkes — feilen er ellers usynlig i en batch som fortsetter.
     */
    @Test
    fun `årløs aktiv årsavregning havner i rapporten med behandlings-id`() {
        val fagsak = lagFagsakMedÅrsavregning(Behandlingsstatus.VURDER_DOKUMENT, BEHANDLING_ID)
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        stubTrygdeavgift(behandlingsresultat)
        // Årløs: behandlingsresultatet finnes, men har ingen aarsavregning-rad.
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
            maksAntall = 5,
        )

        // Ingen ny årsavregning ved siden av den årløse, og ingen brev til borgeren.
        verify(exactly = 0) { skarpUtfoerer.opprettProsessinstans(any(), any()) }
        verify(exactly = 0) { skarpUtfoerer.settStatusVurderDokument(any(), any()) }

        val feil = service.resultater.single().feilmelding
        feil shouldNotBe null
        feil!! shouldContain BEHANDLING_ID.toString()
        feil shouldContain "MEL-1"
        feil shouldContain "lukk den årløse behandlingen"
        service.status()["antallSakerFeilet"] shouldBe 1
    }

    /**
     * Opprettelsen er ikke idempotent på sak og år, så to hendelser for samme person og år ga to
     * årsavregninger og to innhentingsbrev til samme borger. En korrigert skattemelding gir nettopp
     * en ny hendelse for et år vi allerede har sett.
     */
    @Test
    fun `duplikate hendelser for samme person og år gir kun én opprettelse`() {
        val fagsak = lagFagsak("MEL-1")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        stubTrygdeavgift(behandlingsresultat)
        every { skarpUtfoerer.opprettProsessinstans("MEL-1", "2023") } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(
                SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID),
                SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID),
            ),
            skarp = true,
            maksAntall = 5,
        )

        verify(exactly = 1) { skarpUtfoerer.opprettProsessinstans("MEL-1", "2023") }
        with(service.status()) {
            this["antallInputHendelser"] shouldBe 2
            this["antallDuplikaterFjernet"] shouldBe 1
            this["antallOpprettet"] shouldBe 1
        }
    }

    @Test
    fun `ulike år for samme person er ikke duplikater`() {
        val fagsak = lagFagsak("MEL-1")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), any()) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { skarpUtfoerer.opprettProsessinstans(any(), any()) } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(
                SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID),
                SkattehendelseDryrunItem(gjelderPeriode = "2024", identifikator = AKTØR_ID),
            ),
            skarp = true,
            maksAntall = 5,
        )

        verify(exactly = 1) { skarpUtfoerer.opprettProsessinstans("MEL-1", "2023") }
        verify(exactly = 1) { skarpUtfoerer.opprettProsessinstans("MEL-1", "2024") }
        service.status()["antallDuplikaterFjernet"] shouldBe 0
    }

    /**
     * De to feilene i kjøringen har hvert sitt underlag: en sak kan feile før vi vet om den har
     * trygdeavgift (da er den ikke med i antallSakerFunnet), eller etter at den er med (da er den
     * det). Ett felles felt for begge gjør at antallSakerFunnet minus feiltallet ikke er antall
     * saker som gikk bra — her ville det gitt null, og det er feil svar på et tall den som kjører
     * bruker til å avgjøre om kjøringen kan gjentas.
     */
    @Test
    fun `feil før og etter at saken er vurdert telles hver for seg`() {
        val feilerIFilteret = lagFagsak("MEL-1")
        val feilerUnderVurdering =
            lagFagsakMedÅrsavregning(Behandlingsstatus.VURDER_DOKUMENT, BEHANDLING_ID, saksnummer = "MEL-2")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns
            listOf(feilerIFilteret, feilerUnderVurdering)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("MEL-1", GJELDER_ÅR) } throws
            RuntimeException("oppslag feilet for MEL-1")
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning("MEL-2", GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        // Årløs behandling: saken er med i antallSakerFunnet, men feiler under vurderingen.
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
            maksAntall = 5,
        )

        with(service.status()) {
            this["antallSakerFunnet"] shouldBe 1
            this["antallSakerFeilet"] shouldBe 1
            this["antallSakerIkkeVurdert"] shouldBe 1
        }
    }

    /**
     * Tellerne over sakene skal partisjonere [antallSakerFunnet]: hver sak som passerte filteret
     * havner i nøyaktig én av dem. Uten det er ikke summen sammenlignbar med totalen, og den som
     * kjører kan ikke se om alle sakene er gjort rede for.
     *
     * Her nås taket midt i en aktør med to saker, som er tilfellet under en canary.
     */
    @Test
    fun `sakstellerne går opp mot antall saker funnet også når taket kapper`() {
        val fagsaker = listOf(lagFagsak("MEL-1"), lagFagsak("MEL-2"))
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns fagsaker

        val behandlingsresultat = Behandlingsresultat.forTest { }
        stubTrygdeavgift(behandlingsresultat)
        every { skarpUtfoerer.opprettProsessinstans(any(), any()) } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
            maksAntall = 1,
        )

        with(service.status()) {
            this["antallSakerFunnet"] shouldBe 2
            this["antallVilleOpprettetProsessinstans"] shouldBe 1
            this["antallSakerHoppetOverPgaTak"] shouldBe 1
            summerSakstellere() shouldBe this["antallSakerFunnet"]
        }
        // Saken som ble kappet må være synlig, ellers vet ikke den som kjører at den finnes.
        service.resultater.map { it.saksnummer } shouldBe listOf("MEL-1", "MEL-2")
    }

    /**
     * En kjøring som stopper på taket har ikke gjort resten av lista. Uten en markør leser den som
     * kjører antallInputHendelser og tror hele lista er kjørt.
     */
    @Test
    fun `kjøring som stoppes av taket sier fra at den ble avbrutt`() {
        val fagsak = lagFagsak("MEL-1")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, any()) } returns listOf(fagsak)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), any()) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { skarpUtfoerer.opprettProsessinstans(any(), any()) } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(
                SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = "111"),
                SkattehendelseDryrunItem(gjelderPeriode = "2024", identifikator = "222"),
            ),
            skarp = true,
            maksAntall = 1,
        )

        with(service.status()) {
            this["antallHendelserProsessert"] shouldBe 1
            this["stoppetPgaTak"] shouldBe true
            // Oppsummeringen skal finnes selv om kjøringen ble avbrutt.
            @Suppress("UNCHECKED_CAST")
            (this["result"] as Map<String, Any?>)["antallInputHendelser"] shouldBe 2
        }
    }

    /**
     * En aktør der alle sakene feilet i filteret har ikke «ingen sak med trygdeavgift» — vi vet
     * ikke. Å telle den som uten treff sier at aktøren er avklart, og de sakene blir usynlige for
     * den som skal rydde opp.
     */
    @Test
    fun `aktør der alle saker feilet i filteret telles ikke som uten treff`() {
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns
            listOf(lagFagsak("MEL-1"), lagFagsak("MEL-2"))
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } throws
            RuntimeException("oppslag feilet")

        service.prosesserSkattehendelser(
            listOf(SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID)),
            skarp = true,
            maksAntall = 5,
        )

        with(service.status()) {
            this["antallSakerIkkeVurdert"] shouldBe 2
            this["antallUtenTreff"] shouldBe 0
        }
    }

    /**
     * Input er håndbygd fra en SQL-dump, så formatvariasjon på året er reell. «02023» og «2023» er
     * samme år, og skal ikke gi to årsavregninger og to innhentingsbrev til samme borger. Året som
     * sendes videre til opprettelsen skal være det parsede, ikke den rå strengen.
     */
    @Test
    fun `samme år skrevet ulikt er duplikater, og året normaliseres før opprettelse`() {
        val fagsak = lagFagsak("MEL-1")
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, AKTØR_ID) } returns listOf(fagsak)

        val behandlingsresultat = Behandlingsresultat.forTest { }
        stubTrygdeavgift(behandlingsresultat)
        every { skarpUtfoerer.opprettProsessinstans(any(), any()) } returns UUID.randomUUID()

        service.prosesserSkattehendelser(
            listOf(
                SkattehendelseDryrunItem(gjelderPeriode = "2023", identifikator = AKTØR_ID),
                SkattehendelseDryrunItem(gjelderPeriode = "02023", identifikator = AKTØR_ID),
            ),
            skarp = true,
            maksAntall = 5,
        )

        service.status()["antallDuplikaterFjernet"] shouldBe 1
        verify(exactly = 1) { skarpUtfoerer.opprettProsessinstans("MEL-1", "2023") }
        verify(exactly = 0) { skarpUtfoerer.opprettProsessinstans("MEL-1", "02023") }
    }

    /**
     * `/status` serialiserte den levende exceptions-mappen mens den asynkrone jobben skrev til den
     * — og `/status` er nettopp det som polles mens feil registreres.
     */
    @Test
    fun `status returnerer et øyeblikksbilde av exceptions, ikke den levende mappen`() {
        val monitor = JobMonitor(jobName = "test", stats = TomStats())
        monitor.registerException(IllegalStateException("første"))

        @Suppress("UNCHECKED_CAST")
        val foer = monitor.status()["exceptions"] as Map<String, Int>
        monitor.registerException(IllegalStateException("andre"))

        foer.keys shouldBe setOf("første")
        @Suppress("UNCHECKED_CAST")
        (monitor.status()["exceptions"] as Map<String, Int>).keys shouldBe setOf("første", "andre")
    }

    /**
     * JobMonitor er felles for flere jobber, og /status-rapportene deres leses med den første feilen
     * først. Rekkefølgen er en del av avlesningen, ikke en tilfeldighet ved implementasjonen —
     * `toList()` her, ikke `setOf`, nettopp fordi det er rekkefølgen som testes.
     */
    @Test
    fun `exceptions beholder rekkefølgen de ble registrert i`() {
        val monitor = JobMonitor(jobName = "test", stats = TomStats())

        monitor.registerException(IllegalStateException("aaa siste"))
        monitor.registerException(IllegalStateException("zzz først"))
        monitor.registerException(IllegalArgumentException("mmm midt"))

        @Suppress("UNCHECKED_CAST")
        val exceptions = monitor.status()["exceptions"] as Map<String, Int>
        exceptions.keys.toList() shouldBe listOf("aaa siste", "zzz først", "mmm midt")
    }

    private class TomStats : JobMonitor.Stats {
        override fun reset() = Unit
        override fun asMap(): Map<String, Any?> = emptyMap()
    }

    private fun utfoerer() = SkattepliktigeAarsavregningSkarpUtfoerer(opprettelseService)

    /**
     * De fire tellerne som skal dele sakene som passerte filteret mellom seg. Summen er
     * antallSakerFunnet; antallVilleOppdatertStatus er ikke med, den er en delmengde av
     * antallMedEksisterendeAarsavregning.
     */
    private fun Map<String, Any?>.summerSakstellere(): Int =
        listOf(
            "antallVilleOpprettetProsessinstans",
            "antallMedEksisterendeAarsavregning",
            "antallSakerFeilet",
            "antallSakerHoppetOverPgaTak",
        ).sumOf { this[it] as? Int ?: 0 }

    private fun stubTrygdeavgift(behandlingsresultat: Behandlingsresultat) {
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), GJELDER_ÅR) } returns
            GjeldendeBehandlingsresultaterForÅrsavregning(
                behandlingsresultat,
                sisteBehandlingsresultatMedAvgift = behandlingsresultat,
                sisteÅrsavregning = behandlingsresultat,
            )
        every { trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat) } returns true
        every { trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) } returns
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
    }

    private fun lagFagsak(saksnummer: String) = Fagsak.forTest {
        this.saksnummer = saksnummer
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
        behandling { status = Behandlingsstatus.AVSLUTTET }
    }

    private fun lagFagsakMedÅrsavregning(
        behandlingsstatus: Behandlingsstatus,
        behandlingId: Long? = null,
        saksnummer: String = "MEL-1",
    ) =
        Fagsak.forTest {
            this.saksnummer = saksnummer
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status(Saksstatuser.OPPRETTET)
            behandling {
                behandlingId?.let { id = it }
                type = Behandlingstyper.ÅRSAVREGNING
                status = behandlingsstatus
            }
        }

    companion object {
        const val AKTØR_ID = FagsakTestFactory.BRUKER_AKTØR_ID
        const val GJELDER_ÅR = 2023
        const val BEHANDLING_ID = 42L
    }
}
