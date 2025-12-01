package no.nav.melosys.service.behandling.jobb

import no.nav.melosys.domain.Behandling
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Repository for å finne behandlinger som potensielt er berørt av feilen i AvsluttArt13BehandlingJobb.
 *
 * Scenario 1 (X008/X006):
 * - Fagsak har status LOVVALG_AVKLART
 * - Behandlingsresultat er HENLEGGELSE (indikerer at saken ble annullert via X008/X006)
 * - Behandlingstema er BESLUTNING_LOVVALG_ANNET_LAND (artikkel 13-saker)
 *
 * Scenario 2 (Ny vurdering overskrevet):
 * - Fagsak har status LOVVALG_AVKLART
 * - Det finnes en FØRSTEGANG-behandling som er AVSLUTTET
 * - Det finnes en NY_VURDERING-behandling som er AVSLUTTET eller MIDLERTIDIG_LOVVALGSBESLUTNING
 * - NY_VURDERING ble registrert ETTER FØRSTEGANG
 */
interface RettOppFeilMedlPerioderRepository : CrudRepository<Behandling, Long> {

    // ==================== Scenario 1: X008/X006 ====================

    /**
     * Teller antall behandlinger for Scenario 1 (X008/X006).
     */
    @Query(
        """
        SELECT COUNT(b.id) FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN b.fagsak f
        WHERE f.status = 'LOVVALG_AVKLART'
        AND br.type = 'HENLEGGELSE'
        AND b.tema = 'BESLUTNING_LOVVALG_ANNET_LAND'
    """
    )
    fun countBehandlingerMedFeilStatus(): Long

    /**
     * Scenario 1: Finner behandlinger der SED ble invalidert (X008/X006) men MEDL-periode ble satt til endelig.
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
        ORDER BY b.id ASC
    """
    )
    fun finnBehandlingIderMedFeilStatus(startFraBehandlingId: Long, pageable: Pageable): List<Long>

    // ==================== Scenario 2: Ny vurdering ====================

    /**
     * Teller antall behandlinger for Scenario 2 (Ny vurdering overskrevet).
     */
    @Query(
        """
        SELECT COUNT(DISTINCT b.id) FROM Behandling b
        JOIN b.fagsak f
        WHERE f.status = 'LOVVALG_AVKLART'
        AND b.type = 'FØRSTEGANG'
        AND b.status = 'AVSLUTTET'
        AND b.tema IN ('BESLUTNING_LOVVALG_ANNET_LAND', 'BESLUTNING_NORSK_LOVVALG')
        AND EXISTS (
            SELECT 1 FROM Behandling nyVurdering
            WHERE nyVurdering.fagsak = f
            AND nyVurdering.type = 'NY_VURDERING'
            AND nyVurdering.status IN ('AVSLUTTET', 'MIDLERTIDIG_LOVVALGSBESLUTNING')
            AND nyVurdering.registrertDato > b.registrertDato
        )
    """
    )
    fun countBehandlingerMedPotensielleNyVurderingFeil(): Long

    /**
     * Scenario 2: Finner førstegangsbehandlinger der det finnes en nyere ny vurdering,
     * som kan ha blitt overskrevet av AvsluttArt13BehandlingJobb.
     * Returnerer kun ID-er for å unngå OOM ved lasting av mange entiteter.
     * Sortert etter id ASC for stabil paginering.
     *
     * Kriterier:
     * - Fagsak har status LOVVALG_AVKLART
     * - FØRSTEGANG-behandling er AVSLUTTET
     * - Behandlingstema er BESLUTNING_LOVVALG_ANNET_LAND eller BESLUTNING_NORSK_LOVVALG
     * - Det finnes en NY_VURDERING på samme fagsak som ble registrert etter førstegangsbehandlingen
     *
     * @param startFraBehandlingId Hent kun behandlinger med id > denne verdien (for paginering). Bruk 0 for å starte fra begynnelsen.
     * @param pageable Begrenser antall resultater (kun size brukes, offset ignoreres)
     */
    @Query(
        """
        SELECT DISTINCT b.id FROM Behandling b
        JOIN b.fagsak f
        WHERE f.status = 'LOVVALG_AVKLART'
        AND b.type = 'FØRSTEGANG'
        AND b.status = 'AVSLUTTET'
        AND b.tema IN ('BESLUTNING_LOVVALG_ANNET_LAND', 'BESLUTNING_NORSK_LOVVALG')
        AND b.id > :startFraBehandlingId
        AND EXISTS (
            SELECT 1 FROM Behandling nyVurdering
            WHERE nyVurdering.fagsak = f
            AND nyVurdering.type = 'NY_VURDERING'
            AND nyVurdering.status IN ('AVSLUTTET', 'MIDLERTIDIG_LOVVALGSBESLUTNING')
            AND nyVurdering.registrertDato > b.registrertDato
        )
        ORDER BY b.id ASC
    """
    )
    fun finnBehandlingIderMedPotensielleNyVurderingFeil(startFraBehandlingId: Long, pageable: Pageable): List<Long>
}
