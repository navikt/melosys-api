package no.nav.melosys.domain.brev

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException

data class Mottaker(
    var rolle: Mottakerroller? = null,
    var aktørId: String? = null,
    var personIdent: String? = null,
    var orgnr: String? = null,
    var institusjonID: String? = null,
    var trygdemyndighetLand: Land_iso2? = null
) {

    fun orgnrNonNull(): String = orgnr ?: throw FunksjonellException("Mottaker mangler orgnr")

    fun erOrganisasjon(): Boolean = when (rolle) {
        Mottakerroller.BRUKER, Mottakerroller.ANNEN_PERSON -> false
        Mottakerroller.FULLMEKTIG -> orgnr != null
        else -> true
    }

    fun erUtenlandskMyndighet(): Boolean =
        rolle == Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET && (institusjonID != null || trygdemyndighetLand != null)

    fun hentMyndighetLandkode(): Land_iso2 = when {
        erUtenlandskMyndighet() -> when {
            institusjonID != null -> UtenlandskMyndighet.konverterInstitusjonIdTilLandkode(institusjonID)
            else -> trygdemyndighetLand!!
        }
        else -> throw TekniskException("Mottaker er ikke en utenlandsk myndighet")
    }

    companion object {
        @JvmStatic
        fun medRolle(rolle: Mottakerroller?) = Mottaker(rolle = rolle)

        @JvmStatic
        fun av(aktoer: Aktoer) = Mottaker(
            rolle = mottakerrolleAv(aktoer.rolle),
            aktørId = aktoer.aktørId,
            personIdent = aktoer.personIdent,
            orgnr = aktoer.orgnr,
            institusjonID = aktoer.institusjonID,
            trygdemyndighetLand = aktoer.trygdemyndighetLand
        )

        private fun mottakerrolleAv(aktoersrolle: Aktoersroller?): Mottakerroller = when (aktoersrolle) {
            Aktoersroller.BRUKER -> Mottakerroller.BRUKER
            Aktoersroller.VIRKSOMHET -> Mottakerroller.VIRKSOMHET
            Aktoersroller.ARBEIDSGIVER -> Mottakerroller.ARBEIDSGIVER
            Aktoersroller.FULLMEKTIG -> Mottakerroller.FULLMEKTIG
            Aktoersroller.TRYGDEMYNDIGHET -> Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET
            else -> throw FunksjonellException("Støtter ikke mapping av aktoersrolle$aktoersrolle")
        }

        @JvmStatic
        fun av(norskMyndighet: NorskMyndighet) = when (norskMyndighet) {
            NorskMyndighet.HELFO -> mottakerNorskMyndighet(NorskMyndighet.HELFO.orgnr)
            NorskMyndighet.SKATTEETATEN -> mottakerNorskMyndighet(NorskMyndighet.SKATTEETATEN.orgnr)
            NorskMyndighet.SKATTEINNKREVER_UTLAND -> mottakerNorskMyndighet(NorskMyndighet.SKATTEINNKREVER_UTLAND.orgnr)
        }

        private fun mottakerNorskMyndighet(orgnr: String?) = medRolle(Mottakerroller.NORSK_MYNDIGHET).apply {
            this.orgnr = orgnr
        }
    }
}
