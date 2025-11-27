package no.nav.melosys.service.behandling.jobb

import no.nav.melosys.domain.Behandling
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

    /**
     * Scenario 1: Finner behandlinger der SED ble invalidert (X008/X006) men MEDL-periode ble satt til endelig.
     */
    @Query("""
        SELECT b FROM Behandlingsresultat br
        JOIN br.behandling b
        JOIN b.fagsak f
        WHERE f.status = 'LOVVALG_AVKLART'
        AND br.type = 'HENLEGGELSE'
        AND b.tema = 'BESLUTNING_LOVVALG_ANNET_LAND'
        ORDER BY b.endretDato DESC
    """)
    fun finnBehandlingerMedFeilStatus(): List<Behandling>

    /**
     * Scenario 2: Finner førstegangsbehandlinger der det finnes en nyere ny vurdering,
     * som kan ha blitt overskrevet av AvsluttArt13BehandlingJobb.
     *
     * Kriterier:
     * - Fagsak har status LOVVALG_AVKLART
     * - FØRSTEGANG-behandling er AVSLUTTET
     * - Behandlingstema er BESLUTNING_LOVVALG_ANNET_LAND eller BESLUTNING_NORSK_LOVVALG
     * - Det finnes en NY_VURDERING på samme fagsak som ble registrert etter førstegangsbehandlingen
     */
    @Query("""
        SELECT DISTINCT b FROM Behandling b
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
        ORDER BY b.endretDato DESC
    """)
    fun finnBehandlingerMedPotensielleNyVurderingFeil(): List<Behandling>
}
