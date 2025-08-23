package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import java.time.LocalDate

class InnvilgelsePensjonistPliktigFtrl(
    brevbestilling: DokgenBrevbestilling,
    val behandlingstype: Behandlingstyper,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avgiftsperioder: List<AvgiftsperiodePensjonist>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val medlemskapsperiode: MedlemskapsperiodeDto,
    val bestemmelse: Bestemmelse?,
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
    val ukjentSluttdatoMedlemskapsperiode: Boolean,
    val ikkeYrkesaktivOppholdType: String?,
    val ikkeYrkesaktivRelasjonType: String?,
    val betalingsvalg: Betalingstype
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    constructor(
        brevbestilling: DokgenBrevbestilling,
        behandlingstype: Behandlingstyper,
        avgiftsperioder: List<AvgiftsperiodePensjonist>,
        medlemskapsperiode: MedlemskapsperiodeDto,
        bestemmelse: Bestemmelse?,
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
        ukjentSluttdatoMedlemskapsperiode: Boolean,
        ikkeYrkesaktivOppholdType: String?,
        ikkeYrkesaktivRelasjonType: String?,
        betalingsvalg: Betalingstype
    ) : this(
        brevbestilling,
        behandlingstype,
        datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt),
        avgiftsperioder,
        medlemskapsperiode,
        bestemmelse,
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
        ukjentSluttdatoMedlemskapsperiode,
        ikkeYrkesaktivOppholdType,
        ikkeYrkesaktivRelasjonType,
        betalingsvalg
    )
}
