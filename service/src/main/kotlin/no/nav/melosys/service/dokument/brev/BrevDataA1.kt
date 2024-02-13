package no.nav.melosys.service.dokument.brev

import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted


class BrevDataA1(
    saksbehandler: String? = null,
    fritekst: String? = null,
    begrunnelseKode: String? = null,
    var yrkesgruppe: Yrkesgrupper? = null,
    var person: Persondata? = null,
    var hovedvirksomhet: AvklartVirksomhet? = null,
    var arbeidssteder: List<Arbeidssted>? = null,
    var arbeidsland: Collection<Land_iso2>? = null,
    var erUkjenteEllerAlleEosLand: Boolean = false,
    var bostedsadresse: StrukturertAdresse? = null,
    var bivirksomheter: Collection<AvklartVirksomhet>? = null
) : BrevData(saksbehandler, fritekst, begrunnelseKode)
