package no.nav.melosys.service.dokument.brev

import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted
import java.time.LocalDate


class BrevDataA001(
    saksbehandler: String? = null,
    fritekst: String? = null,
    begrunnelseKode: String? = null,
    var utenlandskMyndighet: UtenlandskMyndighet? = null,
    var persondata: Persondata? = null,
    var bostedsadresse: StrukturertAdresse? = null,
    var bostedsadresseTypeKode: BostedsadresseTypeKode? = null,
    var utenlandskIdent: String? = null,
    var arbeidsgivendeVirksomheter: List<AvklartVirksomhet> = emptyList(),
    var selvstendigeVirksomheter: List<AvklartVirksomhet> = emptyList(),
    var arbeidssteder: List<Arbeidssted> = emptyList(),
    var tidligereAnmodninger: List<LocalDate> = emptyList(),
    var anmodningsperioder: Collection<Anmodningsperiode> = emptyList(),
    var tidligereLovvalgsperioder: Collection<Lovvalgsperiode> = emptyList(),
    var ansettelsesperiode: Periode? = null,
    var anmodningUtenArt12Begrunnelser: Set<VilkaarBegrunnelse> = emptySet(),
    var anmodningBegrunnelser: Set<VilkaarBegrunnelse> = emptySet(),
    var anmodningFritekstBegrunnelse: String? = null,
    var ytterligereInformasjon: String? = null
) : BrevData(saksbehandler, fritekst, begrunnelseKode)
