package no.nav.melosys.domain

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import java.time.Instant
import java.time.LocalDate

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

fun BehandlingsresultatTestFactory.Builder.helseutgiftDekkesPeriode(init: HelseutgiftDekkesPeriodeTestFactory.Builder.() -> Unit) = apply {
    this.helseutgiftDekkesPeriode = HelseutgiftDekkesPeriode.forTest(init)
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

fun BehandlingsresultatTestFactory.Builder.vilkaarsresultat(init: VilkaarsresultatTestFactory.Builder.() -> Unit) = apply {
    val nyttVilkaarsresultat = vilkaarsresultatForTest(init)
    vilkaarsresultater.add(nyttVilkaarsresultat)
}

fun avklartefaktaForTest(init: AvklartefaktaTestFactory.Builder.() -> Unit = {}): Avklartefakta =
    AvklartefaktaTestFactory.Builder().apply(init).build()

fun BehandlingsresultatTestFactory.Builder.avklartefakta(init: AvklartefaktaTestFactory.Builder.() -> Unit) = apply {
    val nyttAvklartefakta = avklartefaktaForTest(init)
    this.avklartefakta.add(nyttAvklartefakta)
}

object AvklartefaktaTestFactory {
    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var type: Avklartefaktatyper? = null
        var referanse: String? = null
        var subjekt: String? = null
        var fakta: String? = null
        var begrunnelseFritekst: String? = null

        fun build(): Avklartefakta = Avklartefakta().apply {
            this.id = this@Builder.id
            this.type = this@Builder.type
            this.referanse = this@Builder.referanse
            this.subjekt = this@Builder.subjekt
            this.fakta = this@Builder.fakta
            this.begrunnelseFritekst = this@Builder.begrunnelseFritekst
        }
    }
}

fun BehandlingsresultatTestFactory.Builder.anmodningsperiode(init: AnmodningsperiodeTestFactory.Builder.() -> Unit) = apply {
    val nyAnmodningsperiode = anmodningsperiodeForTest(init)
    anmodningsperioder.add(nyAnmodningsperiode)
}

fun BehandlingsresultatTestFactory.Builder.utpekingsperiode(init: UtpekingsperiodeTestFactory.Builder.() -> Unit) = apply {
    val nyUtpekingsperiode = utpekingsperiodeForTest(init)
    utpekingsperioder.add(nyUtpekingsperiode)
}

fun anmodningsperiodeForTest(init: AnmodningsperiodeTestFactory.Builder.() -> Unit = {}): Anmodningsperiode =
    AnmodningsperiodeTestFactory.Builder().apply(init).build()

fun utpekingsperiodeForTest(init: UtpekingsperiodeTestFactory.Builder.() -> Unit = {}): Utpekingsperiode =
    UtpekingsperiodeTestFactory.Builder().apply(init).build()

object AnmodningsperiodeTestFactory {
    val DEFAULT_FOM: LocalDate = LocalDate.of(2023, 1, 1)
    val DEFAULT_TOM: LocalDate = LocalDate.of(2023, 12, 31)
    val DEFAULT_LOVVALGSLAND = Land_iso2.NO
    val DEFAULT_BESTEMMELSE = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var fom: LocalDate = DEFAULT_FOM
        var tom: LocalDate? = DEFAULT_TOM
        var lovvalgsland: Land_iso2? = DEFAULT_LOVVALGSLAND
        var bestemmelse: LovvalgBestemmelse? = DEFAULT_BESTEMMELSE
        var tilleggsbestemmelse: LovvalgBestemmelse? = null
        var unntakFraLovvalgsland: Land_iso2? = null
        var unntakFraBestemmelse: LovvalgBestemmelse? = null
        var dekning: Trygdedekninger? = null
        var sendtUtland: Boolean = false
        var anmodetAv: String? = null
        private var anmodningsperiodeSvarBuilder: AnmodningsperiodeSvarBuilder? = null

        fun anmodningsperiodeSvar(init: AnmodningsperiodeSvarBuilder.() -> Unit) {
            anmodningsperiodeSvarBuilder = AnmodningsperiodeSvarBuilder().apply(init)
        }

