package no.nav.melosys.service.avgift.aarsavregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.Test

class SkattepliktigAarsavregningOpprettelseServiceTest {

    private val prosessinstansService = mockk<ProsessinstansService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val årsavregningService = mockk<ÅrsavregningService>()
    private val trygdeavgiftMottakerService = mockk<TrygdeavgiftMottakerService>()

    private val service = SkattepliktigAarsavregningOpprettelseService(
        prosessinstansService,
        fagsakService,
        behandlingService,
        behandlingsresultatService,
        årsavregningService,
        trygdeavgiftMottakerService,
    )

    /**
     * En aktiv ÅRSAVREGNING-behandling uten rad i `aarsavregning` får `hentÅrsavregning()` til å
     * kaste. Den rå meldingen sier ikke hvilken behandling det gjelder, og begge flytene som treffer
     * dette må stoppes av et menneske — kastet skal derfor navngi behandlingen som må lukkes.
     */
    @Test
    fun `årløs aktiv årsavregning gir en feilmelding som navngir behandlingen`() {
        val fagsak = lagFagsakMedÅrsavregning()

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns
            Behandlingsresultat.forTest { }

        val feil = shouldThrow<TekniskException> {
            service.finnAktivÅrsavregningBehandling(fagsak, GJELDER_ÅR)
        }

        feil.message!! shouldContain BEHANDLING_ID.toString()
        feil.message!! shouldContain SAKSNUMMER
        // Handlingsanvisningen, ikke bare ordet «årløs»: uten den er meldingen en diagnose
        // operatøren ikke kan gjøre noe med.
        feil.message!! shouldContain "lukk den årløse behandlingen"
        feil.cause.shouldBeInstanceOf<IllegalStateException>()
    }

    private fun lagFagsakMedÅrsavregning() = Fagsak.forTest {
        saksnummer = SAKSNUMMER
        type = Sakstyper.EU_EOS
        tema = Sakstemaer.MEDLEMSKAP_LOVVALG
        status(Saksstatuser.OPPRETTET)
        behandling {
            id = BEHANDLING_ID
            type = Behandlingstyper.ÅRSAVREGNING
            status = Behandlingsstatus.VURDER_DOKUMENT
        }
    }

    companion object {
        const val SAKSNUMMER = "MEL-1"
        const val GJELDER_ÅR = 2023
        const val BEHANDLING_ID = 42L
    }
}
