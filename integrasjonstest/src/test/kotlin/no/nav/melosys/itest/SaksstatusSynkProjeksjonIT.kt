package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.service.sak.SkjemaSaksstatusSyncService
import no.nav.melosys.skjema.types.common.Saksstatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * Verifiserer SAKSSTATUS_SYNK_PROJEKSJON (JPQL) mot ekte database: utledet skjema-status er en
 * REN funksjon av fagsakstatus (produkteierbeslutning 2026-07-21) — sakens behandlinger inngår
 * ikke i projeksjonen. Se javadoc på projeksjonskonstanten i [SkjemaSakMappingRepository].
 */
class SaksstatusSynkProjeksjonIT(
    @Autowired private val skjemaSakMappingRepository: SkjemaSakMappingRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository
) : DataJpaTestBase() {

    private fun lagSkjemakobletFagsak(saksstatus: Saksstatuser, vararg behandlinger: Pair<Behandlingstyper, Behandlingsstatus>): Fagsak {
        val lagretFagsak = fagsakRepository.save(Fagsak.forTest { status = saksstatus })
        behandlinger.forEach { (behandlingstype, behandlingsstatus) ->
            behandlingRepository.save(Behandling.forTest {
                fagsak = lagretFagsak
                type = behandlingstype
                status = behandlingsstatus
            })
        }
        skjemaSakMappingRepository.save(SkjemaSakMapping(UUID.randomUUID(), lagretFagsak, null, "{}"))
        return lagretFagsak
    }

    private fun utledSkjemaSaksstatus(fagsak: Fagsak): Saksstatus {
        val rad = skjemaSakMappingRepository.finnSaksstatusSynkRaderForSaksnummer(fagsak.saksnummer).single()
        return SkjemaSaksstatusSyncService.tilSkjemaSaksstatus(rad.saksstatus)
    }

    @Test
    fun `sak i OPPRETTET gir MOTTATT`() {
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.OPPRETTET,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.UNDER_BEHANDLING
        )

        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.MOTTATT
    }

    @Test
    fun `gjenbrukt sak i LOVVALG_AVKLART gir AVSLUTTET selv med åpen søknadsbehandling`() {
        // Revurdering/ny søknad på ferdigbehandlet sak endrer ikke utledet status — at
        // ferdigbehandlede innsendinger ikke resettes garanteres av monotoni-guarden i skjema-api
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.LOVVALG_AVKLART,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.AVSLUTTET,
            Behandlingstyper.NY_VURDERING to Behandlingsstatus.UNDER_BEHANDLING
        )

        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.AVSLUTTET
    }

    @Test
    fun `avsluttet sak gir AVSLUTTET uavhengig av åpne interne behandlinger`() {
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.AVSLUTTET,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.AVSLUTTET,
            Behandlingstyper.ÅRSAVREGNING to Behandlingsstatus.UNDER_BEHANDLING
        )

        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.AVSLUTTET
    }
}
