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
 * Verifiserer harAktivBehandling-semantikken i SAKSSTATUS_SYNK_PROJEKSJON (JPQL) mot ekte
 * database — den avviker BEVISST fra Behandling.erInaktiv(), se javadoc på projeksjonskonstanten
 * i [SkjemaSakMappingRepository].
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
        return SkjemaSaksstatusSyncService.tilSkjemaSaksstatus(rad.saksstatus, rad.harAktivBehandling)
    }

    @Test
    fun `behandling i art13-vinduet (MIDLERTIDIG_LOVVALGSBESLUTNING) regnes som aktiv og gir MOTTATT`() {
        // Art13-innvilgelse: behandlingen står i MIDLERTIDIG_LOVVALGSBESLUTNING i ~2 mnd uten at
        // fagsakstatus endres. Løpende synk tier i vinduet — massesynk må da også gi MOTTATT,
        // ellers divergerer de (Behandling.erInaktiv() ville regnet den som inaktiv → AVSLUTTET).
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.OPPRETTET,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        )

        val rad = skjemaSakMappingRepository.finnSaksstatusSynkRaderForSaksnummer(fagsak.saksnummer).single()
        rad.harAktivBehandling shouldBe true
        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.MOTTATT
    }

    @Test
    fun `åpen årsavregning på avsluttet sak teller ikke som aktiv behandling og gir AVSLUTTET`() {
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.AVSLUTTET,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.AVSLUTTET,
            Behandlingstyper.ÅRSAVREGNING to Behandlingsstatus.UNDER_BEHANDLING
        )

        val rad = skjemaSakMappingRepository.finnSaksstatusSynkRaderForSaksnummer(fagsak.saksnummer).single()
        rad.harAktivBehandling shouldBe false
        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.AVSLUTTET
    }

    @Test
    fun `åpen satsendring på avsluttet sak teller ikke som aktiv behandling og gir AVSLUTTET`() {
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.AVSLUTTET,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.AVSLUTTET,
            Behandlingstyper.SATSENDRING to Behandlingsstatus.OPPRETTET
        )

        val rad = skjemaSakMappingRepository.finnSaksstatusSynkRaderForSaksnummer(fagsak.saksnummer).single()
        rad.harAktivBehandling shouldBe false
        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.AVSLUTTET
    }

    @Test
    fun `åpen søknadsbehandling på gjenbrukt sak regnes som aktiv og gir MOTTATT`() {
        val fagsak = lagSkjemakobletFagsak(
            Saksstatuser.LOVVALG_AVKLART,
            Behandlingstyper.FØRSTEGANG to Behandlingsstatus.AVSLUTTET,
            Behandlingstyper.NY_VURDERING to Behandlingsstatus.UNDER_BEHANDLING
        )

        val rad = skjemaSakMappingRepository.finnSaksstatusSynkRaderForSaksnummer(fagsak.saksnummer).single()
        rad.harAktivBehandling shouldBe true
        utledSkjemaSaksstatus(fagsak) shouldBe Saksstatus.MOTTATT
    }
}
