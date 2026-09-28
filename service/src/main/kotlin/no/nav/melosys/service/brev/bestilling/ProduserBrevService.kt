package no.nav.melosys.service.brev.bestilling

import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.brev.BrevAdresse
import no.nav.melosys.service.dokument.DokumentServiceFasade
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.dokument.brev.KopiMottakerDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProduserBrevService(
    private val dokumentServiceFasade: DokumentServiceFasade,
    private val hentBrevAdresseTilMottakereService: HentBrevAdresseTilMottakereService
) {

    @Transactional
    fun produserBrev(behandlingId: Long, brevbestillingDto: BrevbestillingDto) {
        validerAtBrevKanProduseres(brevbestillingDto.produserbardokument)
        validerAdresseTilKopiTilBrukerEllerFullmektig(behandlingId, brevbestillingDto.kopiMottakere.orEmpty())
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingDto)
    }

    private fun validerAtBrevKanProduseres(produserbardokument: Produserbaredokumenter) {
        if (produserbardokument !in DOKUMENTER_SOM_KAN_PRODUSERES_UAVHENGIG_AV_FLYT) {
            throw FunksjonellException("Manuell bestilling av $produserbardokument er ikke støttet.")
        }
    }

    private fun validerAdresseTilKopiTilBrukerEllerFullmektig(behandlingId: Long, kopiMottakere: List<KopiMottakerDto>) {
        kopiMottakere.filter { it.erBrukerEllerPrivatFullmektig() }.forEach { kopiMottaker ->
            val brevAdresser = hentBrevAdresseTilMottakereService.hentBrevAdresseTilMottakere(behandlingId, kopiMottaker.rolle())
            if (brevAdresser.all(BrevAdresse::ugyldig)) {
                throw FunksjonellException(manglendeAdresseBegrunnelse(kopiMottaker.rolle()).beskrivelse)
            }
        }
    }

    private fun manglendeAdresseBegrunnelse(rolle: Mottakerroller) =
        if (rolle == Mottakerroller.FULLMEKTIG) MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT
        else MANGLENDE_REGISTRERTE_ADRESSE_BRUKER

    private fun KopiMottakerDto.erBrukerEllerPrivatFullmektig() =
        rolle() == Mottakerroller.BRUKER || (rolle() == Mottakerroller.FULLMEKTIG && orgnr() == null)

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
