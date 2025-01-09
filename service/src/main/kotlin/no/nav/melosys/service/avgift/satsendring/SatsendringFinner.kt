package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import org.springframework.stereotype.Component

@Component
class SatsendringFinner(trygdeavgiftsberegningService: TrygdeavgiftsberegningService) {
    fun finnBehandlingerMedSatsendringer(år: Int): AvgiftSatsendringInfo {
        // Finn alle resultater med vedtak + trygdeavgift i oppgitt år

        // For hver behandling, sjekk om det er satsendring og sett i riktig liste

        return AvgiftSatsendringInfo(
            år = år,
            sakerMedSatsendring = listOf(
                Sak("MEL-2", 77),
            ),
            sakerMedSatsendringOgNyVurdering = listOf(
            ),
            sakerUtenSatsendring = listOf(
            )
        )
    }
}

data class AvgiftSatsendringInfo(
    val år: Int,
    val sakerMedSatsendring: List<Sak>,
    val sakerMedSatsendringOgNyVurdering: List<Sak>,
    val sakerUtenSatsendring: List<Sak>
)

data class Sak(
    val saksnummer: String,
    val behandlingID: Long
)
