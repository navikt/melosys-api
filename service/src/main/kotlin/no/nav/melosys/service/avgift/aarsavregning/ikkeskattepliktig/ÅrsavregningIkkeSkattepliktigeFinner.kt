package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig.ÅrsavregningIkkeSkattepliktigeProsessGenerator.SakMedBehandlinger
import no.nav.melosys.service.sak.FagsakService
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
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
    fun finnSakerMedBehandlinger(fomDato: LocalDate, tomDato: LocalDate): List<SakMedBehandlinger> {
        val år = fomDato.year

        val kandidatSaksnumre = ikkeSkattepliktigeRepository.finnFTRLSaksnumre(
            fomDato,
            tomDato,
            fomDato.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            tomDato.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
        )

        return kandidatSaksnumre.mapNotNull { saksnummer ->
            val gjeldendeBehandlingsresultater = årsavregningService
                .hentGjeldendeBehandlingsresultaterForÅrsavregning(saksnummer, år)
                ?: return@mapNotNull null

            // Sjekk om nyeste behandling har SKATTEPLIKTIG perioder
            val sisteBehandlingsresultat = gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedAvgiftspliktigPeriode
            val harSkattepliktig = sisteBehandlingsresultat?.trygdeavgiftsperioder
                ?.any { it.grunnlagSkatteforholdTilNorge?.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
                ?: false

            if (harSkattepliktig) {
                log.debug { "Ekskluderer sak $saksnummer: nyeste behandling har SKATTEPLIKTIG periode" }
                return@mapNotNull null
            }

            val behandlinger = listOfNotNull(
                gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedAvgiftspliktigPeriode?.hentBehandling(),
                gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedAvgift?.hentBehandling()
            ).distinct().sortedByDescending { it.endretDato }

            if (behandlinger.isEmpty()) return@mapNotNull null

            val fagsak = fagsakService.hentFagsak(saksnummer)
            SakMedBehandlinger(fagsak, behandlinger)
        }.also {
            log.info { "Totalt fant ${it.size} saker for årsavregning ikke skattepliktig" }
        }
    }
}

interface ÅrsavregningIkkeSkattepliktigeRepository : Repository<Fagsak, String> {
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
            AND NOT EXISTS (
                SELECT 1 FROM Behandling b
                JOIN Behandlingsresultat br ON b.id = br.behandling.id
                WHERE b.fagsak = f
                    AND b.type = 'ÅRSAVREGNING'
                    AND br.type = 'FASTSATT_TRYGDEAVGIFT'
                    AND br.registrertDato between :fomInstant and :tomInstant
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
        @Param("fomInstant") fomInstant: Instant,
        @Param("tomInstant") tomInstant: Instant
    ): List<String>
}
