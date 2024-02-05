package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.saksflyt.steg.fakturering.KansellerFakturaserie
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class KansellerFakturaserieTest {


    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService
    @RelaxedMockK
    lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer
    @RelaxedMockK
    lateinit var trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService
    @InjectMockKs
    lateinit var kansellerFakturaserie: KansellerFakturaserie

    @Test
    fun `kanseller fakturaserie`() {
        val behandlingId = 123L
        val opprinneligBehandlingId = 456L
        val fakturaReferanse = "FADKFOGMV123"
        val nyFakturaReferanse = "456FRKVLFVS"
        val SAKSBEHANDLER_IDENT = "S123456"

        val prosessinstans = Prosessinstans().apply {
            this.behandling = Behandling().apply {
                id = behandlingId
                opprinneligBehandling = Behandling().apply {
                    id = opprinneligBehandlingId
                }
            }
            setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = behandlingId
            fakturaserieReferanse = fakturaReferanse
        }

        val nyFakturaserieResponseDto = NyFakturaserieResponseDto(nyFakturaReferanse)

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandlingId) } returns behandlingsresultat
        every { faktureringskomponentenConsumer.kansellerFakturaserie(fakturaReferanse, SAKSBEHANDLER_IDENT) } returns nyFakturaserieResponseDto
        every { trygdeavgiftOppsummeringService.harTrygdeavgiftOgBestiltFaktura(behandlingsresultat) } returns true


        kansellerFakturaserie.utfør(prosessinstans)

        verify { behandlingsresultatService.lagre(behandlingsresultat) }
    }

}
