package no.nav.melosys.service.brev

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils


@Service
class DokumentNavnService(
    private val brevmottakerService: BrevmottakerService,
    private val dokgenService: DokgenService,
    private val lovvalgsperiodeService: LovvalgsperiodeService
) {
    fun utledDokumentNavnForProduserbaredokumenterOgMottakerrolle(
        behandling: Behandling,
        produserbaredokumenter: Produserbaredokumenter,
        mottakerRolle: Mottakerroller
    ): String {
        if (erTrygdeavtaleVedtaksbrev(produserbaredokumenter)) {
            val mottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.medRolle(mottakerRolle), behandling)
            return utledDokumentNavnForProduserbaredokumenterOgMottaker(behandling, produserbaredokumenter, mottaker, null)
        }
        return produserbaredokumenter.beskrivelse
    }

    fun utledDokumentNavnForProduserbaredokumenterOgMottaker(
        behandling: Behandling,
        produserbaredokumenter: Produserbaredokumenter,
        mottaker: Mottaker,
        standardTekst: String?
    ): String {
        if (erTrygdeavtaleVedtaksbrev(produserbaredokumenter)) {
            val dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbaredokumenter)
            return utledTittelTrygdeavtale(behandling, dokumentproduksjonsInfo, mottaker)
        }
        return standardTekst ?: produserbaredokumenter.beskrivelse
    }

    private fun erTrygdeavtaleVedtaksbrev(produserbaredokumenter: Produserbaredokumenter): Boolean {
        return produserbaredokumenter.kode.contains("TRYGDEAVTALE") && produserbaredokumenter.beskrivelse.contains("Vedtaksbrev")
    }

    fun utledTittelTrygdeavtale(behandling: Behandling, dokumentproduksjonsInfo: DokumentproduksjonsInfo, mottaker: Mottaker): String {
        val tittel = utledTittelTrygdeavtale(behandling.id, dokumentproduksjonsInfo, mottaker)
        return if (behandling.erNyVurdering()) lagEndringTittel(tittel) else tittel
    }

    private fun utledTittelTrygdeavtale(behandlingID: Long, dokumentproduksjonsInfo: DokumentproduksjonsInfo, mottaker: Mottaker): String {
        if (mottaker.erUtenlandskMyndighet()) {
            return dokumentproduksjonsInfo.attestTittel ?: throw FunksjonellException("Forventer at trygdeavtale-brev har attest-tittel")
        }

        val erArtikkel8_2 = lovvalgsperiodeService.hentLovvalgsperiode(behandlingID).bestemmelse == Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART8_2
        val tittel = if (erArtikkel8_2) dokumentproduksjonsInfo.alternativTittel else dokumentproduksjonsInfo.journalføringsTittel

        if (mottaker.rolle == Mottakerroller.BRUKER) {
            return tittel
        }

        return lagKopiTittel(tittel)
    }

    private fun lagKopiTittel(tittel: String): String = "Kopi av " + StringUtils.uncapitalize(tittel)
    private fun lagEndringTittel(tittel: String): String = "$tittel - endring"
}
