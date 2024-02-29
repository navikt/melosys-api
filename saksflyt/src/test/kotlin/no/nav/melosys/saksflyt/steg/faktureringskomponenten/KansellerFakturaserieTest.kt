package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.saksflyt.steg.fakturering.KansellerFakturaserie
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class KansellerFakturaserieTest {

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    @InjectMockKs
    lateinit var kansellerFakturaserie: KansellerFakturaserie

    @Test
    fun `kanseller fakturaserie`() {
        val behandlingId = 123L
        val opprinneligBehandlingId = 456L
        val fakturaReferanse = "FADKFOGMV123"
        val SAKSBEHANDLER_IDENT = "S123456"

        val opprinneligBehandling = Behandling().apply {
            id = opprinneligBehandlingId
            registrertDato = Instant.now().minusSeconds(1333337)
        }
        val behandling = Behandling().apply {
            id = behandlingId
            this.opprinneligBehandling = opprinneligBehandling
            registrertDato = Instant.now()
        }

        val fagsak = Fagsak().apply {
            saksnummer = "123"
            behandlinger = mutableListOf(
                behandling,
                opprinneligBehandling
            )
        }
        behandling.fagsak = fagsak

        val prosessinstans = Prosessinstans().apply {
            this.behandling = behandling
            setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
        }
        val behandlingsresultatOpprinneligBehandling = Behandlingsresultat().apply {
            id = behandlingId
            fakturaserieReferanse = fakturaReferanse
        }

        val nyFakturaserieResponseDto = NyFakturaserieResponseDto(fakturaReferanse)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns Behandlingsresultat().apply { id = behandlingId }
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns behandlingsresultatOpprinneligBehandling
        every { faktureringskomponentenConsumer.kansellerFakturaserie(fakturaReferanse, SAKSBEHANDLER_IDENT) } returns nyFakturaserieResponseDto


        kansellerFakturaserie.utfør(prosessinstans)

        verify { behandlingsresultatService.lagre(behandlingsresultatOpprinneligBehandling) }
        verify { faktureringskomponentenConsumer.kansellerFakturaserie(fakturaReferanse, SAKSBEHANDLER_IDENT) }
    }


    @Test
    fun `kanseller fakturaserie med flere behandlinger med forskjellige typer`() {
        val opprinneligBehandlingId = 123L
        val behandlingHenvendelseId = 333L;
        val nyesteBehandlingId = 456L
        val fakturaReferanse = "FADKFOGMV123"
        val SAKSBEHANDLER_IDENT = "S123456"

        val opprinneligBehandling = Behandling().apply {
            id = opprinneligBehandlingId
            type = Behandlingstyper.FØRSTEGANG
            registrertDato = Instant.now().minusSeconds(1333337)
        }

        val behandlingHenvendelse = Behandling().apply {
            id = behandlingHenvendelseId
            type = Behandlingstyper.HENVENDELSE
            registrertDato = Instant.now().minusSeconds(133337)
        }

        val nyesteBehandlingUtenFakturaserieReferanse = Behandling().apply {
            id = nyesteBehandlingId
            registrertDato = Instant.now()
            type = Behandlingstyper.NY_VURDERING
        }

        val fagsak = Fagsak().apply {
            saksnummer = "123"
            behandlinger = mutableListOf(
                nyesteBehandlingUtenFakturaserieReferanse,
                opprinneligBehandling,
                behandlingHenvendelse
            )
        }
        nyesteBehandlingUtenFakturaserieReferanse.fagsak = fagsak

        val prosessinstans = Prosessinstans().apply {
            this.behandling = nyesteBehandlingUtenFakturaserieReferanse
            setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
        }
        val behandlingsresultatOpprinneligBehandling = Behandlingsresultat().apply {
            id = opprinneligBehandlingId
            fakturaserieReferanse = fakturaReferanse
        }

        val nyFakturaserieResponseDto = NyFakturaserieResponseDto(fakturaReferanse)

        every { behandlingsresultatService.hentBehandlingsresultat(nyesteBehandlingId) } returns Behandlingsresultat().apply {
            id = nyesteBehandlingId
        }
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingHenvendelseId) } returns Behandlingsresultat().apply {
            id = behandlingHenvendelseId
        }
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns behandlingsresultatOpprinneligBehandling
        every { faktureringskomponentenConsumer.kansellerFakturaserie(fakturaReferanse, SAKSBEHANDLER_IDENT) } returns nyFakturaserieResponseDto

        kansellerFakturaserie.utfør(prosessinstans)

        verify { behandlingsresultatService.lagre(behandlingsresultatOpprinneligBehandling) }
        verify { faktureringskomponentenConsumer.kansellerFakturaserie(fakturaReferanse, SAKSBEHANDLER_IDENT) }
    }

}
