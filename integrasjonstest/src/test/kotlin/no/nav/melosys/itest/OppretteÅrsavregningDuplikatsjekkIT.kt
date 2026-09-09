package no.nav.melosys.itest

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/** Verifiserer mot ekte DB at [ÅrsavregningService.harAktivÅrsavregningForÅr] er konsistent med SQL-guarden i [AarsavregningRepository]. */
class OppretteÅrsavregningDuplikatsjekkIT(
    @Autowired private val årsavregningService: ÅrsavregningService,
    @Autowired private val aarsavregningRepository: AarsavregningRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
) : MockServerTestBaseWithProsessManager() {

    @Test
    fun `åpen ÅRSAVREGNING uten år (mangler aarsavregning-rad) blokkerer ikke - gate og SQL-guard er enige`() {
        val saksnummer = "MEL-8161-UTEN-AAR"
        // Åpen ÅRSAVREGNING-behandling UTEN årsavregning-rad (år ikke satt)
        lagÅrsavregningBehandling(saksnummer, medÅr = null, resultattype = Behandlingsresultattyper.IKKE_FASTSATT)
        val behandlingId = årsavregningBehandlingId(saksnummer)

        withClue("Gaten skal ikke regne en år-løs behandling som eksisterende årsavregning for $ÅR") {
            årsavregningService.harAktivÅrsavregningForÅr(saksnummer, ÅR).shouldBeFalse()
        }
        withClue("SQL-guarden teller heller ikke en år-løs behandling (ingen aarsavregning-rad)") {
            aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(behandlingId, ÅR) shouldBe 0
        }
    }

    @Test
    fun `år-satt aktiv ÅRSAVREGNING med ikke-IKKE_FASTSATT type - ny gate fanger den, gammel gate gjorde ikke (feilstien)`() {
        val saksnummer = "MEL-8161-FASTSATT-TYPE"
        // Åpen (ikke AVSLUTTET) ÅRSAVREGNING MED år, men behandlingsresultattype != IKKE_FASTSATT
        lagÅrsavregningBehandling(saksnummer, medÅr = ÅR, resultattype = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
        val behandlingId = årsavregningBehandlingId(saksnummer)

        withClue("GAMMEL gate (IKKE_FASTSATT-filter) bommer på en år-satt behandling med annen resultattype") {
            årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, ÅR, Behandlingsresultattyper.IKKE_FASTSATT).shouldBeEmpty()
        }
        withClue("SQL-guarden teller den likevel (kun status != AVSLUTTET) → den gamle stien ville kastet") {
            aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(behandlingId, ÅR) shouldBeGreaterThan 0
        }
        withClue("NY gate er enig med SQL-guarden og blokkerer → ingen «opprett-så-kast»") {
            årsavregningService.harAktivÅrsavregningForÅr(saksnummer, ÅR).shouldBeTrue()
        }
    }

    @Test
    fun `år-satt aktiv ÅRSAVREGNING med IKKE_FASTSATT type blokkerer fortsatt (normal regel uendret)`() {
        val saksnummer = "MEL-8161-IKKE-FASTSATT"
        lagÅrsavregningBehandling(saksnummer, medÅr = ÅR, resultattype = Behandlingsresultattyper.IKKE_FASTSATT)

        withClue("Både gammel og ny gate skal fange en år-satt IKKE_FASTSATT årsavregning for samme år") {
            årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, ÅR, Behandlingsresultattyper.IKKE_FASTSATT).shouldNotBeEmpty()
            årsavregningService.harAktivÅrsavregningForÅr(saksnummer, ÅR).shouldBeTrue()
        }
    }

    private fun årsavregningBehandlingId(saksnummer: String): Long =
        fagsakRepository.findBySaksnummer(saksnummer).shouldBePresent()
            .behandlinger.first { it.type == Behandlingstyper.ÅRSAVREGNING }.id

    /** Persisterer én åpen ÅRSAVREGNING-behandling (evt. med aarsavregning-rad) på en egen FTRL-sak. */
    private fun lagÅrsavregningBehandling(saksnummer: String, medÅr: Int?, resultattype: Behandlingsresultattyper) {
        Behandlingsresultat.forTest {
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = resultattype
            fastsattAvLand = Land_iso2.NO
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
            }
            if (medÅr != null) {
                årsavregning { aar = medÅr }
            }
            behandling {
                type = Behandlingstyper.ÅRSAVREGNING
                status = Behandlingsstatus.OPPRETTET
                tema = Behandlingstema.YRKESAKTIV
                fagsak {
                    this.saksnummer = saksnummer
                    type = Sakstyper.FTRL
                    status = Saksstatuser.LOVVALG_AVKLART
                }
            }
        }.also {
            val fagsak = it.hentBehandling().fagsak
            // Clear bidirectional relationship to prevent Hibernate cascade conflicts when
            // saving Fagsak and Behandlingsresultat separately (samme mønster som ÅrsavregningIkkeSkattepliktigeIT)
            it.hentBehandling().fagsak.behandlinger.clear()
            fagsakRepository.save(fagsak)
            behandlingsresultatRepository.save(it)
        }
    }

    companion object {
        private val ÅR = LocalDate.now().year - 1
    }
}
