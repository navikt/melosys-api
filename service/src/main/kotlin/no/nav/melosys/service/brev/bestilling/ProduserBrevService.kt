package no.nav.melosys.service.brev.bestilling

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProduserBrevService(
    private val dokumentServiceFasade: DokumentServiceFasade
) {

    @Transactional
    fun produserBrev(behandlingId: Long, brevbestillingDto: BrevbestillingDto) {
        validerAtBrevKanProduseres(brevbestillingDto.produserbardokument)
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingDto)
    }

    private fun validerAtBrevKanProduseres(produserbardokument: Produserbaredokumenter) {
        if (produserbardokument !in DOKUMENTER_SOM_KAN_PRODUSERES_UAVHENGIG_AV_FLYT) {
            throw FunksjonellException("Manuell bestilling av $produserbardokument er ikke støttet.")
        }
    }

    companion object {
        private val DOKUMENTER_SOM_KAN_PRODUSERES_UAVHENGIG_AV_FLYT = listOf(
            MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
            MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
            MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER,
            AVSLAG_MANGLENDE_OPPLYSNINGER,
            INNHENTING_AV_INNTEKTSOPPLYSNINGER,
            ORIENTERING_ANMODNING_UNNTAK,
            GENERELT_FRITEKSTBREV_BRUKER,
            GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            GENERELT_FRITEKSTBREV_VIRKSOMHET,
            UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
            ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK,
            FRITEKSTBREV
        )
    }
}
