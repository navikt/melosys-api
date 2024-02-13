package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.VilkaarBegrunnelse

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet

import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper

class BrevDataAnmodningUnntak(
    saksbehandler: String,
    var arbeidsland: String? = null,
    var hovedvirksomhet: AvklartVirksomhet? = null,
    var yrkesaktivitet: Yrkesaktivitetstyper? = null,
    var anmodningBegrunnelser: Set<VilkaarBegrunnelse> = emptySet(),
    var anmodningUtenArt12Begrunnelser: Set<VilkaarBegrunnelse> = emptySet(),
    var anmodningFritekst: String? = null
) : BrevData(saksbehandler = saksbehandler)
