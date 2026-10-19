package no.nav.melosys.service.vedtak.etterregistrering

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.vedtak.VedtakHendelseMeldingBygger
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.Optional

class EtterregistreringAvVedtaksmeldingerTest {

    private val utvalg: EtterregistreringUtvalg = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val persondataService: PersondataService = mockk()
    private val bygger: VedtakHendelseMeldingBygger = mockk()
    private val producer: KafkaMelosysHendelseProducer = mockk(relaxed = true)

    private val etterregistrering = EtterregistreringAvVedtaksmeldinger(
        utvalg, behandlingService, persondataService, bygger, producer, mockk<PlatformTransactionManager>(relaxed = true)
    )

    @Test
    fun `simulering bygger meldingene og rapporterer per vedtaksmåned uten å sende`() {
        gittUtvalg(
            utvalgt(1, "MEL-1", "2024-03-10"),
            utvalgt(2, "MEL-2", "2024-03-20"),
            utvalgt(3, "MEL-3", "2024-04-01"),
        )

        etterregistrering.kjør(parametre(skarp = false))

        verify(exactly = 0) { producer.produserBestillingsmelding(any()) }
        verify(exactly = 3) { bygger.bygg(any(), FOLKEREGISTERIDENT) }
        with(etterregistrering.rapport()) {
            skarp shouldBe false
            sendteBehandlingIder shouldContainExactly listOf(1L, 2L, 3L)
            perVedtaksmaaned shouldContainExactly listOf(
                VedtaksmånedRad("2024-03", iUtvalget = 2, sendt = 2, utenIdent = 0, feilet = 0),
                VedtaksmånedRad("2024-04", iUtvalget = 1, sendt = 1, utenIdent = 0, feilet = 0),
            )
        }
        etterregistrering.status()["antallSendt"] shouldBe 3
        etterregistrering.status()["avbruttAarsak"] shouldBe null
    }

    @Test
    fun `skarp kjøring sender i utvalgets rekkefølge og stopper ved maksAntall`() {
        gittUtvalg(utvalgt(5, "MEL-1", "2024-01-01"), utvalgt(3, "MEL-2", "2024-02-01"), utvalgt(4, "MEL-3", "2024-03-01"))
        val sendt = mutableListOf<MelosysHendelse>()
        every { producer.produserBestillingsmelding(capture(sendt)) } returns Unit

        etterregistrering.kjør(parametre(skarp = true, maksAntall = 2))

        sendt shouldContainExactly listOf(hendelse(5), hendelse(3))
        etterregistrering.rapport().sendteBehandlingIder shouldContainExactly listOf(5L, 3L)
        etterregistrering.status()["avbruttAarsak"] shouldBe "maksAntall=2 er nådd"
        etterregistrering.status()["antallBehandlet"] shouldBe 2
    }

    @Test
    fun `behandling uten folkeregisterident telles og sendes ikke, og identen slås opp én gang per sak`() {
        gittUtvalg(utvalgt(1, "MEL-1", "2024-01-01", aktørId = UKJENT_AKTØR), utvalgt(2, "MEL-1", "2024-02-01", aktørId = UKJENT_AKTØR))
        every { persondataService.finnFolkeregisterident(UKJENT_AKTØR) } returns Optional.empty()

        etterregistrering.kjør(parametre(skarp = true, maksAntall = 10))

        verify(exactly = 1) { persondataService.finnFolkeregisterident(UKJENT_AKTØR) }
        verify(exactly = 0) { producer.produserBestillingsmelding(any()) }
        etterregistrering.status()["antallUtenIdent"] shouldBe 2
        etterregistrering.rapport().sendteBehandlingIder.shouldBeEmpty()
    }

