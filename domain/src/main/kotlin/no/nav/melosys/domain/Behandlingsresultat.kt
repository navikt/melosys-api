package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_konv_efta_storbritannia
import no.nav.melosys.exception.FunksjonellException
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "behandlingsresultat")
@EntityListeners(AuditingEntityListener::class)
open class Behandlingsresultat : RegistreringsInfo() {

    // Populeres av Hibernate med behandling.id
    @Id
    var id: Long? = null

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "behandling_id")
    var behandling: Behandling? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingsmaate", nullable = false)
    var behandlingsmåte: Behandlingsmaate? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_type", nullable = false)
    var type: Behandlingsresultattyper? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "fastsatt_av_land")
    var fastsattAvLand: Land_iso2? = null

    @Column(name = "begrunnelse_fritekst")
    var begrunnelseFritekst: String? = null

    @Column(name = "innledning_fritekst")
    var innledningFritekst: String? = null

    @Column(name = "trygdeavgift_fritekst")
    var trygdeavgiftFritekst: String? = null

    @Column(name = "ny_vurdering_bakgrunn")
    var nyVurderingBakgrunn: String? = null

    @Column(name = "fakturaserie_referanse")
    var fakturaserieReferanse: String? = null

    @OneToOne(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var vedtakMetadata: VedtakMetadata? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "utfall_registrering_unntak")
    var utfallRegistreringUnntak: Utfallregistreringunntak? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "utfall_utpeking")
    var utfallUtpeking: Utfallregistreringunntak? = null

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var avklartefakta: MutableSet<Avklartefakta> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var lovvalgsperioder: MutableSet<Lovvalgsperiode> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var anmodningsperioder: MutableSet<Anmodningsperiode> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var utpekingsperioder: MutableSet<Utpekingsperiode> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var vilkaarsresultater: MutableSet<Vilkaarsresultat> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var kontrollresultater: MutableSet<Kontrollresultat> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var medlemskapsperioder: MutableSet<Medlemskapsperiode> = mutableSetOf()

    @OneToMany(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var behandlingsresultatBegrunnelser: MutableSet<BehandlingsresultatBegrunnelse> = mutableSetOf()

    @OneToOne(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var årsavregning: Årsavregning? = null

    @OneToOne(mappedBy = "behandlingsresultat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode? = null

    @Column(name = "trygdeavgift_type")
    @Enumerated(EnumType.STRING)
    var trygdeavgiftType: Trygdeavgift_typer? = null

    fun addMedlemskapsperiode(medlemskapsperiode: Medlemskapsperiode) {
        medlemskapsperioder.add(medlemskapsperiode)
        medlemskapsperiode.behandlingsresultat = this
    }

    fun hentBehandling() = behandling ?: error("behandling er påkrevd for Behandlingsresultat")

    fun hentVedtakMetadata() = vedtakMetadata ?: error("vedtakMetadata er påkrevd for Behandlingsresultat")

    fun hentHelseutgiftDekkesPeriode() = helseutgiftDekkesPeriode ?: error("helseutgiftDekkesPeriode er påkrevd for Behandlingsresultat")

    fun hentÅrsavregning() = årsavregning ?: error("årsavregning er påkrevd for Behandlingsresultat")

    fun hentId() = id ?: error("id er påkrevd for Behandlingsresultat")

    fun hentType() = type ?: error("type er påkrevd for Behandlingsresultat")

    fun hentFakturaserieReferanse() = fakturaserieReferanse ?: error("fakturaserieReferanse er påkrevd for Behandlingsresultat")

    fun removeMedlemskapsperiode(medlemskapsperiode: Medlemskapsperiode) {
        medlemskapsperioder.remove(medlemskapsperiode)
        medlemskapsperiode.behandlingsresultat = null
    }

    fun clearMedlemskapsperioder() {
        medlemskapsperioder.forEach { it.behandlingsresultat = null }
        medlemskapsperioder.forEach(Medlemskapsperiode::clearTrygdeavgiftsperioder)
        medlemskapsperioder.clear()
    }

    fun clearTrygdevgiftPåHelseutgiftDekkesPeriode() {
        helseutgiftDekkesPeriode?.clearTrygdeavgiftsperioder()
    }

    fun erEøsPensjonist() =
        behandling?.erEøsPensjonist() ?: false


    fun utledSkatteplikttype(): Skatteplikttype {
        val trygdeavgiftsperiode = trygdeavgiftsperioder.firstOrNull()
        val erÅpenSluttdato = utledMedlemskapsperiodeTom() == null
        if (trygdeavgiftsperiode == null && erÅpenSluttdato) {
            return Skatteplikttype.SKATTEPLIKTIG
        } else if (trygdeavgiftsperiode == null) {
            throw RuntimeException("Trygdeavgiftsperiode ikke funnet, og det er ikke åpen sluttdato, id = $id")
        }

        return trygdeavgiftsperiode.grunnlagSkatteforholdTilNorge?.skatteplikttype
            ?: error("grunnlagSkatteforholdTilNorge er påkrevd for Trygdeavgiftsperiode")
    }

    fun utledMedlemskapsperiodeFom(): LocalDate? =
        medlemskapsperioder
            .filter { it.erInnvilget() }
            .mapNotNull { it.fom }
            .minOrNull()

    fun utledMedlemskapsperiodeTom(): LocalDate? =
        medlemskapsperioder
            .filter { it.erInnvilget() }
            .mapNotNull { it.tom }
            .maxOrNull()

    fun utledOpphørtDato(): LocalDate? =
        medlemskapsperioder
            .filter { it.erOpphørt() }
            .mapNotNull { it.fom }
            .minOrNull()

    fun harInnvilgetAvgiftspliktigPeriodeSomOverlapperMedÅr(år: Int): Boolean =
        avgiftspliktigPerioder().any { it.overlapperMedÅr(år) && it.erInnvilget() }

    val trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
        get() {
            if (behandling?.erEøsPensjonist() == true) {
                return eøsPensjonistTrygdeavgiftsperioder
            }

            return medlemskapsperioder.flatMap { it.trygdeavgiftsperioder }.toSet()
        }

    val eøsPensjonistTrygdeavgiftsperioder: Set<Trygdeavgiftsperiode>
        get() {
            if (helseutgiftDekkesPeriode == null) {
                return emptySet()
            }
            return hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder
        }

    fun clearTrygdeavgiftsperioder() {
        medlemskapsperioder.forEach(Medlemskapsperiode::clearTrygdeavgiftsperioder)
    }

    fun clearTrygdeavgiftsperioderHelseutgiftPeriode() {
        helseutgiftDekkesPeriode?.clearTrygdeavgiftsperioder()
            ?: error("helseutgiftDekkesPeriode må være satt for å kunne cleare trygdeavgiftsperioder")
    }

    fun hentSkatteforholdTilNorge(): Set<SkatteforholdTilNorge> =
        trygdeavgiftsperioder
            .mapNotNull { it.grunnlagSkatteforholdTilNorge }
            .toSet()

    fun hentInntektsperioder(): Set<Inntektsperiode> =
        trygdeavgiftsperioder
            .mapNotNull { it.grunnlagInntekstperiode }
            .toSet()

    fun harTrygdeavgiftsperioderSomOverlapperMedÅr(år: Int): Boolean {
        return trygdeavgiftsperioder
            .any { periode -> periode.overlapperMedÅr(år) && (periode.forskuddsvisFaktura || this.årsavregning != null) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Behandlingsresultat) return false
        return type == other.type && behandling == other.behandling
    }

    override fun hashCode(): Int = Objects.hash(type, behandling)

    fun erAvslag(): Boolean =
        type == Behandlingsresultattyper.AVSLAG_SØKNAD ||
            (type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND && hentLovvalgsperiode().erAvslått())

    fun erAnmodningOmUnntak(): Boolean = type == Behandlingsresultattyper.ANMODNING_OM_UNNTAK

    fun erOpphørt(): Boolean = type == Behandlingsresultattyper.OPPHØRT

    fun erInnvilgelse(): Boolean {
        if (type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND ||
            type == Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
        ) {
            return finnLovvalgsperiode().map { it.erInnvilget() }.orElse(false)
        }
        return false
    }

    fun erInnvilgelseFlereLand(): Boolean =
        erInnvilgelse() && finnLovvalgsperiode().map { it.erArtikkel13() }.orElse(false)


    fun erUtpeking(): Boolean =
        type == Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND && utpekingsperioder.isNotEmpty()

    fun erIkkeArtikkel16MedSendtAnmodningOmUnntak(): Boolean =
        !erArtikkel16MedSendtAnmodningOmUnntak()

    fun erArtikkel16MedSendtAnmodningOmUnntak(): Boolean =
        anmodningsperioder.any { it.erSendtUtland() }

    fun erArt16EtterUtlandMedRegistrertSvar(): Boolean =
        finnAnmodningsperiode()
            .map { it.harRegistrertSvar() }
            .orElse(false)

    fun harLovvalgsperiodeMedBestemmelse(lovvalgBestemmelse: LovvalgBestemmelse): Boolean =
        finnLovvalgsperiode()
            .map { it.bestemmelse == lovvalgBestemmelse }
            .orElse(false)

    fun erGodkjenningEllerInnvilgelseArt13(): Boolean =
        (erInnvilgelse() || erGodkjenningRegistreringUnntak()) &&
            finnLovvalgsperiode().map { it.erArtikkel13() }.orElse(false)

    fun harPeriodeOmLovvalg(): Boolean =
        lovvalgsperioder.isNotEmpty() || anmodningsperioder.isNotEmpty() || utpekingsperioder.isNotEmpty()

    fun hentValidertPeriodeOmLovvalg(): PeriodeOmLovvalg = when {
        lovvalgsperioder.isNotEmpty() -> hentLovvalgsperiode()
        anmodningsperioder.isNotEmpty() -> hentAnmodningsperiode()
        utpekingsperioder.isNotEmpty() -> hentValidertUtpekingsperiode()
        else -> throw NoSuchElementException("Ingen periode om lovvalg finnes for behandling $id")
    }

    fun finnValidertPeriodeOmLovvalg(): Optional<PeriodeOmLovvalg> {
        val lovvalgsperiodeOptional = finnLovvalgsperiode()
        val periodeOmLovvalgOptional: Optional<out PeriodeOmLovvalg> = if (lovvalgsperiodeOptional.isPresent) {
            lovvalgsperiodeOptional
        } else {
            finnAnmodningsperiode()
        }
        return periodeOmLovvalgOptional.map { it as PeriodeOmLovvalg }
    }

    fun hentLovvalgsperiode(): Lovvalgsperiode =
        finnLovvalgsperiode()
            .orElseThrow { NoSuchElementException("Ingen lovvalgsperiode finnes for behandlingsresultat $id") }

    fun finnLovvalgsperiode(): Optional<Lovvalgsperiode> {
        if (lovvalgsperioder.size > 1) {
            throw UnsupportedOperationException("Flere enn en lovvalgsperiode er ikke støttet")
        }
        return Optional.ofNullable(lovvalgsperioder.firstOrNull())
    }

    fun hentAnmodningsperiode(): Anmodningsperiode =
        finnAnmodningsperiode()
            .orElseThrow { NoSuchElementException("Ingen anmodningsperioder finnes for behandlingsresultat $id") }

    fun finnAnmodningsperiode(): Optional<Anmodningsperiode> {
        if (anmodningsperioder.size > 1) {
            throw FunksjonellException("Flere enn en anmodningsperiode er ikke støttet")
        }
        return Optional.ofNullable(anmodningsperioder.firstOrNull())
    }

    fun hentValidertUtpekingsperiode(): Utpekingsperiode =
        finnValidertUtpekingsperiode()
            .orElseThrow { NoSuchElementException("Ingen utpekingsperioder finnes for behandlingsresultat $id") }

    fun finnValidertUtpekingsperiode(): Optional<Utpekingsperiode> {
        if (utpekingsperioder.size > 1) {
            throw UnsupportedOperationException("Flere enn en utpekingsperiode er ikke støttet")
        }
        return Optional.ofNullable(utpekingsperioder.firstOrNull())
    }

    fun hentVilkaarbegrunnelser(vararg vilkaarTypeArray: Vilkaar): Set<VilkaarBegrunnelse> =
        vilkaarsresultater
            .filter { vr -> vilkaarTypeArray.any { it == vr.vilkaar } }
            .flatMap { it.begrunnelser }
            .toSet()

    fun avgiftspliktigPerioder(): List<AvgiftspliktigPeriode> {
        return (if (behandling?.erEøsPensjonist() == true && helseutgiftDekkesPeriode != null) {
            listOf(helseutgiftDekkesPeriode!!)
        } else {
            medlemskapsperioder.toList()
        })
    }

    fun manglerVilkår(vararg vilkaarArray: Vilkaar): Boolean =
        vilkaarsresultater.none { vilkår -> vilkaarArray.any { it == vilkår.vilkaar } }

    fun oppfyllerVilkår(vilkår: Collection<Vilkaar>): Boolean =
        vilkår.all(::oppfyllerVilkår)

    fun oppfyllerVilkår(vilkår: Vilkaar): Boolean =
        vilkaarsresultater.any { it.vilkaar == vilkår && it.isOppfylt }

    fun erAutomatisert(): Boolean =
        behandlingsmåte == Behandlingsmaate.AUTOMATISERT ||
            behandlingsmåte == Behandlingsmaate.DELVIS_AUTOMATISERT

    fun erInnvilgetArbeidPåSkipOmfattetAvArbeidsland(): Boolean =
        finnLovvalgsperiode()
            .map { l ->
                l.erInnvilget() &&
                    (l.bestemmelse == Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A || l.bestemmelse == Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3A) &&
                    (l.tilleggsbestemmelse == Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1 || l.tilleggsbestemmelse == Tilleggsbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_4_1)
            }
            .orElse(false)

    fun erRegistrertUnntak(): Boolean = type == Behandlingsresultattyper.REGISTRERT_UNNTAK

    fun erGodkjenningRegistreringUnntak(): Boolean =
        erRegistrertUnntak() && utfallRegistreringUnntak == Utfallregistreringunntak.GODKJENT

    fun a1Produseres(): Boolean = erInnvilgelse() && !erUtpeking() && harVedtak()

    fun utlandSkalVarslesOmVedtak(): Boolean =
        harVedtak() &&
            ((erInnvilgelse() && !harLovvalgsperiodeMedBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1) && !harLovvalgsperiodeMedBestemmelse(
                Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            )) ||
                erInnvilgelseFlereLand() ||
                erUtpeking())

    fun settVedtakMetadata(klagefrist: LocalDate) {
        settVedtakMetadata(null, klagefrist)
    }

    fun settVedtakMetadata(vedtakstype: Vedtakstyper?, klagefrist: LocalDate) {
        if (vedtakMetadata == null) {
            vedtakMetadata = VedtakMetadata().also {
                it.behandlingsresultat = this
            }
        } else {
            throw UnsupportedOperationException("Trenger vi å oppdatere et vedtak?")
        }

        val metadata = hentVedtakMetadata()
        metadata.vedtakstype = vedtakstype
        metadata.vedtaksdato = Instant.now()
        metadata.vedtakKlagefrist = klagefrist
    }

    fun harVedtak(): Boolean = vedtakMetadata != null

    override fun toString(): String = "Behandlingsresultat{id=$id, type=$type}"

    companion object // Tom - muliggjør utvidelsefunksjoner i tester
}
