package no.nav.melosys.service.brev.bestilling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HentMuligeProduserbaredokumenterService(private val behandlingService: BehandlingService, private val unleash: Unleash) {

    @Transactional
    fun hentMuligeProduserbaredokumenter(behandlingId: Long, mottakerroller: Mottakerroller): List<Produserbaredokumenter> {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        val fagsak = behandling.fagsak
        return if (behandling.erInaktiv()) {
            emptyList()
        } else when (mottakerroller) {
            Mottakerroller.BRUKER -> hentMuligeForBruker(behandling, behandling.fagsak)
            Mottakerroller.ARBEIDSGIVER ->
                if (behandling.erManglendeInnbetalingTrygdeavgift())
                    listOf(Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER)
                else
                    listOf(
                        Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
                        Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER
                    )

            Mottakerroller.ANNEN_ORGANISASJON ->
                if (fagsak.hovedpartRolle == Aktoersroller.VIRKSOMHET || behandling.erManglendeInnbetalingTrygdeavgift())
                    listOf(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
                else
                    listOf(
                        Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER,
                        Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER
                    )

            Mottakerroller.VIRKSOMHET -> listOf(Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET)
            Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET -> listOf(Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV)
            Mottakerroller.NORSK_MYNDIGHET -> listOf(Produserbaredokumenter.FRITEKSTBREV)
            else -> throw FunksjonellException("Mottakerrollen $mottakerroller kan ikke sende brev gjennom brevmenyen")
        }
    }

    private fun hentMuligeForBruker(behandling: Behandling, fagsak: Fagsak): List<Produserbaredokumenter> {
        if (behandling.erManglendeInnbetalingTrygdeavgift()) {
            return listOf(Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER)
        }

        val muligeProduserbareDokumenter = hentDefaultMuligeForBruker()

        if (behandling.erÅrsavregning()) {
            muligeProduserbareDokumenter.add(Produserbaredokumenter.INNHENTING_AV_INNTEKTSOPPLYSNINGER)
        }

        if (fagsak.tema == Sakstemaer.MEDLEMSKAP_LOVVALG && (behandling.erFørstegangsvurdering() || behandling.erAndregangsbehandling())) {
            return listOf(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD) + muligeProduserbareDokumenter
        }

        return muligeProduserbareDokumenter
    }

    private fun hentDefaultMuligeForBruker() = mutableListOf(
        Produserbaredokumenter.MANGELBREV_BRUKER,
        Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER
    )
}