    @Test
    fun `en feilet behandling stopper ikke kjøringen, og feilen vises uten meldingen`() {
        gittUtvalg(utvalgt(1, "MEL-1", "2024-01-01"), utvalgt(2, "MEL-2", "2024-01-02"), utvalgt(3, "MEL-3", "2024-01-03"))
        every { bygger.bygg(match { it.id == 2L }, any()) } throws IllegalStateException("feil for $FOLKEREGISTERIDENT")

        etterregistrering.kjør(parametre(skarp = true, maksAntall = 10))

        verify(exactly = 2) { producer.produserBestillingsmelding(any()) }
        with(etterregistrering.rapport()) {
            sendteBehandlingIder shouldContainExactly listOf(1L, 3L)
            feil shouldContainExactly listOf(EtterregistreringFeil(2, "MEL-2", "IllegalStateException"))
        }
        etterregistrering.status()["antallFeilet"] shouldBe 1
        etterregistrering.status().toString() shouldNotContain FOLKEREGISTERIDENT
    }

    @Test
    fun `kjøringen stopper når antall feil går over maksFeil`() {
        gittUtvalg(utvalgt(1, "MEL-1", "2024-01-01"), utvalgt(2, "MEL-2", "2024-01-02"), utvalgt(3, "MEL-3", "2024-01-03"))
        every { bygger.bygg(any(), any()) } throws IllegalStateException("feil")

        etterregistrering.kjør(parametre(skarp = true, maksAntall = 10, maksFeil = 1))

        etterregistrering.status()["antallFeilet"] shouldBe 2
        etterregistrering.status()["avbruttAarsak"] shouldBe "Stoppet etter for mange feil (maksFeil=1)"
    }

    @Test
    fun `vedtakFra, vedtakTil og behandlingIder avgrenser utvalget`() {
        gittUtvalg(
            utvalgt(1, "MEL-1", "2024-12-31"),
            utvalgt(2, "MEL-2", "2025-01-01"),
            utvalgt(3, "MEL-3", "2025-06-30"),
            utvalgt(4, "MEL-4", "2025-07-01"),
        )

        etterregistrering.kjør(parametre(vedtakFra = LocalDate.of(2025, 1, 1), vedtakTil = LocalDate.of(2025, 6, 30)))
        etterregistrering.rapport().sendteBehandlingIder shouldContainExactly listOf(2L, 3L)

        etterregistrering.kjør(parametre(behandlingIder = setOf(4L, 1L)))
        etterregistrering.rapport().sendteBehandlingIder shouldContainExactly listOf(1L, 4L)
    }

    @Test
    fun `stopp på grunn av feil på siste behandling vises som avbrutt`() {
        gittUtvalg(utvalgt(1, "MEL-1", "2024-01-01"))
        every { bygger.bygg(any(), any()) } throws IllegalStateException("feil")

        etterregistrering.kjør(parametre(skarp = true, maksAntall = 10, maksFeil = 0))

        etterregistrering.status()["avbruttAarsak"] shouldBe "Stoppet etter for mange feil (maksFeil=0)"
    }

