package no.nav.melosys.domain.brev

class InnvilgelseEftaStorbritanniaBrevbestilling : DokgenBrevbestilling {
    var innledningFritekst: String? = null
    var begrunnelseFritekst: String? = null
    var innvilgelseFritekst: String? = null
    var nyVurderingBakgrunn: String? = null

    constructor() : super()

    private constructor(builder: Builder) : super(builder) {
        this.innledningFritekst = builder.innledningFritekst
        this.begrunnelseFritekst = builder.begrunnelseFritekst
        this.innvilgelseFritekst = builder.innvilgelseFritekst
        this.nyVurderingBakgrunn = builder.nyVurderingBakgrunn
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var innledningFritekst: String? = null
        internal var begrunnelseFritekst: String? = null
        internal var innvilgelseFritekst: String? = null
        internal var nyVurderingBakgrunn: String? = null

        constructor()

        constructor(innvilgelseBrevbestilling: InnvilgelseEftaStorbritanniaBrevbestilling) : super(innvilgelseBrevbestilling) {
            this.innledningFritekst = innvilgelseBrevbestilling.innledningFritekst
            this.begrunnelseFritekst = innvilgelseBrevbestilling.begrunnelseFritekst
            this.innvilgelseFritekst = innvilgelseBrevbestilling.innvilgelseFritekst
            this.nyVurderingBakgrunn = innvilgelseBrevbestilling.nyVurderingBakgrunn
        }

        fun medInnledningFritekst(innledningFritekst: String?): Builder {
            this.innledningFritekst = innledningFritekst
            return this
        }

        fun medBegrunnelseFritekst(begrunnelseFritekst: String?): Builder {
            this.begrunnelseFritekst = begrunnelseFritekst
            return this
        }

        fun medInnvilgelseFritekst(innvilgelseFritekst: String?): Builder {
            this.innvilgelseFritekst = innvilgelseFritekst
            return this
        }

        fun medNyVurderingBakgrunn(nyVurderingBakgrunn: String?): Builder {
            this.nyVurderingBakgrunn = nyVurderingBakgrunn
            return this
        }

        override fun build(): InnvilgelseEftaStorbritanniaBrevbestilling {
            return InnvilgelseEftaStorbritanniaBrevbestilling(this)
        }
    }
}