        fun build(): Anmodningsperiode {
            val anmodningsperiode = Anmodningsperiode(
                fom,
                tom,
                lovvalgsland,
                bestemmelse,
                tilleggsbestemmelse,
                unntakFraLovvalgsland,
                unntakFraBestemmelse,
                dekning
            ).apply {
                this@apply.id = this@Builder.id
                this@apply.setSendtUtland(this@Builder.sendtUtland)
                this@apply.anmodetAv = this@Builder.anmodetAv
            }

            anmodningsperiodeSvarBuilder?.let { svarBuilder ->
                val svar = AnmodningsperiodeSvar().apply {
                    this.anmodningsperiode = anmodningsperiode
                    this.anmodningsperiodeSvarType = svarBuilder.anmodningsperiodeSvarType
                    this.registrertDato = svarBuilder.registrertDato
                    this.begrunnelseFritekst = svarBuilder.begrunnelseFritekst
                    this.innvilgetFom = svarBuilder.innvilgetFom
                    this.innvilgetTom = svarBuilder.innvilgetTom
                }
                anmodningsperiode.anmodningsperiodeSvar = svar
            }

            return anmodningsperiode
        }
    }

    @MelosysTestDsl
    class AnmodningsperiodeSvarBuilder {
        var anmodningsperiodeSvarType: Anmodningsperiodesvartyper = Anmodningsperiodesvartyper.AVSLAG
        var registrertDato: LocalDate? = null
        var begrunnelseFritekst: String? = null
        var innvilgetFom: LocalDate? = null
        var innvilgetTom: LocalDate? = null
    }
}

object UtpekingsperiodeTestFactory {
    val DEFAULT_FOM: LocalDate = LocalDate.of(2023, 1, 1)
    val DEFAULT_TOM: LocalDate = LocalDate.of(2023, 12, 31)
    val DEFAULT_LOVVALGSLAND = Land_iso2.NO

    @MelosysTestDsl
    class Builder {
        var id: Long? = null
        var fom: LocalDate = DEFAULT_FOM
        var tom: LocalDate? = DEFAULT_TOM
        var lovvalgsland: Land_iso2? = DEFAULT_LOVVALGSLAND
        var bestemmelse: LovvalgBestemmelse? = null
        var tilleggsbestemmelse: LovvalgBestemmelse? = null
        var medlPeriodeID: Long? = null
        var sendtUtland: LocalDate? = null

        fun build(): Utpekingsperiode = Utpekingsperiode(
            fom,
            tom,
            lovvalgsland,
            bestemmelse,
            tilleggsbestemmelse
        ).apply {
            this@apply.id = this@Builder.id
            this@apply.medlPeriodeID = this@Builder.medlPeriodeID
            this@apply.sendtUtland = this@Builder.sendtUtland
        }
    }
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
        var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null

        val medlemskapsperioder: MutableSet<Medlemskapsperiode> = mutableSetOf()
        val lovvalgsperioder: MutableSet<Lovvalgsperiode> = mutableSetOf()
        val behandlingsresultatBegrunnelser: MutableSet<BehandlingsresultatBegrunnelse> = mutableSetOf()
        val vilkaarsresultater: MutableSet<Vilkaarsresultat> = mutableSetOf()
        val avklartefakta: MutableSet<Avklartefakta> = mutableSetOf()
        val anmodningsperioder: MutableSet<Anmodningsperiode> = mutableSetOf()
        val utpekingsperioder: MutableSet<Utpekingsperiode> = mutableSetOf()

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

            // Sett opp relasjoner for helseutgiftDekkesPeriode
            this@Builder.helseutgiftDekkesPeriode?.let {
                behandlingsresultat.helseutgiftDekkesPeriode = it
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

            // Sett opp relasjoner for vilkaarsresultater
            vilkaarsresultater.forEach { vilkaarsresultat ->
                behandlingsresultat.vilkaarsresultater.add(vilkaarsresultat)
                vilkaarsresultat.behandlingsresultat = behandlingsresultat
            }

            // Sett opp relasjoner for avklartefakta
            avklartefakta.forEach { fakta ->
                behandlingsresultat.avklartefakta.add(fakta)
                fakta.behandlingsresultat = behandlingsresultat
            }

            // Sett opp relasjoner for anmodningsperioder
            anmodningsperioder.forEach { anmodningsperiode ->
                behandlingsresultat.anmodningsperioder.add(anmodningsperiode)
                anmodningsperiode.behandlingsresultat = behandlingsresultat
            }

            // Sett opp relasjoner for utpekingsperioder
            utpekingsperioder.forEach { utpekingsperiode ->
                behandlingsresultat.utpekingsperioder.add(utpekingsperiode)
                utpekingsperiode.behandlingsresultat = behandlingsresultat
            }

            return behandlingsresultat
        }
    }
}
