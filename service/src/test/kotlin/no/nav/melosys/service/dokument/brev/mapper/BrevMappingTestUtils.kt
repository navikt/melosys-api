package no.nav.melosys.service.dokument.brev.mapper

import no.nav.dok.brevdata.felles.v1.navfelles.*
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon

object BrevMappingTestUtils {

    fun lagNAVFelles() = MelosysNAVFelles().apply {
        mottaker = lagMottaker()
        sakspart = lagSakspart()

        val navEnhet = NavEnhet()
            .withEnhetsId("4567")
            .withEnhetsNavn("MEL")
        behandlendeEnhet = navEnhet

        val navAnsatt = NavAnsatt()
            .withAnsattId("A94840")
            .withNavn("Aleksander Z")

        val saksbehandler = Saksbehandler()
            .withNavEnhet(navEnhet)
            .withNavAnsatt(navAnsatt)
        signerendeSaksbehandler = saksbehandler
        signerendeBeslutter = saksbehandler
        kontaktinformasjon = lagKontaktInformasjon()
    }

    private fun lagMottaker(): Mottaker = Person().apply {
        id = "ID"
        typeKode = AktoerType.PERSON
        kortNavn = "Nvn"
        navn = "Navn"
        mottakeradresse = lagAdresse()
        spraakkode = Spraakkode.NB
    }

    private fun lagAdresse(): Adresse = NorskPostadresse()
        .withAdresselinje1("Gate")
        .withAdresselinje2("12B")
        .withPoststed("Sted")
        .withPostnummer("4321")
        .withLand(Landkoder.BG.kode)

    private fun lagSakspart(): Sakspart = Sakspart().apply {
        id = "AktørID"
        typeKode = AktoerType.PERSON
        navn = "Navn"
    }

    fun lagFellesType(): FellesType = FellesType().withFagsaksnummer("MELTEST-1")
}
