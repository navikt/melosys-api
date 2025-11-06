package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger
import no.nav.melosys.service.sak.FagsakService
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val log = KotlinLogging.logger {}

@Component
class ÅrsavregningIkkeSkattepliktigeFinner(
    private val ikkeSkattepliktigeRepository: ÅrsavregningIkkeSkattepliktigeRepository,
    private val årsavregningService: ÅrsavregningService,
    private val fagsakService: FagsakService
) {

    @Transactional(readOnly = true)
    fun finnSakerMedBehandlinger(fomDato: LocalDate, tomDato: LocalDate, onSakerMedFastsetting: () -> Unit = {}): List<SakMedBehandlinger> {
        val år = fomDato.year

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

        val kandidatSaksnumre = ikkeSkattepliktigeRepository.finnFTRLSaksnumre(fomDato, tomDato)

        return kandidatSaksnumre.mapNotNull { saksnummer ->
            val gjeldendeBehandlingsresultater = årsavregningService
                .hentGjeldendeBehandlingsresultaterForÅrsavregning(saksnummer, år)
                ?: return@mapNotNull null

            val behandlinger = listOfNotNull(
                gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedAvgiftspliktigPeriode?.hentBehandling(),
                gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedAvgift?.hentBehandling()
            ).distinct().sortedByDescending { it.endretDato }

            if (behandlinger.isEmpty()) return@mapNotNull null

            val fagsak = fagsakService.hentFagsak(saksnummer)
            SakMedBehandlinger(fagsak, behandlinger)
        }.also {
            println("STØRRELSE: ${it.size}, sakerMedFastsetting=${sakerMedFastsetting.size}")
            log.info { "Totalt fant ${it.size} saker for årsavregning ikke skattepliktig" }
        }
    }
}

interface ÅrsavregningIkkeSkattepliktigeRepository : CrudRepository<Behandling, Long> {
    @Query(
        """
        SELECT DISTINCT f.saksnummer
        FROM Fagsak f
        WHERE f.type = 'FTRL'
            AND f.status = 'LOVVALG_AVKLART'
            AND NOT EXISTS (
                SELECT 1 FROM Behandling b
                JOIN Behandlingsresultat br ON b.id = br.behandling.id
                JOIN br.årsavregning a
                WHERE b.fagsak = f
                    AND b.type = 'ÅRSAVREGNING'
                    AND a.aar = EXTRACT(YEAR FROM :fomDato)
            )
            AND NOT EXISTS (
                SELECT 1 FROM Behandling b
                JOIN Behandlingsresultat br ON b.id = br.behandling.id
                WHERE b.fagsak = f
                    AND b.status = 'AVSLUTTET'
                    AND br.type = 'OPPHØRT'
                    AND b.id = (
                        SELECT MAX(b2.id)
                        FROM Behandling b2
                        JOIN Behandlingsresultat br2 ON b2.id = br2.behandling.id
                        WHERE b2.fagsak = f
                            AND b2.status = 'AVSLUTTET'
                    )
            )
            AND EXISTS (
                SELECT 1 FROM Behandling b
                JOIN Behandlingsresultat br ON b.id = br.behandling.id
                JOIN br.medlemskapsperioder mp
                JOIN mp.trygdeavgiftsperioder tap
                JOIN tap.grunnlagSkatteforholdTilNorge stn
                WHERE b.fagsak = f
                    AND b.status = 'AVSLUTTET'
                    AND br.type = 'MEDLEM_I_FOLKETRYGDEN'
                    AND stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG'
                    AND tap.periodeFra <= :tomDato
                    AND tap.periodeTil >= :fomDato
            )
        """
    )
    fun finnFTRLSaksnumre(
        @Param("fomDato") fomDato: LocalDate,
        @Param("tomDato") tomDato: LocalDate,
    ): List<String>

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
