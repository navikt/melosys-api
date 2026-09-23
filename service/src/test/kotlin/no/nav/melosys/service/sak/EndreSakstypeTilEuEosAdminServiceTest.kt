package no.nav.melosys.service.sak

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.EU_EOS
import no.nav.melosys.domain.kodeverk.Sakstyper.FTRL
import no.nav.melosys.domain.kodeverk.Sakstyper.TRYGDEAVTALE
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.UNDER_BEHANDLING
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.YRKESAKTIV
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(MockKExtension::class)
class EndreSakstypeTilEuEosAdminServiceTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @RelaxedMockK
    private lateinit var endreSakService: EndreSakService

    @MockK
    private lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    private lateinit var service: EndreSakstypeTilEuEosAdminService

    @BeforeEach
    fun setUp() {
        service = EndreSakstypeTilEuEosAdminService(fagsakService, endreSakService, lovligeKombinasjonerSaksbehandlingService)
        every {
            lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), any(), any(), any(), any())
        } throws FunksjonellException("ugyldig")
    }

    @Test
    fun `dryRun endrer ikke saken`() {
        lagSak(SAKSNUMMER, TRYGDEAVTALE, UTSENDT_ARBEIDSTAKER)

        val resultat = service.endreTilEuEøs(listOf(SAKSNUMMER), dryRun = true)

        resultat.map { it.status } shouldContainExactly listOf(EndreSakstypeStatus.VIL_ENDRES)
        verify(exactly = 0) { endreSakService.endre(any(), any(), any(), any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Sakstyper::class, names = ["TRYGDEAVTALE", "FTRL"])
    fun `endrer sakstype til EU_EOS og beholder behandlingen uendret`(sakstype: Sakstyper) {
        lagSak(SAKSNUMMER, sakstype, UTSENDT_ARBEIDSTAKER)

        val resultat = service.endreTilEuEøs(listOf(SAKSNUMMER), dryRun = false)

        resultat.map { it.status } shouldContainExactly listOf(EndreSakstypeStatus.ENDRET)
        verify(exactly = 1) {
            endreSakService.endre(SAKSNUMMER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, FØRSTEGANG, UNDER_BEHANDLING, null)
        }
    }

    @Test
    fun `hopper over sak med gyldig kombinasjon`() {
        lagSak(SAKSNUMMER, TRYGDEAVTALE, YRKESAKTIV)
        every {
            lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), TRYGDEAVTALE, any(), YRKESAKTIV, any())
        } returns Unit

        val resultat = service.endreTilEuEøs(listOf(SAKSNUMMER), dryRun = false)

        resultat.map { it.status } shouldContainExactly listOf(EndreSakstypeStatus.HOPPET_OVER)
        verify(exactly = 0) { endreSakService.endre(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `hopper over sak som allerede er EU_EOS`() {
        lagSak(SAKSNUMMER, EU_EOS, UTSENDT_ARBEIDSTAKER)

        val resultat = service.endreTilEuEøs(listOf(SAKSNUMMER), dryRun = false)

        resultat.map { it.status } shouldContainExactly listOf(EndreSakstypeStatus.HOPPET_OVER)
        verify(exactly = 0) { endreSakService.endre(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `feil i en sak stopper ikke de andre`() {
        lagSak(SAKSNUMMER, TRYGDEAVTALE, UTSENDT_ARBEIDSTAKER)
        lagSak(ANNET_SAKSNUMMER, FTRL, UTSENDT_ARBEIDSTAKER)
        every { endreSakService.endre(SAKSNUMMER, any(), any(), any(), any(), any(), any()) } throws FunksjonellException("kan ikke endres")

        val resultat = service.endreTilEuEøs(listOf(SAKSNUMMER, ANNET_SAKSNUMMER), dryRun = false)

        resultat.map { it.status } shouldContainExactly listOf(EndreSakstypeStatus.FEILET, EndreSakstypeStatus.ENDRET)
    }

    private fun lagSak(saksnummer: String, sakstype: Sakstyper, behandlingstema: Behandlingstema) {
        val behandling = Behandling.forTest {
            id = 1L
            tema = behandlingstema
            type = FØRSTEGANG
            status = UNDER_BEHANDLING
        }
        val fagsak = Fagsak.forTest {
            medBruker()
            this.saksnummer = saksnummer
            type = sakstype
            tema = MEDLEMSKAP_LOVVALG
        }.apply { leggTilBehandling(behandling) }
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
    }

    companion object {
        private const val SAKSNUMMER = "MEL-1"
        private const val ANNET_SAKSNUMMER = "MEL-2"
    }
}
