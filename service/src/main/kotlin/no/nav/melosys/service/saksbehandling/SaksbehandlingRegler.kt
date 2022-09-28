package no.nav.melosys.service.saksbehandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.springframework.stereotype.Component

@Component
class SaksbehandlingRegler(private val behandlingsresultatRepository: BehandlingsresultatRepository) {

    fun skalTidligereBehandlingReplikeres(
        fagsak: Fagsak,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        val sistRegistrertBehandling = fagsak.hentSistRegistrertBehandling()
        val sakstype = sistRegistrertBehandling.fagsak.type

        if (harTomFlyt(sakstype, behandlingstype, behandlingstema)) return false

        return finnesBehandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyper(
            fagsak.hentBehandlingerSortertSynkendePåRegistrertDato(),
            listOf(
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.ENDRET_PERIODE,
                Behandlingstyper.FØRSTEGANG
            ),
            listOf(
                Behandlingsresultattyper.HENLEGGELSE,
                Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
                Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,
                Behandlingsresultattyper.FERDIGBEHANDLET,
                Behandlingsresultattyper.HENLEGGELSE_BORTFALT
            )
        )
    }

    internal fun finnesBehandlingMedBehandlingTyperOgIkkeBehandlingsresultatTyper(
        behandlinger: List<Behandling>,
        behandlingstyper: List<Behandlingstyper>,
        behandlingsresultattyper: List<Behandlingsresultattyper>
    ): Boolean {
        return behandlinger.firstOrNull {
            val behandlingsresultat = behandlingsresultatRepository.findById(it.id)
            behandlingstyper.contains(it.type)
                && behandlingsresultat.isPresent
                && !behandlingsresultattyper.contains(behandlingsresultat.get().type)
        } != null
    }

    companion object {
        @JvmStatic
        fun harTomFlyt(
            sakstype: Sakstyper,
            behandlingstype: Behandlingstyper,
            behandlingstema: Behandlingstema
        ): Boolean {
            if (behandlingstype == Behandlingstyper.HENVENDELSE || behandlingstype == Behandlingstyper.KLAGE) return true

            return when (behandlingstema) {
                ARBEID_KUN_NORGE,
                IKKE_YRKESAKTIV,
                PENSJONIST,
                REGISTRERING_UNNTAK,
                UNNTAK_MEDLEMSKAP,
                FORESPØRSEL_TRYGDEMYNDIGHET,
                TRYGDETID,
                ØVRIGE_SED_MED,
                ØVRIGE_SED_UFM -> true

                ANMODNING_OM_UNNTAK_HOVEDREGEL -> sakstype == Sakstyper.TRYGDEAVTALE
                YRKESAKTIV -> sakstype == Sakstyper.FTRL

                else -> return false
            }
        }
    }
}
