package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalAmount

/*
 * ref: https://confluence.adeo.no/display/TEESSI/Behandlingsfrister+i+Melosys
 */
object BehandlingfristKriterier {

    @JvmStatic
    fun hentBehandlingsFrist(
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        utgangspunktDato: LocalDate
    ): LocalDate = behandlingFristRegler
        .firstOrNull { it.matches(sakstema, behandlingstema, behandlingstype) }
        ?.regnUtBehandlingfrist(utgangspunktDato)
        ?: throw FunksjonellException(
            "Kunne ikke utlede behandlingsfrist for behandling med: " +
                "sakstema $sakstema, behandlingstema $behandlingstema, behandlingstype $behandlingstype"
        )

    private val behandlingFristRegler = listOf(
        BehandlingfristRegel(6.weeks(),
            behandlingstyper = setOf(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
        ),

        BehandlingfristRegel(8.weeks(),
            behandlingstemaer = setOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND),
            behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        ),

        BehandlingfristRegel(70.days(),
            behandlingstyper = setOf(Behandlingstyper.KLAGE)
        ),

        // Søknadsbehandlinger: 90 dager for spesifikke sakstemaer, UNNTATT BESLUTNING_LOVVALG_ANNET_LAND
        BehandlingfristRegel(90.days(),
            sakstemaer = setOf(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstemaer.TRYGDEAVGIFT),
            excludeBehandlingstemaer = setOf(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND),
            behandlingstyper = setOf(
                Behandlingstyper.FØRSTEGANG,
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.ENDRET_PERIODE,
                Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT,
                Behandlingstyper.ÅRSAVREGNING
            )
        ),

        BehandlingfristRegel(90.days(),
            behandlingstemaer = setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL),
            behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        ),

        BehandlingfristRegel(90.days(),
            behandlingstemaer = setOf(Behandlingstema.REGISTRERING_UNNTAK, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR),
            behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        ),

        BehandlingfristRegel(90.days(),
            behandlingstyper = setOf(Behandlingstyper.HENVENDELSE)
        ),

        BehandlingfristRegel(180.days(),
            behandlingstemaer = setOf(
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
            ),
            behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
        )
    )

    private data class BehandlingfristRegel(
        private val period: TemporalAmount,
        private val sakstemaer: Set<Sakstemaer> = emptySet(),
        private val behandlingstemaer: Set<Behandlingstema> = emptySet(),
        private val behandlingstyper: Set<Behandlingstyper> = emptySet(),
        private val excludeBehandlingstemaer: Set<Behandlingstema> = emptySet()
    ) {
        fun matches(
            sakstema: Sakstemaer,
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper
        ): Boolean {
            return (sakstemaer.isEmpty() || sakstemaer.contains(sakstema)) &&
                (behandlingstemaer.isEmpty() || behandlingstemaer.contains(behandlingstema)) &&
                (behandlingstyper.isEmpty() || behandlingstyper.contains(behandlingstype)) &&
                (excludeBehandlingstemaer.isEmpty() || !excludeBehandlingstemaer.contains(behandlingstema))
        }

        fun regnUtBehandlingfrist(startDate: LocalDate): LocalDate = startDate.plus(period)
    }

    private fun Int.weeks(): Period = Period.ofWeeks(this)
    private fun Int.days(): Period = Period.ofDays(this)
}
