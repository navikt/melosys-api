package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import java.time.Instant

fun Behandlingsresultat.Companion.forTest(init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}): Behandlingsresultat =
    BehandlingsresultatTestFactory.Builder().apply(init).build()

fun BehandlingsresultatTestFactory.Builder.behandling(init: BehandlingTestFactory.BehandlingTestBuilder.() -> Unit) = apply {
    this.behandling = BehandlingTestFactory.builderWithDefaults().apply(init).build().knyttTilFagsakOgSaksopplysninger()
}

fun BehandlingsresultatTestFactory.Builder.vedtakMetadata(init: VedtakMetadata.() -> Unit) = apply {
    this.vedtakMetadata = VedtakMetadata().apply {
        registrertDato = Instant.now()
        endretDato = Instant.now()
        endretAv = "bla"
    }.apply(init)
}

fun BehandlingsresultatTestFactory.Builder.årsavregning(init: ÅrsavregningTestFactory.Builder.() -> Unit) = apply {
    this.årsavregning = Årsavregning.forTest(init)
}

fun BehandlingsresultatTestFactory.Builder.medlemskapsperiode(init: MedlemskapsperiodeTestFactory.Builder.() -> Unit) = apply {
    val nyMedlemskapsperiode = medlemskapsperiodeForTest(init)
    medlemskapsperioder.add(nyMedlemskapsperiode)
}

fun BehandlingsresultatTestFactory.Builder.lovvalgsperiode(init: LovvalgsperiodeTestFactory.Builder.() -> Unit) = apply {
    val nyLovvalgsperiode = lovvalgsperiodeForTest(init)
    lovvalgsperioder.add(nyLovvalgsperiode)
}

fun BehandlingsresultatTestFactory.Builder.begrunnelse(kode: String) = apply {
    val nyBegrunnelse = BehandlingsresultatBegrunnelse.lag(kode)
    behandlingsresultatBegrunnelser.add(nyBegrunnelse)
}

fun Medlemskapsperiode.trygdeavgiftsperiode(init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit) {
    val periode = Trygdeavgiftsperiode.forTest(init).apply {
        grunnlagMedlemskapsperiode = this@trygdeavgiftsperiode
    }
    trygdeavgiftsperioder.add(periode)
}

object BehandlingsresultatTestFactory {
    const val DEFAULT_ID = 1L
    const val DEFAULT_ENDRET_AV = "test"

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var behandling: Behandling? = null
        var behandlingsmåte: Behandlingsmaate = Behandlingsmaate.MANUELT
        var type: Behandlingsresultattyper = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        var fastsattAvLand: Land_iso2? = null
        var begrunnelseFritekst: String? = null
        var innledningFritekst: String? = null
        var trygdeavgiftFritekst: String? = null
        var nyVurderingBakgrunn: String? = null
        var fakturaserieReferanse: String? = null
        var utfallRegistreringUnntak: Utfallregistreringunntak? = null
        var utfallUtpeking: Utfallregistreringunntak? = null
        var trygdeavgiftType: Trygdeavgift_typer? = null
        var registrertDato: Instant = Instant.now()
        var endretDato: Instant = Instant.now()
        var endretAv: String = DEFAULT_ENDRET_AV

        var vedtakMetadata: VedtakMetadata? = null
        var årsavregning: Årsavregning? = null

        val medlemskapsperioder: MutableSet<Medlemskapsperiode> = mutableSetOf()
        val lovvalgsperioder: MutableSet<Lovvalgsperiode> = mutableSetOf()
        val behandlingsresultatBegrunnelser: MutableSet<BehandlingsresultatBegrunnelse> = mutableSetOf()

        fun build(): Behandlingsresultat {
            val behandlingsresultat = Behandlingsresultat().apply {
                this.id = this@Builder.id
                this.behandling = this@Builder.behandling
                this.behandlingsmåte = this@Builder.behandlingsmåte
                this.type = this@Builder.type
                this.fastsattAvLand = this@Builder.fastsattAvLand
                this.begrunnelseFritekst = this@Builder.begrunnelseFritekst
                this.innledningFritekst = this@Builder.innledningFritekst
                this.trygdeavgiftFritekst = this@Builder.trygdeavgiftFritekst
                this.nyVurderingBakgrunn = this@Builder.nyVurderingBakgrunn
                this.fakturaserieReferanse = this@Builder.fakturaserieReferanse
                this.utfallRegistreringUnntak = this@Builder.utfallRegistreringUnntak
                this.utfallUtpeking = this@Builder.utfallUtpeking
                this.trygdeavgiftType = this@Builder.trygdeavgiftType
                this.registrertDato = this@Builder.registrertDato
                this.endretDato = this@Builder.endretDato
                this.endretAv = this@Builder.endretAv
            }

            // Sett opp relasjoner for vedtakMetadata
            this@Builder.vedtakMetadata?.let {
                behandlingsresultat.vedtakMetadata = it
                it.behandlingsresultat = behandlingsresultat
            }

            // Sett opp relasjoner for årsavregning
            this@Builder.årsavregning?.let {
                behandlingsresultat.årsavregning = it
                it.behandlingsresultat = behandlingsresultat
            }

            // Sett opp relasjoner for medlemskapsperioder
            medlemskapsperioder.forEach { medlemskapsperiode ->
                behandlingsresultat.addMedlemskapsperiode(medlemskapsperiode)
            }

            // Sett opp relasjoner for lovvalgsperioder
            lovvalgsperioder.forEach { lovvalgsperiode ->
                behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
                lovvalgsperiode.behandlingsresultat = behandlingsresultat
            }

            // Sett opp relasjoner for behandlingsresultatBegrunnelser
            behandlingsresultatBegrunnelser.forEach { begrunnelse ->
                behandlingsresultat.behandlingsresultatBegrunnelser.add(begrunnelse)
                begrunnelse.behandlingsresultat = behandlingsresultat
            }

            return behandlingsresultat
        }
    }
}

