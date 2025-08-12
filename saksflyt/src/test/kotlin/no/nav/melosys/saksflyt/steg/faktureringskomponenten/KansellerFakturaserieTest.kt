package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.saksflyt.steg.fakturering.KansellerFakturaserie
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
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

        val opprinneligBehandling = Behandling.forTest {
            id = opprinneligBehandlingId
            registrertDato = Instant.now().minusSeconds(1333337)
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                this.opprinneligBehandling = opprinneligBehandling
                registrertDato = Instant.now()
                fagsak {
                    leggTilBehandling(opprinneligBehandling)
                }
            }
            medData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
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
        val behandlingHenvendelseId = 333L
        val nyesteBehandlingId = 456L
        val fakturaReferanse = "FADKFOGMV123"
        val SAKSBEHANDLER_IDENT = "S123456"

        val nyesteBehandlingUtenFakturaserieReferanse = Behandling.forTest {
            id = nyesteBehandlingId
            registrertDato = Instant.now()
            type = Behandlingstyper.NY_VURDERING
            fagsak = Fagsak.forTest {
                behandling {
                    id = opprinneligBehandlingId
                    type = Behandlingstyper.FØRSTEGANG
                    registrertDato = Instant.now().minusSeconds(1333337)
                }
                behandling {
                    id = behandlingHenvendelseId
                    type = Behandlingstyper.HENVENDELSE
                    registrertDato = Instant.now().minusSeconds(133337)
                }
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            medBehandling(nyesteBehandlingUtenFakturaserieReferanse)
            medData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
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
