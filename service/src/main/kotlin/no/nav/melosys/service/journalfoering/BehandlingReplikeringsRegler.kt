package no.nav.melosys.service.journalfoering

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
class BehandlingReplikeringsRegler(private val behandlingsresultatRepository: BehandlingsresultatRepository) {

    fun skalTidligereBehandlingReplikeres(
        fagsak: Fagsak,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        val sistRegistrertBehandling = fagsak.hentSistRegistrertBehandling()
        val sakstype = sistRegistrertBehandling.fagsak.type

        when (behandlingstema) {
            ARBEID_KUN_NORGE,
            IKKE_YRKESAKTIV,
            PENSJONIST,
            REGISTRERING_UNNTAK,
            UNNTAK_MEDLEMSKAP,
            FORESPØRSEL_TRYGDEMYNDIGHET,
            TRYGDETID,
            ØVRIGE_SED_MED,
            ØVRIGE_SED_UFM -> return false

            ANMODNING_OM_UNNTAK_HOVEDREGEL -> if (sakstype == Sakstyper.TRYGDEAVTALE) return false
            YRKESAKTIV -> if (sakstype == Sakstyper.FTRL) return false
            else ->
                if (behandlingstype == Behandlingstyper.HENVENDELSE || behandlingstype == Behandlingstyper.KLAGE) return false
        }

        return finnBehandlingSomKanReplikeres(fagsak) != null
    }

    fun finnBehandlingSomKanReplikeres(fagsak: Fagsak) =
        finnBehandlingSomKanReplikeres(fagsak.hentBehandlingerSortertSynkendePåRegistrertDato())

    internal fun finnBehandlingSomKanReplikeres(behandlinger: List<Behandling>) =
        behandlinger
            .filter { it.erInaktiv() }
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
    }
}
