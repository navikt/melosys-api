package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftsperiodeDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import java.time.LocalDate

class InnvilgelseFtrlYrkesaktivFrivillig(
    brevbestilling: InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling,
    val behandlingstype: Behandlingstyper,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avgiftsperioder: List<AvgiftsperiodeDto>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
    val bestemmelse: Bestemmelse?,
    val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean,
    val avslåttMedlemskapsperiodeFørMottaksdatoFullDekning: Boolean,
    val trygdeavgiftMottaker: Trygdeavgiftmottaker?,
    val fullmektigTrygdeavgift: String?,
    val skatteplikttype: Skatteplikttype?,
    val begrunnelse: Kodeverk?,
    val begrunnelseAnnenGrunnFritekst: String?,
    val nyVurderingBakgrunn: String?,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
    val trygdeavgiftFritekst: String?,
    val arbeidsgivere: List<String>,
    val flereLandUkjentHvilke: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val land: List<String>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val trygdeavtaleLand: List<String>,
    val betalerArbeidsgiveravgift: Boolean,
    val ukjentSluttdato: Boolean,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(
        brevbestilling: InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling,
        behandlingstype: Behandlingstyper,
        avgiftsperioder: List<AvgiftsperiodeDto>,
        medlemskapsperioder: List<MedlemskapsperiodeDto>,
        bestemmelse: Bestemmelse,
        avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean,
        avslåttMedlemskapsperiodeFørMottaksdatoFullDekning: Boolean,
        trygdeavgiftMottaker: Trygdeavgiftmottaker?,
        fullmektigTrygdeavgift: String?,
        skatteplikttype: Skatteplikttype,
        begrunnelse: Kodeverk?,
        begrunnelseAnnenGrunnFritekst: String?,
        nyVurderingBakgrunn: String?,
        innledningFritekst: String?,
        begrunnelseFritekst: String?,
        trygdeavgiftFritekst: String?,
        arbeidsgivere: List<String>,
        flereLandUkjentHvilke: Boolean,
        land: List<String>,
        trygdeavtaleLand: List<String>,
        betalerArbeidsgiveravgift: Boolean,
        ukjentSluttdato: Boolean
    ) : this(
        brevbestilling,
        behandlingstype,
        datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt),
        avgiftsperioder,
        medlemskapsperioder,
        bestemmelse,
        avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
        avslåttMedlemskapsperiodeFørMottaksdatoFullDekning,
        trygdeavgiftMottaker,
        fullmektigTrygdeavgift,
        skatteplikttype,
        begrunnelse,
        begrunnelseAnnenGrunnFritekst,
        nyVurderingBakgrunn,
        innledningFritekst,
        begrunnelseFritekst,
        trygdeavgiftFritekst,
        arbeidsgivere,
        flereLandUkjentHvilke,
        land,
        trygdeavtaleLand,
        betalerArbeidsgiveravgift,
        ukjentSluttdato
    )
}
