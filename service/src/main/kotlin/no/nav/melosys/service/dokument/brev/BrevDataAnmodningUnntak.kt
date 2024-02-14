package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.VilkaarBegrunnelse

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet

import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper

class BrevDataAnmodningUnntak(
    saksbehandler: String,
    val arbeidsland: String? = null,
    val hovedvirksomhet: AvklartVirksomhet? = null,
    val yrkesaktivitet: Yrkesaktivitetstyper? = null,
    var anmodningBegrunnelser: Set<VilkaarBegrunnelse> = emptySet(),
    val anmodningUtenArt12Begrunnelser: Set<VilkaarBegrunnelse> = emptySet(),
    val anmodningFritekst: String? = null
) : BrevData(saksbehandler = saksbehandler)
