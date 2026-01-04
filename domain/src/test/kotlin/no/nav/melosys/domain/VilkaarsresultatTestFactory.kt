package no.nav.melosys.domain

import no.nav.melosys.domain.kodeverk.Vilkaar

fun vilkaarsresultatForTest(init: VilkaarsresultatTestFactory.Builder.() -> Unit = {}): Vilkaarsresultat =
    VilkaarsresultatTestFactory.Builder().apply(init).build()

fun VilkaarsresultatTestFactory.Builder.begrunnelse(kode: String) = apply {
    begrunnelseKoder.add(kode)
}

object VilkaarsresultatTestFactory {
    val DEFAULT_VILKAAR = Vilkaar.FO_883_2004_ART12_1

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var vilkaar: Vilkaar = DEFAULT_VILKAAR
        var isOppfylt: Boolean = false
        var begrunnelseFritekst: String? = null
        var begrunnelseFritekstEessi: String? = null
        val begrunnelseKoder: MutableList<String> = mutableListOf()

        fun build(): Vilkaarsresultat {
            val vilkaarsresultat = Vilkaarsresultat().apply {
                this.id = this@Builder.id
                this.vilkaar = this@Builder.vilkaar
                this.isOppfylt = this@Builder.isOppfylt
                this.begrunnelseFritekst = this@Builder.begrunnelseFritekst
                this.begrunnelseFritekstEessi = this@Builder.begrunnelseFritekstEessi
            }

            // Use toSet() on list to create a LinkedHashSet that preserves insertion order
            vilkaarsresultat.begrunnelser = begrunnelseKoder.map { kode ->
                VilkaarBegrunnelse().apply {
                    this.kode = kode
                    this.vilkaarsresultat = vilkaarsresultat
                }
            }.toSet()

            return vilkaarsresultat
        }
    }
}
