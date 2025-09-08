package no.nav.melosys.domain.eessi.sed

open class SedGrunnlagA003Dto : SedGrunnlagDto() {
    var overgangsregelbestemmelser: MutableList<Bestemmelse> = mutableListOf()
    var norskeArbeidsgivendeVirksomheter: MutableList<Virksomhet> = mutableListOf()
}
