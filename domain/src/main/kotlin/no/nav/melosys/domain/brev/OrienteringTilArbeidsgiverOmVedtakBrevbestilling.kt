package no.nav.melosys.domain.brev

class OrienteringTilArbeidsgiverOmVedtakBrevbestilling : DokgenBrevbestilling {
    var erInnvilgelse: Boolean = false

    constructor() : super()

    constructor(builder: Builder) : super(builder) {
        this.erInnvilgelse = builder.erInnvilgelse
    }

    override fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder : DokgenBrevbestilling.Builder<Builder> {
        internal var erInnvilgelse: Boolean = false

        constructor()

        constructor(orienteringTilArbeidsgiverOmVedtakBrevbestilling: OrienteringTilArbeidsgiverOmVedtakBrevbestilling) : super(
            orienteringTilArbeidsgiverOmVedtakBrevbestilling
        ) {
            this.erInnvilgelse = orienteringTilArbeidsgiverOmVedtakBrevbestilling.erInnvilgelse
        }

        fun medErInnvilgelse(erInnvilgelse: Boolean): Builder {
            this.erInnvilgelse = erInnvilgelse
            return this
        }

        override fun build(): OrienteringTilArbeidsgiverOmVedtakBrevbestilling {
            return OrienteringTilArbeidsgiverOmVedtakBrevbestilling(this)
        }
    }
}
