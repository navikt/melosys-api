package no.nav.melosys.service.saksbehandling

import no.finn.unleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName.IKKEYRKESAKTIV_FLYT
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.springframework.stereotype.Component

@Component
class SaksbehandlingRegler(private val behandlingsresultatRepository: BehandlingsresultatRepository, private val unleash: Unleash) {

    fun skalTidligereBehandlingReplikeres(
        fagsak: Fagsak,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        val ftrlToggleEnabled = unleash.isEnabled("melosys.folketrygden.mvp")
        val ikkeYrkesaktivToggleEnabled = unleash.isEnabled(IKKEYRKESAKTIV_FLYT)
        if (harTomFlyt(fagsak.type, fagsak.tema, behandlingstype, behandlingstema, ftrlToggleEnabled, ikkeYrkesaktivToggleEnabled)) return false

        return finnBehandlingSomKanReplikeres(fagsak) != null
    }

    fun finnBehandlingSomKanReplikeres(fagsak: Fagsak) =
        finnBehandlingSomKanReplikeres(fagsak.hentBehandlingerSortertSynkendePåRegistrertDato())

    internal fun finnBehandlingSomKanReplikeres(behandlinger: List<Behandling>) =
        behandlinger
            .filter { it.erInaktiv() }
            .filter { !harTomFlyt(it, unleash.isEnabled("melosys.folketrygden.mvp"), unleash.isEnabled(IKKEYRKESAKTIV_FLYT)) }
            .firstOrNull {
                val behandlingsresultat = behandlingsresultatRepository.findById(it.id)
                behandlingstyperSomKanReplikeres.contains(it.type)
                    && behandlingsresultat.isPresent
                    && !behandlingsresultattyperSomIkkeKanReplikeres.contains(behandlingsresultat.get().type)
            }

    companion object {
        val behandlingstyperSomKanReplikeres = listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.ENDRET_PERIODE,
            Behandlingstyper.FØRSTEGANG
        )
        val behandlingsresultattyperSomIkkeKanReplikeres = listOf(
            Behandlingsresultattyper.HENLEGGELSE,
            Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
            Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,
            Behandlingsresultattyper.FERDIGBEHANDLET,
            Behandlingsresultattyper.HENLEGGELSE_BORTFALT
        )

        @JvmStatic
        fun harTomFlyt(behandling: Behandling, ftrlToggleEnabled: Boolean, ikkeYrkesaktivToggleEnabled: Boolean): Boolean =
            harTomFlyt(behandling.fagsak.type, behandling.fagsak.tema, behandling.type, behandling.tema, ftrlToggleEnabled, ikkeYrkesaktivToggleEnabled)

        @JvmStatic
        fun harTomFlyt(
            sakstype: Sakstyper,
            sakstema: Sakstemaer,
            behandlingstype: Behandlingstyper,
            behandlingstema: Behandlingstema,
            ftrlToggleEnabled: Boolean,
            ikkeYrkesaktivToggleEnabled: Boolean,
        ): Boolean {
            if (sakstema == Sakstemaer.TRYGDEAVGIFT) return true
            if (behandlingstype == Behandlingstyper.HENVENDELSE || behandlingstype == Behandlingstyper.KLAGE) return true

            return when (behandlingstema) {
                ARBEID_KUN_NORGE,
                PENSJONIST,
                REGISTRERING_UNNTAK,
                UNNTAK_MEDLEMSKAP,
                FORESPØRSEL_TRYGDEMYNDIGHET,
                TRYGDETID,
                -> true

                ANMODNING_OM_UNNTAK_HOVEDREGEL -> sakstype == Sakstyper.TRYGDEAVTALE
                YRKESAKTIV -> (sakstype == Sakstyper.FTRL && !ftrlToggleEnabled)
                IKKE_YRKESAKTIV -> (!ikkeYrkesaktivToggleEnabled)

                else -> return false
            }
        }
    }
}
