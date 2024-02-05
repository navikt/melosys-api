package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.InnvilgelseFtrlBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftsperiodeDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto
import java.time.LocalDate

class InnvilgelseFtrl(
    brevbestilling: InnvilgelseFtrlBrevbestilling,
    val behandlingstype: Behandlingstyper,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avgiftsperioder: List<AvgiftsperiodeDto>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val medlemskapsperioder: List<MedlemskapsperiodeDto>,
    val bestemmelse: Folketrygdloven_kap2_bestemmelser?,
    val avslåttHelsedelFørMottaksdato: Boolean,
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
    val arbeidsland: String?,
    val trygdeavtaleMedArbeidsland: Boolean,
    val betalerArbeidsgiveravgift: Boolean
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    class Builder(val brevbestilling: InnvilgelseFtrlBrevbestilling) {
        private val datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt)
        private lateinit var behandlingstype: Behandlingstyper
        private var avgiftsperioder: List<AvgiftsperiodeDto> = emptyList()
        private var medlemskapsperioder: List<MedlemskapsperiodeDto> = emptyList()
        private var bestemmelse: Folketrygdloven_kap2_bestemmelser? = null
        private var fullmektigTrygdeavgift: String? = null
        private var avslåttHelsedelFørMottaksdato = false
        private var trygdeavgiftMottaker: Trygdeavgiftmottaker? = null
        private var skatteplikttype: Skatteplikttype? = null
        private var begrunnelse: Kodeverk? = null
        private var begrunnelseAnnenGrunnFritekst: String? = null
        private var nyVurderingBakgrunn: String? = null
        private var innledningFritekst: String? = null
        private var begrunnelseFritekst: String? = null
        private var trygdeavgiftFritekst: String? = null
        private var arbeidsgivere: List<String> = emptyList()
        private var arbeidsland: String? = null
        private var trygdeavtaleMedArbeidsland = false
        private var betalerArbeidsgiveravgift = false

        fun behandlingstype(behandlingstype: Behandlingstyper): Builder {
            this.behandlingstype = behandlingstype
            return this
        }

        fun avgiftsperioder(avgiftsperioder: List<AvgiftsperiodeDto>): Builder {
            this.avgiftsperioder = avgiftsperioder
            return this
        }

        fun medlemskapsperioder(medlemskapsperioder: List<MedlemskapsperiodeDto> ): Builder {
            this.medlemskapsperioder = medlemskapsperioder
            return this
        }

        fun bestemmelse(bestemmelse: Folketrygdloven_kap2_bestemmelser?): Builder {
            this.bestemmelse = bestemmelse
            return this
        }

        fun fullmektigTrygdeavgift(fullmektigTrygdeavgift: String?): Builder {
            this.fullmektigTrygdeavgift = fullmektigTrygdeavgift
            return this
        }

        fun avslåttHelsedelFørMottaksdato(avslåttHelsedelFørMottaksdato: Boolean): Builder {
            this.avslåttHelsedelFørMottaksdato = avslåttHelsedelFørMottaksdato
            return this
        }

        fun trygdeavgiftMottaker(trygdeavgiftMottaker: Trygdeavgiftmottaker?): Builder {
            this.trygdeavgiftMottaker = trygdeavgiftMottaker
            return this
        }

        fun skatteplikttype(skatteplikttype: Skatteplikttype?): Builder {
            this.skatteplikttype = skatteplikttype
            return this
        }

        fun begrunnelse(begrunnelse: Kodeverk?): Builder {
            this.begrunnelse = begrunnelse
            return this
        }

        fun begrunnelseAnnenGrunnFritekst(begrunnelseAnnenGrunnFritekst: String?): Builder {
            this.begrunnelseAnnenGrunnFritekst = begrunnelseAnnenGrunnFritekst
            return this
        }

        fun nyVurderingBakgrunn(nyVurderingBakgrunn: String?): Builder {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn
            return this;
        }

        fun innledningFritekst(innledningFritekst: String?): Builder {
            this.innledningFritekst = innledningFritekst
            return this
        }

        fun begrunnelseFritekst(begrunnelseFritekst: String?): Builder {
            this.begrunnelseFritekst = begrunnelseFritekst
            return this
        }

        fun trygdeavgiftFritekst(trygdeavgiftFritekst: String?): Builder {
            this.trygdeavgiftFritekst = trygdeavgiftFritekst
            return this
        }

        fun arbeidsgivere(arbeidsgivere: List<String>): Builder {
            this.arbeidsgivere = arbeidsgivere
            return this
        }

        fun arbeidsland(arbeidsland: String?): Builder {
            this.arbeidsland = arbeidsland
            return this
        }

        fun trygdeavtaleMedArbeidsland(trygdeavtaleMedArbeidsland: Boolean): Builder {
            this.trygdeavtaleMedArbeidsland = trygdeavtaleMedArbeidsland
            return this
        }

        fun betalerArbeidsgiveravgift(betalerArbeidsgiveravgift: Boolean): Builder {
            this.betalerArbeidsgiveravgift = betalerArbeidsgiveravgift
            return this
        }

        fun build(): InnvilgelseFtrl {
            return InnvilgelseFtrl(
                brevbestilling,
                behandlingstype,
                datoMottatt,
                avgiftsperioder,
                medlemskapsperioder,
                bestemmelse,
                avslåttHelsedelFørMottaksdato,
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
                arbeidsland,
                trygdeavtaleMedArbeidsland,
                betalerArbeidsgiveravgift,
            )
        }
    }
}
