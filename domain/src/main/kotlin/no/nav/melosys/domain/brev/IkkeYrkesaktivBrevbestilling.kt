package no.nav.melosys.domain.brev

import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype
import java.time.LocalDate

class IkkeYrkesaktivBrevbestilling : DokgenBrevbestilling {
    var begrunnelseFritekst: String? = null
    var innledningFritekst: String? = null
    var nyVurderingBakgrunn: String? = null
    var oppholdsLand: String? = null
    var periodeFom: LocalDate? = null
    var periodeTom: LocalDate? = null
    var bestemmelse: String? = null
    var artikkel: String? = null
    var ikkeYrkesaktivSituasjontype: Ikkeyrkesaktivsituasjontype? = null

    constructor() : super()

    private constructor(builder: Builder) : super(builder) {
        this.begrunnelseFritekst = builder.begrunnelseFritekst
        this.innledningFritekst = builder.innledningFritekst
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn
        this.oppholdsLand = builder.oppholdsLand
        this.periodeFom = builder.periodeFom
        this.periodeTom = builder.periodeTom
        this.bestemmelse = builder.bestemmelse
        this.ikkeYrkesaktivSituasjontype = builder.ikkeYrkesaktivSituasjontype
        this.artikkel = builder.artikkel
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder?> {
        internal var begrunnelseFritekst: String? = null
        var innledningFritekst: String? = null
        var oppholdsLand: String? = null
        var nyVurderingBakgrunn: String? = null
        var periodeFom: LocalDate? = null
        var periodeTom: LocalDate? = null
        var bestemmelse: String? = null
        var ikkeYrkesaktivSituasjontype: Ikkeyrkesaktivsituasjontype? = null
        var artikkel: String? = null

        constructor()

        constructor(ikkeYrkesaktivBrevbestilling: IkkeYrkesaktivBrevbestilling) : super(ikkeYrkesaktivBrevbestilling) {
            this.begrunnelseFritekst = ikkeYrkesaktivBrevbestilling.begrunnelseFritekst
            this.innledningFritekst = ikkeYrkesaktivBrevbestilling.innledningFritekst
            this.oppholdsLand = ikkeYrkesaktivBrevbestilling.oppholdsLand
            this.periodeFom = ikkeYrkesaktivBrevbestilling.periodeFom
            this.periodeTom = ikkeYrkesaktivBrevbestilling.periodeTom
            this.bestemmelse = ikkeYrkesaktivBrevbestilling.bestemmelse
            this.ikkeYrkesaktivSituasjontype = ikkeYrkesaktivBrevbestilling.ikkeYrkesaktivSituasjontype
            this.nyVurderingBakgrunn = ikkeYrkesaktivBrevbestilling.nyVurderingBakgrunn
            this.artikkel = ikkeYrkesaktivBrevbestilling.artikkel
        }

        fun medBegrunnelseFritekst(begrunnelseFritekst: String?): Builder {
            this.begrunnelseFritekst = begrunnelseFritekst
            return this
        }

        fun medNyVurderingBakgrunn(nyVurderingBakgrunn: String?): Builder {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn
            return this
        }

        fun medPeriodeFom(periodeFom: LocalDate?): Builder {
            this.periodeFom = periodeFom
            return this
        }

        fun medPeriodeTom(periodeTom: LocalDate?): Builder {
            this.periodeTom = periodeTom
            return this
        }

        fun medBestemmelse(bestemmelse: String?): Builder {
            this.bestemmelse = bestemmelse
            return this
        }

        fun medArtikkel(artikkel: String?): Builder {
            this.artikkel = artikkel
            return this
        }

        fun medInnledningFritekst(innledningFritekst: String?): Builder {
            this.innledningFritekst = innledningFritekst
            return this
        }

        fun medOppholdsLand(oppholdsLand: String?): Builder {
            this.oppholdsLand = oppholdsLand
            return this
        }

        fun medIkkeyrkesaktivSituasjontype(ikkeYrkesaktivSituasjontype: Ikkeyrkesaktivsituasjontype?): Builder {
            this.ikkeYrkesaktivSituasjontype = ikkeYrkesaktivSituasjontype
            return this
        }

        override fun build(): IkkeYrkesaktivBrevbestilling {
            return IkkeYrkesaktivBrevbestilling(this)
        }
    }
}
