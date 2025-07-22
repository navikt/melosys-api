package no.nav.melosys.service.brev

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.brev.Brevbestilling
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.dokument.BrevmottakerService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.util.StringUtils.hasText


@Service
class DokumentNavnService(
    private val brevmottakerService: BrevmottakerService,
    private val dokgenService: DokgenService,
    private val lovvalgsperiodeService: LovvalgsperiodeService,
    private val medlemskapsperiodeService: MedlemskapsperiodeService
) {
    fun utledDokumentNavnForProduserbartdokumentOgMottakerrolle(
        behandling: Behandling,
        produserbartdokument: Produserbaredokumenter,
        mottakerRolle: Mottakerroller
    ): String =
        utledTittel(
            behandling = behandling,
            produserbartdokument = produserbartdokument,
            dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbartdokument),
            mottakerRolle = mottakerRolle
        )


    fun utledDokumentNavnForProduserbartdokumentOgMottaker(
        behandling: Behandling,
        produserbardokument: Produserbaredokumenter,
        mottaker: Mottaker,
        standardTekst: String
    ): String =
        utledTittel(
            behandling = behandling,
            produserbartdokument = produserbardokument,
            dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbardokument),
            mottaker = mottaker,
            standardTekst = standardTekst
        )

    fun utledTittel(
        behandling: Behandling,
        produserbartdokument: Produserbaredokumenter,
        dokumentproduksjonsInfo: DokumentproduksjonsInfo,
        mottaker: Mottaker? = null,
        mottakerRolle: Mottakerroller? = null,
        brevbestilling: Brevbestilling? = null,
        standardTekst: String? = null
    ): String =
        when {
            produserbartdokument.erTrygdeavtaleVedtak() ->
                utledTittelTrygdeavtale(behandling, produserbartdokument, dokumentproduksjonsInfo, mottaker, mottakerRolle)

            standardTekst != null ->
                standardTekst

            produserbartdokument.erFritekstbrev() ->
                utledTittelFritekstbrev(dokumentproduksjonsInfo, brevbestilling as FritekstbrevBrevbestilling?)

            produserbartdokument == Produserbaredokumenter.VARSELBREV_MANGLENDE_INNBETALING ->
                utledTittelManglendeInnbetaling(behandling.id, dokumentproduksjonsInfo)

            else -> dokumentproduksjonsInfo.journalføringsTittel
        }

    private fun utledTittelManglendeInnbetaling(behandlingID: Long, dokumentproduksjonsInfo: DokumentproduksjonsInfo): String {
        val erFrivilligMedlemskap = medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID).any { it.erFrivillig() }
        return if (erFrivilligMedlemskap) dokumentproduksjonsInfo.alternativTittel else dokumentproduksjonsInfo.journalføringsTittel
    }

    private fun utledTittelFritekstbrev(dokumentproduksjonsInfo: DokumentproduksjonsInfo, brevbestilling: FritekstbrevBrevbestilling?): String {
        if (brevbestilling == null) {
            return dokumentproduksjonsInfo.journalføringsTittel
        }

        val tittel = brevbestilling.dokumentTittel ?: brevbestilling.fritekstTittel ?: ""

        if (!hasText(tittel)) {
            throw FunksjonellException("Tittel til fritekstbrev mangler, behandlingId:" + brevbestilling.behandlingId)
        }

        if ("Request to remain subject to Norwegian legislation" == tittel) {
            return "Søknad om unntak"
        }

        return tittel
    }

    private fun utledTittelTrygdeavtale(
        behandling: Behandling,
        produserbartdokument: Produserbaredokumenter,
        dokumentproduksjonsInfo: DokumentproduksjonsInfo,
        mottaker: Mottaker?,
        mottakerRolle: Mottakerroller?
    ): String {
        val mottakerAvBrev = mottaker ?: brevmottakerService.avklarMottaker(produserbartdokument, Mottaker.medRolle(mottakerRolle), behandling)
        val tittel = utledTittelTrygdeavtale(behandling.id, dokumentproduksjonsInfo, mottakerAvBrev)
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

    private fun Produserbaredokumenter.erFritekstbrev(): Boolean =
        listOf(
            Produserbaredokumenter.FRITEKSTBREV,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET,
            Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
            Produserbaredokumenter.GENERELT_FRITEKSTVEDLEGG,
            Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV
        ).contains(this)

    private fun Produserbaredokumenter.erTrygdeavtaleVedtak(): Boolean =
        listOf(
            Produserbaredokumenter.TRYGDEAVTALE_AU,
            Produserbaredokumenter.TRYGDEAVTALE_GB,
            Produserbaredokumenter.TRYGDEAVTALE_US,
            Produserbaredokumenter.TRYGDEAVTALE_CAN
        ).contains(this)
}
