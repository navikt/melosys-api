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
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.featuretoggle.ToggleName.*
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.springframework.stereotype.Component

@Component
class SaksbehandlingRegler(
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    private val unleash: Unleash
) {

    fun skalTidligereBehandlingReplikeres(
        fagsak: Fagsak,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        if (harTomFlytIgnorerInaktivOgMottatteOpplysninger(
                fagsak.type,
                fagsak.tema,
                behandlingstype,
                behandlingstema
            )
        ) return false

        return finnBehandlingSomKanReplikeres(fagsak) != null
    }

    fun finnBehandlingSomKanReplikeres(fagsak: Fagsak) =
        finnBehandlingSomKanReplikeres(fagsak.hentBehandlingerSortertSynkendePåRegistrertDato())

    internal fun finnBehandlingSomKanReplikeres(behandlinger: List<Behandling>) =
        behandlinger
            .filter { it.erInaktiv() }
            .filter { !harTomFlytIgnorerInaktivOgMottatteOpplysninger(it.fagsak.type, it.fagsak.tema, it.type, it.tema) }
            .firstOrNull {
                val behandlingsresultat = behandlingsresultatRepository.findById(it.id)
                behandlingstyperSomKanReplikeres.contains(it.type)
                    && behandlingsresultat.isPresent
                    && !behandlingsresultattyperSomIkkeKanReplikeres.contains(behandlingsresultat.get().type)
            }

    fun harTomFlyt(behandling: Behandling): Boolean =
        harTomFlyt(
            behandling.fagsak.type,
            behandling.fagsak.tema,
            behandling.type,
            behandling.tema,
            behandling.mottatteOpplysninger,
            behandling.erInaktiv()
        )

    fun harTomFlytIgnorerInaktivOgMottatteOpplysninger(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean =
        harTomFlyt(sakstype, sakstema, behandlingstype, behandlingstema, null, false)

    fun harTomFlyt(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
        mottatteOpplysninger: MottatteOpplysninger?,
        erBehandlingInaktiv: Boolean
    ): Boolean {
        if (erBehandlingInaktiv && mottatteOpplysninger === null)
            return true

        if (harRegistreringUnntakFraMedlemskapFlyt(
                sakstype,
                sakstema,
                behandlingstema,
            )
        ) return false

        if (sakstema == Sakstemaer.TRYGDEAVGIFT) return true
        if (behandlingstype == Behandlingstyper.HENVENDELSE || behandlingstype == Behandlingstyper.KLAGE) return true

        return when (behandlingstema) {
            ARBEID_KUN_NORGE,
            PENSJONIST,
            REGISTRERING_UNNTAK,
            UNNTAK_MEDLEMSKAP,
            FORESPØRSEL_TRYGDEMYNDIGHET,
            TRYGDETID,
            A1_ANMODNING_OM_UNNTAK_PAPIR
            -> true

            ANMODNING_OM_UNNTAK_HOVEDREGEL -> sakstype == Sakstyper.TRYGDEAVTALE
            YRKESAKTIV -> (sakstype == Sakstyper.FTRL && !unleash.isEnabled(FOLKETRYGDEN_MVP))
            IKKE_YRKESAKTIV -> (!unleash.isEnabled(IKKEYRKESAKTIV_FLYT))

            else -> false
        }
    }

    fun harRegistreringUnntakFraMedlemskapFlyt(behandling: Behandling): Boolean =
        harRegistreringUnntakFraMedlemskapFlyt(
            behandling.fagsak.type,
            behandling.fagsak.tema,
            behandling.tema
        )

    fun harRegistreringUnntakFraMedlemskapFlyt(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema
    ): Boolean {
        if (!unleash.isEnabled(REGISTRERING_UNNTAK_FRA_MEDLEMSKAP) || sakstema != Sakstemaer.UNNTAK) {
            return false
        }

        if (sakstype == Sakstyper.EU_EOS && behandlingstema == A1_ANMODNING_OM_UNNTAK_PAPIR) {
            return true
        }

        if (sakstype == Sakstyper.TRYGDEAVTALE && listOf(
                REGISTRERING_UNNTAK,
                ANMODNING_OM_UNNTAK_HOVEDREGEL
            ).contains(behandlingstema)
        ) {
            return true
        }
        return false
    }

    fun harIkkeYrkesaktivFlyt(sakstype: Sakstyper, behandlingstema: Behandlingstema): Boolean =
        unleash.isEnabled(IKKEYRKESAKTIV_FLYT)
            && (sakstype == Sakstyper.EU_EOS || sakstype == Sakstyper.TRYGDEAVTALE)
            && behandlingstema == IKKE_YRKESAKTIV

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
