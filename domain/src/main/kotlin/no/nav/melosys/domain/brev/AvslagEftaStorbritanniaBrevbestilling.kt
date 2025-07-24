package no.nav.melosys.domain.brev

class AvslagEftaStorbritanniaBrevbestilling : DokgenBrevbestilling {
    var innledningFritekstAvslagEfta: String? = null
    var begrunnelseFritekstAvslagEfta: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.innledningFritekstAvslagEfta = builder.innledningFritekstavslagEfta
        this.begrunnelseFritekstAvslagEfta = builder.begrunnelseFritekstavslagEfta
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var innledningFritekstavslagEfta: String? = null
        internal var begrunnelseFritekstavslagEfta: String? = null

        constructor()

        constructor(avslagEftaStorbritanniaBrevbestilling: AvslagEftaStorbritanniaBrevbestilling) : super(
            avslagEftaStorbritanniaBrevbestilling
        ) {
            this.innledningFritekstavslagEfta = avslagEftaStorbritanniaBrevbestilling.innledningFritekstAvslagEfta
            this.begrunnelseFritekstavslagEfta = avslagEftaStorbritanniaBrevbestilling.begrunnelseFritekstAvslagEfta
        }

        fun medInnledningFritekstAvslagEfta(innledningFritekst: String?): Builder {
            this.innledningFritekstavslagEfta = innledningFritekst
            return this
        }

        fun medBegrunnelseFritekstAvslagEfta(begrunnelseFritekst: String?): Builder {
            this.begrunnelseFritekstavslagEfta = begrunnelseFritekst
            return this
        }

        override fun build(): AvslagEftaStorbritanniaBrevbestilling {
            return AvslagEftaStorbritanniaBrevbestilling(this)
        }
    }
}
