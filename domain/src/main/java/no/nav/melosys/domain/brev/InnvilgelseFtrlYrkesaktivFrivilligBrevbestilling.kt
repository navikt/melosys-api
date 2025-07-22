package no.nav.melosys.domain.brev

class InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling : DokgenBrevbestilling {
    var nyVurderingBakgrunn: String? = null
    var innledningFritekst: String? = null
    var begrunnelseFritekst: String? = null
    var trygdeavgiftFritekst: String? = null
    var ukjentSluttdatoMedlemskapsperiode: Boolean? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn
        this.innledningFritekst = builder.innledningFritekst
        this.begrunnelseFritekst = builder.begrunnelseFritekst
        this.trygdeavgiftFritekst = builder.trygdeavgiftFritekst
        this.ukjentSluttdatoMedlemskapsperiode = builder.ukjentSluttdatoMedlemskapsperiode
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var nyVurderingBakgrunn: String? = null
        internal var innledningFritekst: String? = null
        internal var begrunnelseFritekst: String? = null
        internal var trygdeavgiftFritekst: String? = null
        internal var ukjentSluttdatoMedlemskapsperiode: Boolean? = null

        constructor()

        constructor(brevbestilling: InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling) : super(brevbestilling) {
            this.nyVurderingBakgrunn = brevbestilling.nyVurderingBakgrunn
            this.innledningFritekst = brevbestilling.innledningFritekst
            this.begrunnelseFritekst = brevbestilling.begrunnelseFritekst
            this.trygdeavgiftFritekst = brevbestilling.trygdeavgiftFritekst
            this.ukjentSluttdatoMedlemskapsperiode = brevbestilling.ukjentSluttdatoMedlemskapsperiode
        }

        fun medInnledningFritekst(innledningFritekst: String?) = apply { this.innledningFritekst = innledningFritekst }

        fun medBegrunnelseFritekst(begrunnelseFritekst: String?) = apply { this.begrunnelseFritekst = begrunnelseFritekst }

        fun medTrygdeavgiftFritekst(trygdeavgiftFritekst: String?) = apply { this.trygdeavgiftFritekst = trygdeavgiftFritekst }

        fun medNyVurderingBakgrunn(nyVurderingBakgrunn: String?) = apply { this.nyVurderingBakgrunn = nyVurderingBakgrunn }

        fun medUkjentSluttdatoMedlemskapsperiode(ukjentSluttdatoMedlemskapsperiode: Boolean?) = apply { this.ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode }

        override fun build(): InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling = InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling(this)
    }
}
