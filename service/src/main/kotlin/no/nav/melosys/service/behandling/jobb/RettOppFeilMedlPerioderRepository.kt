package no.nav.melosys.service.behandling.jobb

import no.nav.melosys.domain.Behandling
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Repository for å finne behandlinger som potensielt er berørt av feilen i AvsluttArt13BehandlingJobb.
 *
 * Kriterier for berørte saker:
 * - Fagsak har status LOVVALG_AVKLART
 * - Behandlingsresultat er HENLEGGELSE (indikerer at saken ble annullert via X008/X006)
 * - Behandlingstema er BESLUTNING_LOVVALG_ANNET_LAND (artikkel 13-saker)
 */
interface RettOppFeilMedlPerioderRepository : CrudRepository<Behandling, Long> {

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
}
