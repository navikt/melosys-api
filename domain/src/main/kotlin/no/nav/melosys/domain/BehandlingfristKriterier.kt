package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import org.apache.commons.lang3.tuple.Pair
import java.time.LocalDate

/*
 * ref: https://confluence.adeo.no/display/TEESSI/Behandlingsfrister+i+Melosys
 */
class BehandlingfristKriterier : Behandling() {
    companion object {
        @JvmStatic
        fun hentBehandlingsFrist(
            sakstema: Sakstemaer,
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper,
            utgangspunktDato: LocalDate
        ): LocalDate {
            val frist6Uker = utgangspunktDato.plusWeeks(6)
            val frist8Uker = utgangspunktDato.plusWeeks(8)
            val frist70Dager = utgangspunktDato.plusDays(70)
            val frist90Dager = utgangspunktDato.plusDays(90)
            val frist180Dager = utgangspunktDato.plusDays(180)

            val behandlingfrister = listOf(
                Pair.of(harFrist6UkerManglendeInnbetaling(behandlingstype), frist6Uker),
                Pair.of(harFrist8UkerLovvalg(behandlingstema, behandlingstype), frist8Uker),
                Pair.of(harFrist70DagerKlager(behandlingstype), frist70Dager),
                Pair.of(harFrist90DagerSøknadsbehandlinger(sakstema, behandlingstema, behandlingstype), frist90Dager),
                Pair.of(harFrist90DagerAnmodningOmUnntak(behandlingstema, behandlingstype), frist90Dager),
                Pair.of(
                    harFrist90DagerAttesterFraAndreTrygdeavtaleland(behandlingstema, behandlingstype),
                    frist90Dager
                ),
                Pair.of(harFrist90DagerHenvendelser(behandlingstype), frist90Dager),
                Pair.of(
                    harFrist180DagerMeldingOmUtstasjoneringEllerLovvalg(behandlingstema, behandlingstype),
                    frist180Dager
                )
            )
            val behandlingsFrist =
                behandlingfrister.firstOrNull { fristPair: Pair<Boolean, LocalDate> -> fristPair.left == true }
                    ?: throw FunksjonellException(
                        String.format(
                            "Kunne ikke utlede behandlingsfrist for behandling med: sakstema %s, behandlingstema %s, behandlingstype %s",
                            sakstema,
                            behandlingstema,
                            behandlingstype
                        )
                    )

            return behandlingsFrist.right
        }


        private fun harFrist6UkerManglendeInnbetaling(behandlingstype: Behandlingstyper): Boolean {
            val behandlingstyper = setOf(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            return behandlingstyper.contains(behandlingstype)
        }


        private fun harFrist8UkerLovvalg(behandlingsTema: Behandlingstema, behandlingstype: Behandlingstyper): Boolean {
            val behandlingstemaer =
                setOf(Behandlingstema.BESLUTNING_LOVVALG_NORGE, Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND)
            val behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
            return behandlingstemaer.contains(behandlingsTema) && behandlingstyper.contains(behandlingstype)
        }

        private fun harFrist70DagerKlager(behandlingstype: Behandlingstyper): Boolean {
            val behandlingstyper = setOf(Behandlingstyper.KLAGE)
            return behandlingstyper.contains(behandlingstype)
        }

        private fun harFrist90DagerSøknadsbehandlinger(
            sakstema: Sakstemaer,
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper
        ): Boolean {
            val sakstemaer = setOf(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstemaer.TRYGDEAVGIFT)
            val behandlingstemaer = setOf(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND)
            val behandlingstyper =
                setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.ENDRET_PERIODE, Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            return sakstemaer.contains(sakstema) && !behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(
                behandlingstype
            )
        }

        private fun harFrist90DagerAnmodningOmUnntak(
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper
        ): Boolean {
            val behandlingstemaer = setOf(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL)
            val behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
            return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype)
        }

        private fun harFrist90DagerAttesterFraAndreTrygdeavtaleland(
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper
        ): Boolean {
            val behandlingstemaer =
                setOf(Behandlingstema.REGISTRERING_UNNTAK, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)
            val behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
            return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype)
        }

        private fun harFrist90DagerHenvendelser(behandlingstype: Behandlingstyper): Boolean {
            val behandlingstyper = setOf(Behandlingstyper.HENVENDELSE)
            return behandlingstyper.contains(behandlingstype)
        }

        private fun harFrist180DagerMeldingOmUtstasjoneringEllerLovvalg(
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper
        ): Boolean {
            val behandlingstemaer = setOf(
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
            )
            val behandlingstyper = setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING)
            return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype)
        }
    }

}
