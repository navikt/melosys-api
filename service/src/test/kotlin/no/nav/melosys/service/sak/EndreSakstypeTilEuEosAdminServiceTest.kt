package no.nav.melosys.service.sak

import io.kotest.matchers.collections.shouldBeEmpty
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
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.PENSJONIST
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.YRKESAKTIV
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.FØRSTEGANG
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

// TODO MELOSYS-8309: Engangsretting. Fjern når sakene er rettet i prod.
@ExtendWith(MockKExtension::class)
class EndreSakstypeTilEuEosAdminServiceTest {

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var fagsakService: FagsakService

    @RelaxedMockK
    private lateinit var endreSakService: EndreSakService

    @MockK
    private lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    private lateinit var service: EndreSakstypeTilEuEosAdminService

    private val kandidater = mutableMapOf<Sakstyper, MutableList<String>>()

    @BeforeEach
    fun setUp() {
        service = EndreSakstypeTilEuEosAdminService(
            fagsakRepository, fagsakService, endreSakService, lovligeKombinasjonerSaksbehandlingService
        )
        every { lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, any(), any(), null, null) } returns emptySet()
        every {
            lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), any(), any(), any(), any())
        } throws FunksjonellException("ugyldig")
        every { fagsakRepository.finnDigitalSoknadSaksnumreMedAktivBehandlingUtenforTemaer(any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `dryRun lister kandidater uten å endre`() {
        lagKandidat(SAKSNUMMER, TRYGDEAVTALE, UTSENDT_ARBEIDSTAKER)

        val resultat = service.endreTilEuEøs(dryRun = true)

        resultat.map { it.saksnummer to it.status } shouldContainExactly listOf(SAKSNUMMER to EndreSakstypeStatus.VIL_ENDRES)
        verify(exactly = 0) { endreSakService.endre(any(), any(), any(), any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Sakstyper::class, names = ["TRYGDEAVTALE", "FTRL"])
    fun `endrer sakstype til EU_EOS og beholder behandlingen uendret`(sakstype: Sakstyper) {
        lagKandidat(SAKSNUMMER, sakstype, UTSENDT_ARBEIDSTAKER)

        val resultat = service.endreTilEuEøs(dryRun = false)

        resultat.map { it.status } shouldContainExactly listOf(EndreSakstypeStatus.ENDRET)
        verify(exactly = 1) {
            endreSakService.endre(SAKSNUMMER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, FØRSTEGANG, UNDER_BEHANDLING, null)
        }
    }

    @Test
    fun `søker med temaene som er gyldige for sakstypen`() {
        every {
            lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, null, null)
        } returns setOf(YRKESAKTIV, PENSJONIST)

        service.endreTilEuEøs(dryRun = true)

        verify { fagsakRepository.finnDigitalSoknadSaksnumreMedAktivBehandlingUtenforTemaer(TRYGDEAVTALE, any(), setOf(YRKESAKTIV, PENSJONIST)) }
    }

    @Test
    fun `endrer ikke kandidat der kombinasjonen likevel er gyldig`() {
        lagKandidat(SAKSNUMMER, TRYGDEAVTALE, YRKESAKTIV)
        every {
            lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), TRYGDEAVTALE, any(), YRKESAKTIV, any())
        } returns Unit

        val resultat = service.endreTilEuEøs(dryRun = false)

        resultat.shouldBeEmpty()
        verify(exactly = 0) { endreSakService.endre(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `feil i en sak stopper ikke de andre`() {
        lagKandidat(SAKSNUMMER, TRYGDEAVTALE, UTSENDT_ARBEIDSTAKER)
        lagKandidat(ANNET_SAKSNUMMER, FTRL, UTSENDT_ARBEIDSTAKER)
        every { endreSakService.endre(SAKSNUMMER, any(), any(), any(), any(), any(), any()) } throws FunksjonellException("kan ikke endres")

        val resultat = service.endreTilEuEøs(dryRun = false)

        resultat.map { it.saksnummer to it.status } shouldContainExactly listOf(
            SAKSNUMMER to EndreSakstypeStatus.FEILET,
            ANNET_SAKSNUMMER to EndreSakstypeStatus.ENDRET
        )
    }

    private fun lagKandidat(saksnummer: String, sakstype: Sakstyper, behandlingstema: Behandlingstema) {
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

        val saksnumre = kandidater.getOrPut(sakstype) { mutableListOf() }.apply { add(saksnummer) }
        every { fagsakRepository.finnDigitalSoknadSaksnumreMedAktivBehandlingUtenforTemaer(sakstype, any(), any()) } returns saksnumre
    }

    companion object {
        private const val SAKSNUMMER = "MEL-1"
        private const val ANNET_SAKSNUMMER = "MEL-2"
    }
}
