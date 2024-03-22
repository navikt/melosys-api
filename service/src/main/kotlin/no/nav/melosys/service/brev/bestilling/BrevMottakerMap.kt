package no.nav.melosys.service.brev.bestilling

import no.nav.melosys.domain.brev.BrevkopiRegel
import no.nav.melosys.domain.brev.Mottakerliste
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter

object BrevMottakerMap {
    private val map = mapOf(
        Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD to Mottakerliste(Mottakerroller.BRUKER),
        Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE to Mottakerliste(Mottakerroller.BRUKER),

        Produserbaredokumenter.MANGELBREV_BRUKER to Mottakerliste(Mottakerroller.BRUKER),
        Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER to Mottakerliste(Mottakerroller.ARBEIDSGIVER, listOf(BrevkopiRegel.BRUKER_FÅR_KOPI)),

        Produserbaredokumenter.INNVILGELSE_FOLKETRYGDLOVEN to Mottakerliste(Mottakerroller.BRUKER),
        Produserbaredokumenter.IKKE_YRKESAKTIV_PLIKTIG_FTRL to Mottakerliste(Mottakerroller.BRUKER),
        Produserbaredokumenter.IKKE_YRKESAKTIV_FRIVILLIG_FTRL to Mottakerliste(Mottakerroller.BRUKER),

        Produserbaredokumenter.IKKE_YRKESAKTIV_VEDTAKSBREV to Mottakerliste(Mottakerroller.BRUKER),

        Produserbaredokumenter.VEDTAK_OPPHOERT_MEDLEMSKAP to Mottakerliste(Mottakerroller.BRUKER),

        Produserbaredokumenter.TRYGDEAVTALE_GB to
            Mottakerliste(Mottakerroller.BRUKER, listOf(BrevkopiRegel.ARBEIDSGIVER_FÅR_KOPI, BrevkopiRegel.UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI_HVIS_IKKE_ART_8_2)),
        Produserbaredokumenter.TRYGDEAVTALE_US to
            Mottakerliste(Mottakerroller.BRUKER, listOf(BrevkopiRegel.ARBEIDSGIVER_FÅR_KOPI, BrevkopiRegel.UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI)),
        Produserbaredokumenter.TRYGDEAVTALE_CAN to
            Mottakerliste(Mottakerroller.BRUKER, listOf(BrevkopiRegel.ARBEIDSGIVER_FÅR_KOPI, BrevkopiRegel.UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI)),
        Produserbaredokumenter.TRYGDEAVTALE_AU to
            Mottakerliste(Mottakerroller.BRUKER, listOf(BrevkopiRegel.ARBEIDSGIVER_FÅR_KOPI, BrevkopiRegel.UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI)),

        Produserbaredokumenter.GENERELT_FRITEKSTBREV_BRUKER to Mottakerliste(Mottakerroller.BRUKER),
        Produserbaredokumenter.GENERELT_FRITEKSTBREV_ARBEIDSGIVER to
            Mottakerliste(Mottakerroller.ARBEIDSGIVER, listOf(BrevkopiRegel.BRUKER_FÅR_KOPI)),
        Produserbaredokumenter.GENERELT_FRITEKSTBREV_VIRKSOMHET to Mottakerliste(Mottakerroller.VIRKSOMHET),
        Produserbaredokumenter.FRITEKSTBREV to Mottakerliste(Mottakerroller.NORSK_MYNDIGHET),
        Produserbaredokumenter.UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV to Mottakerliste(Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET)
    )

    fun getMap() = map
}
