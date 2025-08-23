package no.nav.melosys.domain.brev

import java.time.LocalDate
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype

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

    constructor(builder: Builder) : super(builder) {
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

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var begrunnelseFritekst: String? = null
        internal var innledningFritekst: String? = null
        internal var oppholdsLand: String? = null
        internal var nyVurderingBakgrunn: String? = null
        internal var periodeFom: LocalDate? = null
        internal var periodeTom: LocalDate? = null
        internal var bestemmelse: String? = null
        internal var ikkeYrkesaktivSituasjontype: Ikkeyrkesaktivsituasjontype? = null
        internal var artikkel: String? = null

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

        fun medBegrunnelseFritekst(begrunnelseFritekst: String?) = apply { this.begrunnelseFritekst = begrunnelseFritekst }

        fun medNyVurderingBakgrunn(nyVurderingBakgrunn: String?) = apply { this.nyVurderingBakgrunn = nyVurderingBakgrunn }

        fun medPeriodeFom(periodeFom: LocalDate?) = apply { this.periodeFom = periodeFom }

        fun medPeriodeTom(periodeTom: LocalDate?) = apply { this.periodeTom = periodeTom }

        fun medBestemmelse(bestemmelse: String?) = apply { this.bestemmelse = bestemmelse }

        fun medArtikkel(artikkel: String?) = apply { this.artikkel = artikkel }

        fun medInnledningFritekst(innledningFritekst: String?) = apply { this.innledningFritekst = innledningFritekst }

        fun medOppholdsLand(oppholdsLand: String?) = apply { this.oppholdsLand = oppholdsLand }

        fun medIkkeyrkesaktivSituasjontype(ikkeYrkesaktivSituasjontype: Ikkeyrkesaktivsituasjontype?) = apply { this.ikkeYrkesaktivSituasjontype = ikkeYrkesaktivSituasjontype }

        override fun build(): IkkeYrkesaktivBrevbestilling = IkkeYrkesaktivBrevbestilling(this)
    }
}
