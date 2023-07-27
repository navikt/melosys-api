package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.integrasjon.dokgen.dto.felles.Innvilgelse
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.Periode
import java.time.LocalDate

class InnvilgelseFtrl(
    brevbestilling: InnvilgelseBrevbestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    val innvilgelse: Innvilgelse,
    val perioder: List<Periode>,
    val bestemmelse: Folketrygdloven_kap2_bestemmelser?,
    val avslåttHelsedelFørMottaksdato: Boolean,
    val trygdeavgiftMottaker: Trygdeavgiftmottaker?,
    val skatteplikttype: Skatteplikttype?,
    val ftrl_2_8_begrunnelse: Ftrl_2_8_naer_tilknytning_norge_begrunnelser?,
    val begrunnelseAnnenGrunnFritekst: String?,
    val arbeidsgivere: List<String>,
    val arbeidsland: String?,
    val trygdeavtaleMedArbeidsland: Boolean,
    val arbeidsgiverFullmektigNavn: String?,
    val brukerHarFullmektig: Boolean,
    val betalerArbeidsgiveravgift: Boolean
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    class Builder(val brevbestilling: InnvilgelseBrevbestilling) {
        private val datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt)
        private val innvilgelse = Innvilgelse.av(brevbestilling)
        private val brukerHarFullmektig =
            brevbestilling.behandling.fagsak.finnRepresentant(Representerer.BRUKER).isPresent

        private var perioder: List<Periode> = emptyList()
        private var bestemmelse: Folketrygdloven_kap2_bestemmelser? = null
        private var avslåttHelsedelFørMottaksdato = false
        private var trygdeavgiftMottaker: Trygdeavgiftmottaker? = null
        private var skatteplikttype: Skatteplikttype? = null
        private var ftrl_2_8_begrunnelse: Ftrl_2_8_naer_tilknytning_norge_begrunnelser? = null
        private var begrunnelseAnnenGrunnFritekst: String? = null
        private var arbeidsgivere: List<String> = emptyList()
        private var arbeidsland: String? = null
        private var trygdeavtaleMedArbeidsland = false
        private var arbeidsgiverFullmektigNavn: String? = null
        private var betalerArbeidsgiveravgift = false

        fun perioder(perioder: List<Periode>): Builder {
            this.perioder = perioder
            return this
        }

        fun bestemmelse(bestemmelse: Folketrygdloven_kap2_bestemmelser?): Builder {
            this.bestemmelse = bestemmelse
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

        fun ftrl_2_8_begrunnelse(ftrl_2_8_begrunnelse: Ftrl_2_8_naer_tilknytning_norge_begrunnelser?): Builder {
            this.ftrl_2_8_begrunnelse = ftrl_2_8_begrunnelse
            return this
        }

        fun begrunnelseAnnenGrunnFritekst(begrunnelseAnnenGrunnFritekst: String?): Builder {
            this.begrunnelseAnnenGrunnFritekst = begrunnelseAnnenGrunnFritekst
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

        fun arbeidsgiverFullmektigNavn(arbeidsgiverFullmektigNavn: String?): Builder {
            this.arbeidsgiverFullmektigNavn = arbeidsgiverFullmektigNavn
            return this
        }

        fun betalerArbeidsgiveravgift(betalerArbeidsgiveravgift: Boolean): Builder {
            this.betalerArbeidsgiveravgift = betalerArbeidsgiveravgift
            return this
        }

        fun build(): InnvilgelseFtrl {
            return InnvilgelseFtrl(
                brevbestilling,
                datoMottatt,
                innvilgelse,
                perioder,
                bestemmelse,
                avslåttHelsedelFørMottaksdato,
                trygdeavgiftMottaker,
                skatteplikttype,
                ftrl_2_8_begrunnelse,
                begrunnelseAnnenGrunnFritekst,
                arbeidsgivere,
                arbeidsland,
                trygdeavtaleMedArbeidsland,
                arbeidsgiverFullmektigNavn,
                brukerHarFullmektig,
                betalerArbeidsgiveravgift,
            )
        }
    }
}
