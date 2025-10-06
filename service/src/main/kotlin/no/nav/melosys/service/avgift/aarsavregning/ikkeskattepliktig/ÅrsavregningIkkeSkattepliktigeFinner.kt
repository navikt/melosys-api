package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger
import no.nav.melosys.service.logInfoIf
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val log = KotlinLogging.logger {}

@Component
class ÅrsavregningIkkeSkattepliktigeFinner(
    private val ikkeSkattepliktigeRepository: ÅrsavregningIkkeSkattepliktigeRepository
) {
    fun finnSakerMedBehandlinger(fomDato: LocalDate, tomDato: LocalDate, onSakerMedFastsetting: () -> Unit = {}): List<SakMedBehandlinger> {
        val sakerMedFastsetting = ikkeSkattepliktigeRepository
            .finnBehandlingerMedTidligereÅrsavregningOgFastsetting(
                fomDato.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                tomDato.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
            )
            .groupBy { it.fagsak.saksnummer }
            .mapValues { it.value.sortedByDescending { b -> b.endretDato } }
            .also {
                log.info { "Fant ${it.size} saker med tidligere årsavregning med fastsetting" }
                onSakerMedFastsetting()
            }

        return ikkeSkattepliktigeRepository.finnFTRLBehandlinger(fomDato, tomDato)
            .filterNot {
                sakerMedFastsetting[it.fagsak.saksnummer]?.let { behandlinger ->
                    log.info { "Ekskluderer sak ${it.fagsak.saksnummer} pga behandlinger: ${behandlinger.map { b -> b.id }}" }
                    true
                } ?: false
            }
            .groupBy { it.fagsak }
            .map { (fagsak, behandlinger) ->
                SakMedBehandlinger(fagsak, behandlinger.sortedByDescending { it.endretDato })
            }.filterNot {
                it.harÅrsavregning() logInfoIf {
                    "Ekskluderer sak ${it.sak.saksnummer} siden saken allerede har behandling med type:ÅRSAVREGNING"
                }
            }
    }
}

interface ÅrsavregningIkkeSkattepliktigeRepository : CrudRepository<Behandling, Long> {
    @Query(
        """
        select distinct b
        FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN br.medlemskapsperioder mp
        JOIN br.vedtakMetadata vm
        JOIN b.fagsak f
        WHERE f.type = 'FTRL'
            and f.status = 'LOVVALG_AVKLART'
            and mp.fom >= :fomDato
            and mp.tom <= :tomDato
            and EXISTS (
                SELECT 1 FROM mp.trygdeavgiftsperioder tap
                JOIN tap.grunnlagSkatteforholdTilNorge stn
                WHERE stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG'
            )
            """
    )
    fun finnFTRLBehandlinger(
        @Param("fomDato") fomDato: LocalDate,
        @Param("tomDato") tomDato: LocalDate,
    ): List<Behandling>

    @Query(
        """
        select distinct b
        FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN b.fagsak f
        WHERE f.type = 'FTRL'
            and b.type = 'ÅRSAVREGNING'
            and br.type = 'FASTSATT_TRYGDEAVGIFT'
            and br.registrertDato between :fomDato and :tomDato
        """
    )
    fun finnBehandlingerMedTidligereÅrsavregningOgFastsetting(
        @Param("fomDato") fomDato: Instant,
        @Param("tomDato") tomDato: Instant,
    ): List<Behandling>
}

