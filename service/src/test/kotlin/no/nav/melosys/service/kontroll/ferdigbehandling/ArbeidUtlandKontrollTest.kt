package no.nav.melosys.service.kontroll.ferdigbehandling

import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.arbeidsstedLandManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.foretakUtlandManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.luftfartArbeidsstedManglerFelter
import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll.Companion.maritimtArbeidsstedManglerFelter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class ArbeidUtlandKontrollTest {
    private var mottatteOpplysningerData: MottatteOpplysningerData? = null

    @BeforeEach
    fun setup() {
        mottatteOpplysningerData = MottatteOpplysningerData()
        val fysiskArbeidssted = FysiskArbeidssted()
        val maritimtArbeid = MaritimtArbeid()
        val luftfartBase = LuftfartBase()
        val foretakUtland = ForetakUtland();
        fysiskArbeidssted.virksomhetNavn = " "
        maritimtArbeid.enhetNavn = " "
        luftfartBase.hjemmebaseNavn = " "
        foretakUtland.selvstendigNæringsvirksomhet = false
        maritimtArbeid.fartsomradeKode = Fartsomrader.INNENRIKS
        mottatteOpplysningerData!!.arbeidPaaLand.fysiskeArbeidssteder = listOf(fysiskArbeidssted)
        mottatteOpplysningerData!!.maritimtArbeid = listOf(maritimtArbeid)
        mottatteOpplysningerData!!.luftfartBaser = listOf(luftfartBase)
        mottatteOpplysningerData!!.foretakUtland = listOf(foretakUtland)
    }

    @Test
    fun utførKontroller_arbeidsstedLandManglerFelter_returnererKode() {
        val kontrollfeil = arbeidsstedLandManglerFelter(mottatteOpplysningerData!!)!!
        assert(kontrollfeil.kode == Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND)
    }

    @Test
    fun utførKontroller_arbeidsstedMaritimManglerFelter_returnererKode() {
        val kontrollfeil = maritimtArbeidsstedManglerFelter(mottatteOpplysningerData!!)!!
        assert(kontrollfeil.kode == Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_MARITIM)
    }

    @Test
    fun utførKontroller_arbeidsstedFlyManglerFelter_returnererKode() {
        val kontrollfeil = luftfartArbeidsstedManglerFelter(mottatteOpplysningerData!!)!!
        assert(kontrollfeil.kode == Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LUFTFART)
    }

    @Test
    fun utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        val kontrollfeil = foretakUtlandManglerFelter(mottatteOpplysningerData!!)!!
        assert(kontrollfeil.kode == Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL)
    }
}
