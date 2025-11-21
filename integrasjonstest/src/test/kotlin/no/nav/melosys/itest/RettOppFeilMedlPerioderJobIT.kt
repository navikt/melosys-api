package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.BehandlingsresultatTestFactory
import no.nav.melosys.domain.saksopplysning
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.SedInformasjon
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiApi
import no.nav.melosys.melosysmock.melosyseessi.MelosysEessiRepo
import no.nav.melosys.melosysmock.melosyseessi.Saksrelasjon
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.jobb.RettOppFeilMedlPerioderJob
import no.nav.melosys.service.behandling.jobb.RettOppFeilMedlPerioderRepository
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RettOppFeilMedlPerioderJobIT(
    @Autowired private val rettOppFeilMedlPerioderRepository: RettOppFeilMedlPerioderRepository,
    @Autowired private val rettOppFeilMedlPerioderJob: RettOppFeilMedlPerioderJob,
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

    @Nested
    @DisplayName("Job execution")
    inner class JobExecution {
        @Test
        fun `skal identifisere sak som skal rettes opp i dryRun modus`() {
            val saksnummer = "MEL-JOB-TEST"
            val rinaSaksnummer = "RINA-123"
            val rinaDokumentID = "DOC-456"
            val gsakSaksnummer = 789L

            // Opprett behandling med SedDokument
            lagBehandlingsresultatMedSed(saksnummer, gsakSaksnummer, rinaSaksnummer, rinaDokumentID)

            // Sett opp EESSI mock med invalidert SED
            setupEessiMock(gsakSaksnummer, rinaSaksnummer, rinaDokumentID, sedStatus = "AVBRUTT")

            // Kjør jobb i dryRun modus (må clearce ThreadLocalAccessInfo først, og re-registrere etterpå)
            ThreadLocalAccessInfo.afterExecuteProcess(randomUUID)
            rettOppFeilMedlPerioderJob.kjør(dryRun = true)
            ThreadLocalAccessInfo.beforeExecuteProcess(randomUUID, "Test")

            // Verifiser at jobben fant saken
            val jobStatus = rettOppFeilMedlPerioderJob.status()
            (jobStatus["skalRettesOpp"] as Int) shouldBe 1
            (jobStatus["rettetOpp"] as Int) shouldBe 0 // dryRun, ingen faktiske endringer

            // Verifiser at saksstatus IKKE er endret (dryRun)
            val fagsak = fagsakRepository.findBySaksnummer(saksnummer).get()
            fagsak.status.shouldBe(Saksstatuser.LOVVALG_AVKLART)
        }

        @Test
        fun `skal hoppe over sak som ikke er invalidert i EESSI`() {
            val saksnummer = "MEL-IKKE-INVALIDERT"
            val rinaSaksnummer = "RINA-999"
            val rinaDokumentID = "DOC-888"
            val gsakSaksnummer = 777L

            lagBehandlingsresultatMedSed(saksnummer, gsakSaksnummer, rinaSaksnummer, rinaDokumentID)

            // Sett opp EESSI mock med aktiv (ikke invalidert) SED
            setupEessiMock(gsakSaksnummer, rinaSaksnummer, rinaDokumentID, sedStatus = "SENDT")

            // Må clearce ThreadLocalAccessInfo før jobben kjøres, og re-registrere etterpå
            ThreadLocalAccessInfo.afterExecuteProcess(randomUUID)
            rettOppFeilMedlPerioderJob.kjør(dryRun = true)
            ThreadLocalAccessInfo.beforeExecuteProcess(randomUUID, "Test")

            val jobStatus = rettOppFeilMedlPerioderJob.status()
            (jobStatus["ikkeInvalidertIEessi"] as Int) shouldBe 1
            (jobStatus["skalRettesOpp"] as Int) shouldBe 0
        }

        private fun setupEessiMock(gsakSaksnummer: Long, rinaSaksnummer: String, rinaDokumentID: String, sedStatus: String) {
            // Legg til saksrelasjon
            MelosysEessiApi.saksrelasjoner.add(
                Saksrelasjon(gsakSaksnummer = gsakSaksnummer, rinaSaksnummer = rinaSaksnummer)
            )

            // Legg til BUC med SED
            val sedInfo = SedInformasjon(
                bucId = rinaSaksnummer,
                sedId = rinaDokumentID,
                opprettetDato = LocalDate.now(),
                sistOppdatert = LocalDate.now(),
                sedType = "A003",
                status = sedStatus,
                rinaUrl = null
            )
            val bucInfo = BucInformasjon(
                id = rinaSaksnummer,
                erÅpen = true,
                bucType = "LA_BUC_02",
                opprettetDato = LocalDate.now(),
                mottakerinstitusjoner = null,
                seder = listOf(sedInfo)
            )
            MelosysEessiRepo.opprettBucinformasjon(bucInfo)
        }

        private fun lagBehandlingsresultatMedSed(
            saksnummer: String,
            gsakSaksnummer: Long,
            rinaSaksnummer: String,
            rinaDokumentID: String
        ) {
            val sedDokument = SedDokument().apply {
                this.rinaSaksnummer = rinaSaksnummer
                this.rinaDokumentID = rinaDokumentID
            }

            Behandlingsresultat.forTest {
                behandling {
                    tema = Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
                    status = Behandlingsstatus.AVSLUTTET
                    fagsak {
                        this.saksnummer = saksnummer
                        this.gsakSaksnummer = gsakSaksnummer
                        type = Sakstyper.EU_EOS
                        status = Saksstatuser.LOVVALG_AVKLART
                    }
                    saksopplysning {
                        type = SaksopplysningType.SEDOPPL
                        dokument = sedDokument
                    }
                }
                type = Behandlingsresultattyper.HENLEGGELSE
            }.also {
                val fagsak = it.hentBehandling().fagsak
                it.hentBehandling().fagsak.behandlinger.clear()

                fagsakRepository.save(fagsak)
                addCleanUpAction {
                    slettSakMedAvhengigheter(saksnummer)
                    MelosysEessiApi.saksrelasjoner.clear()
                    MelosysEessiRepo.clear()
                }
                behandlingsresultatRepository.save(it)
            }
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
