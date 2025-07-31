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
            .filterNot { it.erÅrsavregning() }
            .firstOrNull {
                val behandlingsresultat = behandlingsresultatRepository.findById(it.id)
                BEHANDLINGSTYPER_SOM_KAN_REPLIKERES.contains(it.type)
                    && behandlingsresultat.isPresent
                    && !BEHANDLINGSRESULTATTYPER_SOM_IKKE_KAN_REPLIKERES.contains(behandlingsresultat.get().type)
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
        if (sakstema == Sakstemaer.TRYGDEAVGIFT){

            if(sakstype == Sakstyper.EU_EOS && behandlingstema == PENSJONIST){
                return !unleash.isEnabled(ToggleName.MELOSYS_PENSJONIST_EØS)
            }

            return true
        }

        if (behandlingstype == Behandlingstyper.HENVENDELSE || behandlingstype == Behandlingstyper.KLAGE) return true

        if (harRegistreringUnntakFraMedlemskapFlyt(
                sakstype,
                sakstema,
                behandlingstema,
            )
        ) return false

        return when (behandlingstema) {
            REGISTRERING_UNNTAK,
            UNNTAK_MEDLEMSKAP,
            FORESPØRSEL_TRYGDEMYNDIGHET,
            TRYGDETID,
            A1_ANMODNING_OM_UNNTAK_PAPIR,
            -> true
            PENSJONIST -> !unleash.isEnabled(ToggleName.MELOSYS_PENSJONIST)
            ANMODNING_OM_UNNTAK_HOVEDREGEL -> sakstype == Sakstyper.TRYGDEAVTALE

            else -> return false
        }
    }

    fun harRegistreringUnntakFraMedlemskapFlyt(
        behandling: Behandling
    ): Boolean = harRegistreringUnntakFraMedlemskapFlyt(
        behandling.fagsak.type,
        behandling.fagsak.tema,
        behandling.tema
    )

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
    ) = harIkkeYrkesaktivFlyt(behandling.fagsak.type, behandling.tema)

    fun harIkkeYrkesaktivFlyt(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema
    ) = (sakstype == Sakstyper.EU_EOS || sakstype == Sakstyper.TRYGDEAVTALE) && behandlingstema == IKKE_YRKESAKTIV

    fun harPensjonistUføretrygdetFlyt(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema
    ) = sakstype == Sakstyper.EU_EOS && behandlingstema == PENSJONIST

    fun harUtsendtArbeidsTakerKunNorgeFlyt(erSakstypeEøs: Boolean, behandlingstema: Behandlingstema, land: Land_iso2) = erSakstypeEøs
        && (behandlingstema == UTSENDT_ARBEIDSTAKER
        || behandlingstema == UTSENDT_SELVSTENDIG
        || behandlingstema == ARBEID_KUN_NORGE)
        && land == Land_iso2.NO

    companion object {

        private val BEHANDLINGSTYPER_SOM_KAN_REPLIKERES = listOf(
            Behandlingstyper.FØRSTEGANG,
            Behandlingstyper.NY_VURDERING,
            Behandlingstyper.SATSENDRING,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )
        private val BEHANDLINGSRESULTATTYPER_SOM_IKKE_KAN_REPLIKERES = listOf(
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
