package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class JournalfoeringReplikeringIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val unleash: FakeUnleash
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @Test
    fun journalførOgOpprettAndregangsBehandling_replikerBehandling_replikerBehandlingProsessStegBlirKjørt() {
        unleash.enable("melosys.behandle_alle_saker")

        val journalfoeringOpprettDto = defaultJournalføringDto().apply {
            fagsak.sakstype = Sakstyper.EU_EOS.kode
            fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
            behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
            behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
        }
        val prosessinstans = journalførOgVentTilProsesserErFerdige(journalfoeringOpprettDto)
        val behandling = prosessinstans.behandling

        behandling.status = Behandlingsstatus.AVSLUTTET
        behandlingRepository.save(behandling)

        val journalfoeringTilordneDto = lagJournalfoeringTilordneDto(
            saksnummer = behandling.fagsak.saksnummer,
            journalfoeringTilordneDto = defaultJournalfoeringTilordneDto().apply {
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
                behandlingstypeKode = Behandlingstyper.NY_VURDERING.kode
            }
        )


        executeAndWait(ProsessType.JFR_ANDREGANG_REPLIKER_BEHANDLING) {
            journalføringService.journalførOgOpprettAndregangsBehandling(journalfoeringTilordneDto)
        }


        val fagsak = fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer).get()
        fagsak.behandlinger
            .shouldHaveSize(2)
            .maxBy { it.id }
            .apply {
                type.shouldBe(Behandlingstyper.NY_VURDERING)
                opprinneligBehandling.id.shouldBe(behandling.id)
                initierendeJournalpostId.shouldBe(journalfoeringOpprettDto.journalpostID)
            }
            .behandlingsgrunnlag.behandlingsgrunnlagdata.shouldBeInstanceOf<Soeknad>()
            .shouldBeEqualToComparingFields(Soeknad().apply {
                soeknadsland.apply {
                    landkoder = listOf(Landkoder.IE.kode)
                    erUkjenteEllerAlleEosLand = false
                }
                periode = Periode(
                    periodeFOM,
                    periodeTOM
                )
            })
    }
}
