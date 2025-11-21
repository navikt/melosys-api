package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.BehandlingsresultatTestFactory
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.jobb.RettOppFeilMedlPerioderRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RettOppFeilMedlPerioderJobIT(
    @Autowired private val rettOppFeilMedlPerioderRepository: RettOppFeilMedlPerioderRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository
) : MockServerTestBaseWithProsessManager() {

    @Nested
    @DisplayName("Repository query")
    inner class RepositoryQuery {
        @Test
        fun `skal finne behandling med LOVVALG_AVKLART, HENLEGGELSE og BESLUTNING_LOVVALG_ANNET_LAND`() {
            val saksnummer = "MEL-FEIL-STATUS"

            lagBehandlingsresultat(saksnummer)

            val behandlinger = rettOppFeilMedlPerioderRepository.finnBehandlingerMedFeilStatus()

            behandlinger.filter { it.fagsak.saksnummer == saksnummer }
                .size shouldBe 1
        }

        @Test
        fun `skal ikke finne behandling med korrekt ANNULLERT status`() {
            val saksnummer = "MEL-KORREKT-STATUS"

            lagBehandlingsresultat(saksnummer) {
                behandling {
                    fagsak {
                        status = Saksstatuser.ANNULLERT
                    }
                }
            }

            val behandlinger = rettOppFeilMedlPerioderRepository.finnBehandlingerMedFeilStatus()

            behandlinger.filter { it.fagsak.saksnummer == saksnummer }
                .size shouldBe 0
        }

        @Test
        fun `skal ikke finne behandling med annet tema enn BESLUTNING_LOVVALG_ANNET_LAND`() {
            val saksnummer = "MEL-ANNET-TEMA"

            lagBehandlingsresultat(saksnummer) {
                behandling {
                    tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
                }
            }

            val behandlinger = rettOppFeilMedlPerioderRepository.finnBehandlingerMedFeilStatus()

            behandlinger.filter { it.fagsak.saksnummer == saksnummer }
                .size shouldBe 0
        }

        @Test
        fun `skal ikke finne behandling med annet behandlingsresultat enn HENLEGGELSE`() {
            val saksnummer = "MEL-IKKE-HENLEGGELSE"

            lagBehandlingsresultat(saksnummer) {
                type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            }

            val behandlinger = rettOppFeilMedlPerioderRepository.finnBehandlingerMedFeilStatus()

            behandlinger.filter { it.fagsak.saksnummer == saksnummer }
                .size shouldBe 0
        }
    }

    private fun lagBehandlingsresultat(
        saksnummer: String,
        block: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
        behandling {
            tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
            status = Behandlingsstatus.AVSLUTTET
            fagsak {
                this.saksnummer = saksnummer
                type = Sakstyper.EU_EOS
                status = Saksstatuser.LOVVALG_AVKLART
            }
        }
        type = Behandlingsresultattyper.HENLEGGELSE
        block()
    }.also {
        val fagsak = it.hentBehandling().fagsak
        it.hentBehandling().fagsak.behandlinger.clear()

        fagsakRepository.save(fagsak)
        addCleanUpAction { slettSakMedAvhengigheter(saksnummer) }
        behandlingsresultatRepository.save(it)
    }
}
