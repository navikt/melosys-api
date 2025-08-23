package no.nav.melosys.domain.brev

class OrienteringAnmodningUnntakBrevbestilling : DokgenBrevbestilling {
    var anmodningUnntakFritekst: String? = null

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.anmodningUnntakFritekst = builder.anmodningUnntakFritekst
    }

    override fun toBuilder(): Builder = Builder(this)

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var anmodningUnntakFritekst: String? = null

        constructor()

        constructor(brevbestilling: OrienteringAnmodningUnntakBrevbestilling) : super(brevbestilling) {
            this.anmodningUnntakFritekst = brevbestilling.anmodningUnntakFritekst
        }

        fun medFritekst(fritekst: String?) = apply { this.anmodningUnntakFritekst = fritekst }

        override fun build(): OrienteringAnmodningUnntakBrevbestilling = OrienteringAnmodningUnntakBrevbestilling(this)
    }
}
