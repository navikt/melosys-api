package no.nav.melosys.service.behandling.jobb

import no.nav.melosys.domain.Behandling
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Repository for å finne behandlinger som er berørt av feilen i AvsluttArt13BehandlingJobb.
 *
 * Scenario 1 (X008/X006):
 * - Fagsak har status LOVVALG_AVKLART
 * - Behandlingsresultat er HENLEGGELSE (indikerer at saken ble annullert via X008/X006)
 * - Behandlingstema er BESLUTNING_LOVVALG_ANNET_LAND (artikkel 13-saker)
 * - Det finnes INGEN NY_VURDERING på fagsaken (for å unngå overlapp med Scenario 2)
 */
interface RettOppFeilMedlPerioderRepository : CrudRepository<Behandling, Long> {

    /**
     * Teller antall behandlinger for Scenario 1 (X008/X006) uten ny vurdering.
     */
    @Query(
        """
        SELECT COUNT(b.id) FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN b.fagsak f
        WHERE f.status = 'LOVVALG_AVKLART'
        AND br.type = 'HENLEGGELSE'
        AND b.tema = 'BESLUTNING_LOVVALG_ANNET_LAND'
        AND NOT EXISTS (
            SELECT 1 FROM Behandling nyv
            WHERE nyv.fagsak = f
            AND nyv.type = 'NY_VURDERING'
        )
    """
    )
    fun countBehandlingerMedFeilStatus(): Long

    /**
     * Scenario 1: Finner behandlinger der SED ble invalidert (X008/X006) men MEDL-periode ble satt til endelig.
     * Ekskluderer fagsaker som har NY_VURDERING for å unngå overlapp med Scenario 2.
     * Returnerer kun ID-er for å unngå OOM ved lasting av mange entiteter.
     * Sortert etter id ASC for stabil paginering.
     *
     * @param startFraBehandlingId Hent kun behandlinger med id > denne verdien (for paginering). Bruk 0 for å starte fra begynnelsen.
     * @param pageable Begrenser antall resultater (kun size brukes, offset ignoreres)
     */
    @Query(
        """
        SELECT b.id FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN b.fagsak f
        WHERE f.status = 'LOVVALG_AVKLART'
        AND br.type = 'HENLEGGELSE'
        AND b.tema = 'BESLUTNING_LOVVALG_ANNET_LAND'
        AND b.id > :startFraBehandlingId
        AND NOT EXISTS (
            SELECT 1 FROM Behandling nyv
            WHERE nyv.fagsak = f
            AND nyv.type = 'NY_VURDERING'
        )
        ORDER BY b.id ASC
    """
    )
    fun finnBehandlingIderMedFeilStatus(startFraBehandlingId: Long, pageable: Pageable): List<Long>
}
