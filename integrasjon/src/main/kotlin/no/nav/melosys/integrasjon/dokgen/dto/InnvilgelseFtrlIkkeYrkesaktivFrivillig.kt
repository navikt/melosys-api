package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import java.time.LocalDate

class InnvilgelseFtrlIkkeYrkesaktivFrivillig(
    brevbestilling: DokgenBrevbestilling,
    val behandlingstype: Behandlingstyper,
    val sakstype: Sakstyper,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate,
    val flereLandUkjentHvilke: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val land: List<String>,
    val bestemmelse: Bestemmelse,
    val nyVurderingBakgrunn: String?,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
    val ikkeYrkesaktivRelasjonType: String?,
    val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean,
    val avslåttMedlemskapsperiodeFørMottaksdatoFullDekning: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
    ukjentSluttdato: Boolean,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(
        brevbestilling: DokgenBrevbestilling,
        flereLandUkjentHvilke: Boolean,
        land: List<String>,
        bestemmelse: Bestemmelse,
        nyVurderingBakgrunn: String?,
        innledningFritekst: String?,
        begrunnelseFritekst: String?,
        ikkeYrkesaktivRelasjonType: String?,
        avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean,
        avslåttMedlemskapsperiodeFørMottaksdatoFullDekning: Boolean,
        medlemskapsperioder: List<MedlemskapsperiodeDto>,
        ukjentSluttdato: Boolean,
    ) : this(
        brevbestilling = brevbestilling,
        behandlingstype = brevbestilling.behandling.type,
        sakstype = brevbestilling.behandling.fagsak.type,
        datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt),
        flereLandUkjentHvilke = flereLandUkjentHvilke,
        land = land,
        bestemmelse = bestemmelse,
        nyVurderingBakgrunn = nyVurderingBakgrunn,
        innledningFritekst = innledningFritekst,
        begrunnelseFritekst = begrunnelseFritekst,
        ikkeYrkesaktivRelasjonType = ikkeYrkesaktivRelasjonType,
        avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
        avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = avslåttMedlemskapsperiodeFørMottaksdatoFullDekning,
        medlemskapsperioder = medlemskapsperioder,
        ukjentSluttdato = ukjentSluttdato
    )
}
