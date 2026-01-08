package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import java.math.BigDecimal
import java.time.LocalDate

class InnvilgelseFtrlPensjonistFrivillig(
    brevbestilling: DokgenBrevbestilling,
    val behandlingstype: Behandlingstyper,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avgiftsperioder: List<AvgiftsperiodePensjonist>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
    val bestemmelse: Bestemmelse?,
    val avslåttMedlemskapsIPensjonsdel: Boolean,
    val avslåttMedlemskapsIPensjonsdelMenIkkeHelsedel: Boolean,
    val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean,
    val trygdeavgiftMottaker: Trygdeavgiftmottaker?,
    val fullmektigTrygdeavgift: String?,
    val begrunnelse: Kodeverk?,
    val begrunnelseAnnenGrunnFritekst: String?,
    val nyVurderingBakgrunn: String?,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
    val trygdeavgiftFritekst: String?,
    val flereLandUkjentHvilke: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val land: List<String>,
    val trygdeavtaleLand: List<String>,
    val ukjentSluttdatoMedlemskapsperiode: Boolean,
    val betalingsvalg: Betalingstype,
    val harMedlemskapsperioderIForegåendeÅr: Boolean
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(
        brevbestilling: DokgenBrevbestilling,
        behandlingstype: Behandlingstyper,
        avgiftsperioder: List<AvgiftsperiodePensjonist>,
        medlemskapsperioder: List<MedlemskapsperiodeDto>,
        bestemmelse: Bestemmelse,
        avslåttMedlemskapsIPensjonsdel: Boolean,
        avslåttMedlemskapsIPensjonsdelMenIkkeHelsedel: Boolean,
        avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean,
        trygdeavgiftMottaker: Trygdeavgiftmottaker?,
        fullmektigTrygdeavgift: String?,
        begrunnelse: Kodeverk?,
        begrunnelseAnnenGrunnFritekst: String?,
        nyVurderingBakgrunn: String?,
        innledningFritekst: String?,
        begrunnelseFritekst: String?,
        trygdeavgiftFritekst: String?,
        flereLandUkjentHvilke: Boolean,
        land: List<String>,
        trygdeavtaleLand: List<String>,
        ukjentSluttdatoMedlemskapsperiode: Boolean,
        betalingsvalg: Betalingstype,
        harMedlemskapsperioderIForegåendeÅr: Boolean
    ) : this(
        brevbestilling,
        behandlingstype,
        datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt),
        avgiftsperioder,
        medlemskapsperioder,
        bestemmelse,
        avslåttMedlemskapsIPensjonsdel,
        avslåttMedlemskapsIPensjonsdelMenIkkeHelsedel,
        avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
        trygdeavgiftMottaker,
        fullmektigTrygdeavgift,
        begrunnelse,
        begrunnelseAnnenGrunnFritekst,
        nyVurderingBakgrunn,
        innledningFritekst,
        begrunnelseFritekst,
        trygdeavgiftFritekst,
        flereLandUkjentHvilke,
        land,
        trygdeavtaleLand,
        ukjentSluttdatoMedlemskapsperiode,
        betalingsvalg,
        harMedlemskapsperioderIForegåendeÅr
    )
}

data class AvgiftsperiodePensjonist(
    val fom: LocalDate,
    val tom: LocalDate,
    val avgiftssats: BigDecimal,
    val avgiftPerMd: BigDecimal,
    val avgiftspliktigInntektPerMd: BigDecimal,
    val inntektskilde: String,
    val inntektskildetype: String,
    val trygdedekning: String,
    val arbeidsgiveravgiftBetalt: SvarAlternativ,
    val skatteplikt: Boolean
)
