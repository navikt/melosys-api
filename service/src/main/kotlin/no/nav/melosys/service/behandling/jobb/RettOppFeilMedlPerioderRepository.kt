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
        SELECT b FROM Behandling b
        JOIN FETCH b.fagsak f
        JOIN b.behandlingsresultat br
        WHERE f.status = no.nav.melosys.domain.kodeverk.Saksstatuser.LOVVALG_AVKLART
        AND br.type = no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.HENLEGGELSE
        AND b.behandlingsTema = no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND
        ORDER BY b.endretDato DESC
    """)
    fun finnBehandlingerMedFeilStatus(): List<Behandling>
}