    @Test
    fun `et nytt kall mens forrige ligger i kø avvises med 409, og kjøringen frigir plassen når den er ferdig`() {
        gittUtvalg()
        val køetJobb = spyk(etterregistrering)
        every { køetJobb.kjørAsynkront(any()) } returns Unit
        val controller = EtterregistreringAvVedtaksmeldingerController(køetJobb)

        controller.run(EtterregistreringRequest(skarp = true, maksAntall = 1)).statusCode shouldBe HttpStatus.OK
        controller.run(EtterregistreringRequest(skarp = true, maksAntall = 1)).statusCode shouldBe HttpStatus.CONFLICT
        verify(exactly = 1) { køetJobb.kjørAsynkront(any()) }

        køetJobb.kjør(parametre())
        controller.run(EtterregistreringRequest()).statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `feil i utvalget gir tom rapport og feilVedUtvalg`() {
        every { utvalg.finnBehandlinger(any()) } throws RuntimeException("ORA-00942")

        etterregistrering.kjør(parametre())

        etterregistrering.status()["feilVedUtvalg"] shouldBe "RuntimeException"
        etterregistrering.rapport().perVedtaksmaaned.shouldBeEmpty()
    }

    @Test
    fun `skarp kjøring uten positivt maksAntall avvises uten å starte jobben`() {
        val etterregistreringMock: EtterregistreringAvVedtaksmeldinger = mockk(relaxed = true)
        val controller = EtterregistreringAvVedtaksmeldingerController(etterregistreringMock)

        controller.run(EtterregistreringRequest(skarp = true)).statusCode shouldBe HttpStatus.BAD_REQUEST
        controller.run(EtterregistreringRequest(skarp = true, maksAntall = 0)).statusCode shouldBe HttpStatus.BAD_REQUEST
        controller.run(EtterregistreringRequest(maksFeil = -1)).statusCode shouldBe HttpStatus.BAD_REQUEST
        controller.run(EtterregistreringRequest(vedtakFra = LocalDate.of(2025, 2, 1), vedtakTil = LocalDate.of(2025, 1, 1)))
            .statusCode shouldBe HttpStatus.BAD_REQUEST
        controller.run(EtterregistreringRequest(behandlingIder = emptyList())).statusCode shouldBe HttpStatus.BAD_REQUEST

        verify(exactly = 0) { etterregistreringMock.kjørAsynkront(any()) }
    }

    @Test
    fun `simulering starter med standardverdiene`() {
        val etterregistreringMock: EtterregistreringAvVedtaksmeldinger = mockk(relaxed = true)
        every { etterregistreringMock.reserver() } returns true

        EtterregistreringAvVedtaksmeldingerController(etterregistreringMock).run(EtterregistreringRequest())
            .statusCode shouldBe HttpStatus.OK

        verify {
            etterregistreringMock.kjørAsynkront(
                EtterregistreringParametre(periodeTomFra = LocalDate.of(2024, 1, 1), maksFeil = 100, skarp = false)
            )
        }
    }

    private fun parametre(
        skarp: Boolean = false,
        maksAntall: Int? = null,
        maksFeil: Int = 100,
        vedtakFra: LocalDate? = null,
        vedtakTil: LocalDate? = null,
        behandlingIder: Set<Long>? = null,
    ) = EtterregistreringParametre(
        periodeTomFra = LocalDate.of(2024, 1, 1),
        vedtakFra = vedtakFra,
        vedtakTil = vedtakTil,
        behandlingIder = behandlingIder,
        maksAntall = maksAntall,
        maksFeil = maksFeil,
        skarp = skarp,
    )

    private fun gittUtvalg(vararg behandlinger: Pair<UtvalgtBehandling, Behandling>) {
        every { utvalg.finnBehandlinger(LocalDate.of(2024, 1, 1)) } returns behandlinger.map { it.first }
        behandlinger.forEach { (utvalgt, behandling) ->
            every { behandlingService.hentBehandling(utvalgt.behandlingId) } returns behandling
            every { bygger.bygg(behandling, FOLKEREGISTERIDENT) } returns hendelse(utvalgt.behandlingId)
        }
        every { persondataService.finnFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns Optional.of(FOLKEREGISTERIDENT)
    }

    private fun utvalgt(id: Long, saksnummer: String, vedtaksdato: String, aktørId: String = FagsakTestFactory.BRUKER_AKTØR_ID) =
        UtvalgtBehandling(id, saksnummer, LocalDate.parse(vedtaksdato)) to Behandling.forTest {
            this.id = id
            fagsak {
                this.saksnummer = saksnummer
                type = Sakstyper.FTRL
                medBruker { this.aktørId = aktørId }
            }
        }

    // Året i perioden er behandling-id-en, så testene ser hvilken behandling en melding kom fra.
    private fun hendelse(behandlingId: Long) = MelosysHendelse(
        VedtakHendelseMelding(
            folkeregisterIdent = FOLKEREGISTERIDENT,
            sakstype = Sakstyper.FTRL,
            behandligsresultatType = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN,
            medlemskapsperioder = listOf(
                Periode(LocalDate.of(2000 + behandlingId.toInt(), 1, 1), LocalDate.of(2000 + behandlingId.toInt(), 12, 31), InnvilgelsesResultat.INNVILGET)
            ),
            lovvalgsperioder = emptyList(),
        )
    )

    private companion object {
        const val FOLKEREGISTERIDENT = "30056928150"
        const val UKJENT_AKTØR = "9999999999990"
    }
}
