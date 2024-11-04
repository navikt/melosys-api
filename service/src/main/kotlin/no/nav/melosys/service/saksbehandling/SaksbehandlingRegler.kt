package no.nav.melosys.service.saksbehandling

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.springframework.stereotype.Component

@Component
class SaksbehandlingRegler(private val behandlingsresultatRepository: BehandlingsresultatRepository, private val unleash: Unleash) {

    fun skalTidligereBehandlingReplikeres(
        fagsak: Fagsak,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        if (harIngenFlyt(
                fagsak.type,
                fagsak.tema,
                behandlingstype,
                behandlingstema
            )
        ) return false

        if (behandlingstype == Behandlingstyper.ÅRSAVREGNING) return false

        return finnBehandlingSomKanReplikeres(fagsak) != null
    }

    fun finnBehandlingSomKanReplikeres(fagsak: Fagsak): Behandling? =
        finnBehandlingSomKanReplikeres(fagsak.hentBehandlingerSortertSynkendePåRegistrertDato())

    internal fun finnBehandlingSomKanReplikeres(behandlinger: List<Behandling>) =
        behandlinger
            .filter { it.erInaktiv() }
            .filter { !harIngenFlyt(it) }
            .filter { !it.erÅrsavregning() }
            .firstOrNull {
                val behandlingsresultat = behandlingsresultatRepository.findById(it.id)
                behandlingstyperSomKanReplikeres.contains(it.type)
                    && behandlingsresultat.isPresent
                    && !behandlingsresultattyperSomIkkeKanReplikeres.contains(behandlingsresultat.get().type)
            }

    fun harIngenFlyt(behandling: Behandling): Boolean =
        harIngenFlyt(
            behandling.fagsak.type,
            behandling.fagsak.tema,
            behandling.type,
            behandling.tema
        )

    fun harIngenFlyt(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        if (sakstema == Sakstemaer.TRYGDEAVGIFT) return true

        if (behandlingstype == Behandlingstyper.HENVENDELSE || behandlingstype == Behandlingstyper.KLAGE) return true

        if (harRegistreringUnntakFraMedlemskapFlyt(
                sakstype,
                sakstema,
                behandlingstema,
            )
        ) return false

        return when (behandlingstema) {
            PENSJONIST,
            REGISTRERING_UNNTAK,
            UNNTAK_MEDLEMSKAP,
            FORESPØRSEL_TRYGDEMYNDIGHET,
            TRYGDETID,
            A1_ANMODNING_OM_UNNTAK_PAPIR
            -> true

            ARBEID_KUN_NORGE -> !unleash.isEnabled(ToggleName.MELOSYS_ARBEID_KUN_NORGE)

            ANMODNING_OM_UNNTAK_HOVEDREGEL -> sakstype == Sakstyper.TRYGDEAVTALE

            else -> return false
        }
    }

    fun harRegistreringUnntakFraMedlemskapFlyt(
        behandling: Behandling
    ): Boolean {
        return harRegistreringUnntakFraMedlemskapFlyt(
            behandling.fagsak.type,
            behandling.fagsak.tema,
            behandling.tema
        )
    }

    fun harRegistreringUnntakFraMedlemskapFlyt(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema
    ): Boolean {
        if (sakstema != Sakstemaer.UNNTAK) {
            return false
        }

        if (sakstype == Sakstyper.EU_EOS && behandlingstema == A1_ANMODNING_OM_UNNTAK_PAPIR) {
            return true
        }

        return sakstype == Sakstyper.TRYGDEAVTALE && listOf(
            REGISTRERING_UNNTAK,
            ANMODNING_OM_UNNTAK_HOVEDREGEL
        ).contains(behandlingstema)
    }

    fun harIkkeYrkesaktivFlyt(
        behandling: Behandling
    ): Boolean {
        return harIkkeYrkesaktivFlyt(behandling.fagsak.type, behandling.tema)
    }

    fun harIkkeYrkesaktivFlyt(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema
    ): Boolean {
        return (sakstype == Sakstyper.EU_EOS || sakstype == Sakstyper.TRYGDEAVTALE) && behandlingstema == IKKE_YRKESAKTIV
    }

    fun harUtsendtArbeidsTakerKunNorgeFlyt(erSakstypeEøs: Boolean, behandlingstema: Behandlingstema, land: Land_iso2): Boolean {
        return erSakstypeEøs
            && (behandlingstema.equals(UTSENDT_ARBEIDSTAKER)
            || behandlingstema.equals(UTSENDT_SELVSTENDIG)
            || behandlingstema.equals(ARBEID_KUN_NORGE))
            && land == Land_iso2.NO
    }

    companion object {
        val behandlingstyperSomKanReplikeres = listOf(
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )
        val behandlingsresultattyperSomIkkeKanReplikeres = listOf(
            Behandlingsresultattyper.HENLEGGELSE,
            Behandlingsresultattyper.ANMODNING_OM_UNNTAK,
            Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,
            Behandlingsresultattyper.FERDIGBEHANDLET,
            Behandlingsresultattyper.HENLEGGELSE_BORTFALT,
            Behandlingsresultattyper.ANNULLERT,
            Behandlingsresultattyper.OPPHØRT
        )
    }
}
