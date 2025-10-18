package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "behandling")
@EntityListeners(AuditingEntityListener::class)
class Behandling(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    var fagsak: Fagsak,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Behandlingsstatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_type", nullable = false)
    var type: Behandlingstyper,

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_tema", nullable = false)
    var tema: Behandlingstema,

    @Column(name = "behandlingsfrist", nullable = false)
    var behandlingsfrist: LocalDate,

    @Column(name = "siste_opplysninger_hentet_dato")
    var sisteOpplysningerHentetDato: Instant? = null,

    @Column(name = "dokumentasjon_svarfrist_dato")
    var dokumentasjonSvarfristDato: Instant? = null,

    @Column(name = "initierende_journalpost_id")
    var initierendeJournalpostId: String? = null,

    @Column(name = "initierende_dokument_id")
    var initierendeDokumentId: String? = null,

    @Column(name = "oppgave_id")
    var oppgaveId: String? = null,

    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var saksopplysninger: MutableSet<Saksopplysning> = mutableSetOf(),

    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var behandlingsnotater: MutableSet<Behandlingsnotat> = mutableSetOf(),

    @OneToOne(mappedBy = "behandling", cascade = [CascadeType.ALL], orphanRemoval = true)
    var behandlingsårsak: Behandlingsaarsak? = null,

    @OneToOne(mappedBy = "behandling", cascade = [CascadeType.ALL], orphanRemoval = true)
    var mottatteOpplysninger: MottatteOpplysninger? = null,

    @ManyToOne
    @JoinColumn(name = "opprinnelig_behandling_id")
    var opprinneligBehandling: Behandling? = null

) : RegistreringsInfo() {

    fun settBehandlingsårsak(behandlingsårsak: Behandlingsaarsak?) {
        if (behandlingsårsak == null) {
            this.behandlingsårsak?.behandling = null
        } else {
            behandlingsårsak.behandling = this
        }
        this.behandlingsårsak = behandlingsårsak
    }

    fun hentMedlemskapDokument(): MedlemskapDokument {
        val saksopplysning = finnDokument(SaksopplysningType.MEDL)
        return saksopplysning.orElseThrow { TekniskException("Finner ikke medlemskapdokument") } as MedlemskapDokument
    }

    fun finnArbeidsforholdDokument(): Optional<ArbeidsforholdDokument> =
        finnDokument(SaksopplysningType.ARBFORH).map { it as ArbeidsforholdDokument }

    fun hentOrganisasjonDokumenter(): List<OrganisasjonDokument> =
        saksopplysninger
            .filter { it.type == SaksopplysningType.ORG }
            .map { it.dokument as OrganisasjonDokument }

    fun hentSedDokument(): SedDokument {
        val saksopplysning = finnDokument(SaksopplysningType.SEDOPPL)
        return saksopplysning.orElseThrow { TekniskException("Finner ikke seddokument") } as SedDokument
    }

    fun finnSedDokument(): Optional<SedDokument> =
        finnDokument(SaksopplysningType.SEDOPPL).map { it as SedDokument }

    fun hentInntektDokument(): InntektDokument {
        val saksopplysning = finnDokument(SaksopplysningType.INNTK)
        return saksopplysning.orElseThrow { TekniskException("Finner ikke inntektdokument") } as InntektDokument
    }

    fun finnUtbetalingDokument(): Optional<UtbetalingDokument> =
        finnDokument(SaksopplysningType.UTBETAL).map { it as UtbetalingDokument }

    fun finnMedlemskapDokument(): Optional<MedlemskapDokument> =
        finnDokument(SaksopplysningType.MEDL).map { it as MedlemskapDokument }

    fun finnDokument(saksopplysningType: SaksopplysningType): Optional<SaksopplysningDokument> =
        saksopplysninger.firstOrNull { it.type == saksopplysningType }?.dokument?.let { Optional.of(it) } ?: Optional.empty()

    fun finnMottatteOpplysningerData(): Optional<no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData> =
        Optional.ofNullable(mottatteOpplysninger?.mottatteOpplysningerData)

    fun harPeriodeOgSøknadsland(): Boolean = harPeriode() && harSøknadsland()

    fun harPeriode(): Boolean {
        val optionalPeriode = finnPeriode()
        return optionalPeriode.isPresent && optionalPeriode.get().fom != null
    }

    fun harLand(): Boolean {
        val mottatteOpplysningerData = mottatteOpplysninger?.mottatteOpplysningerData
        return when {
            mottatteOpplysningerData is AnmodningEllerAttest -> mottatteOpplysningerData.lovvalgsland != null
            mottatteOpplysningerData != null -> mottatteOpplysningerData.soeknadsland.erGyldig()
            else -> false
        }
    }

    fun harSøknadsland(): Boolean = mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland?.erGyldig() == true

    fun hentPeriode(): ErPeriode =
        finnPeriode().orElseThrow { IkkeFunnetException("Finner ikke periode for behandling $id") }

    fun finnPeriode(): Optional<ErPeriode> {
        if (erÅrsavregning()) {
            throw FunksjonellException("Kan ikke hente periode for årsavregning $id")
        }

        val optionalSeddokument = finnSedDokument()
        if (optionalSeddokument.isPresent) {
            return Optional.of(optionalSeddokument.get().hentLovvalgsperiode())
        }

        val mottatteOpplysningerData = mottatteOpplysninger?.mottatteOpplysningerData
        return mottatteOpplysningerData?.let { Optional.of(it.periode) } ?: Optional.empty()
    }

    fun hentSøknadsLand(): Collection<String> =
        if (erNorgeUtpekt()) {
            val utenlandskeArbeidsstederLandkoder =
                mottatteOpplysninger!!.mottatteOpplysningerData!!.hentUtenlandskeArbeidsstederLandkode()
            if (utenlandskeArbeidsstederLandkoder.isEmpty()) {
                setOf(Landkoder.NO.kode)
            } else {
                utenlandskeArbeidsstederLandkoder
            }
        } else {
            mottatteOpplysninger!!.mottatteOpplysningerData!!.soeknadsland.landkoder
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Behandling) return false

        val thisId = this.id
        val otherId = other.id

        if (thisId != 0L && otherId != 0L) {
            return thisId == otherId
        }

        return registrertDato == other.registrertDato && fagsak == other.fagsak
    }

    override fun hashCode(): Int = Objects.hash(registrertDato, fagsak)

    fun kanResultereIVedtak(): Boolean = erNorgeUtpekt() || !erBehandlingAvSed()

    fun erAktiv(): Boolean = !erInaktiv()

    fun erInaktiv(): Boolean = erAvsluttet() || erMidlertidigLovvalgsbeslutning()

    fun erAvsluttet(): Boolean = status == Behandlingsstatus.AVSLUTTET

    private fun erMidlertidigLovvalgsbeslutning(): Boolean = status == Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING

    fun erRedigerbar(): Boolean = erAktiv() && status != Behandlingsstatus.IVERKSETTER_VEDTAK &&
        !(status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT && tema != IKKE_YRKESAKTIV)

    fun erVenterForDokumentasjon(): Boolean = status in listOf(
        Behandlingsstatus.AVVENT_DOK_PART,
        Behandlingsstatus.AVVENT_DOK_UTL,
        Behandlingsstatus.ANMODNING_UNNTAK_SENDT
    )

    fun erFørstegangsvurdering(): Boolean = type == Behandlingstyper.FØRSTEGANG

    fun erNyVurdering(): Boolean = type == Behandlingstyper.NY_VURDERING

    fun erManglendeInnbetalingTrygdeavgift(): Boolean = type == Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT

    fun erAndregangsbehandling(): Boolean = erNyVurdering() || erManglendeInnbetalingTrygdeavgift()

    fun erNorgeUtpekt(): Boolean = tema == BESLUTNING_LOVVALG_NORGE

    fun erBeslutningLovvalgAnnetLand(): Boolean = tema == BESLUTNING_LOVVALG_ANNET_LAND

    fun erUtsending(): Boolean = tema == UTSENDT_ARBEIDSTAKER || tema == UTSENDT_SELVSTENDIG

    fun erRegisteringAvUnntak(): Boolean = erRegistreringAvUnntak(tema.kode)

    fun erAnmodningOmUnntak(): Boolean = ANMODNING_OM_UNNTAK_HOVEDREGEL.kode.equals(tema.kode, ignoreCase = true)

    fun erPensjonist(): Boolean = tema == PENSJONIST

    fun erEøsPensjonist(): Boolean = tema == PENSJONIST && fagsak.type == Sakstyper.EU_EOS && fagsak.tema == Sakstemaer.TRYGDEAVGIFT

    fun erBehandlingAvSed(): Boolean = erRegistreringAvUnntak(tema) ||
        erAnmodningOmUnntakOgSakstypeEuEøs(tema, fagsak.type) ||
        BESLUTNING_LOVVALG_NORGE == tema

    fun erÅrsavregning(): Boolean = type == Behandlingstyper.ÅRSAVREGNING

    fun erHenvendelse(): Boolean = type == Behandlingstyper.HENVENDELSE

    fun harStatus(status: Behandlingsstatus): Boolean = this.status == status

    fun manglerSaksopplysningerAvType(saksopplysningTyper: List<SaksopplysningType>): Boolean =
        Collections.disjoint(saksopplysningTyper, saksopplysninger.map { it.type })

    override fun toString(): String = "Behandling{id=$id, fagsak=${fagsak.saksnummer}, type=$type, status=$status}"

    private fun erAnmodningOmUnntakOgSakstypeEuEøs(behandlingstema: Behandlingstema, sakstype: Sakstyper): Boolean =
        ANMODNING_OM_UNNTAK_HOVEDREGEL == behandlingstema && Sakstyper.EU_EOS == sakstype

    fun erRegistreringAvUnntak(behandlingstema: Behandlingstema): Boolean =
        erRegistreringAvUnntak(behandlingstema.kode)

    private fun erRegistreringAvUnntak(behandlingstemaKode: String?): Boolean =
        REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.kode.equals(behandlingstemaKode, ignoreCase = true) ||
            REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.kode.equals(behandlingstemaKode, ignoreCase = true) ||
            BESLUTNING_LOVVALG_ANNET_LAND.kode.equals(behandlingstemaKode, ignoreCase = true)

    fun utledBehandlingsfrist(utgangspunktDato: LocalDate?): LocalDate {
        return BehandlingfristKriterier.hentBehandlingsFrist(
            sakstema = fagsak.tema,
            behandlingstema = tema,
            behandlingstype = type,
            utgangspunktDato = utgangspunktDato ?: error("Utgangspunkt dato kan ikke være null")
        )
    }

    companion object {
        // Tom - muliggjør utvidelsefunksjoner i tester
    }

    class Builder {
        var id: Long = 0
        var fagsak: Fagsak? = null
        var status: Behandlingsstatus? = null
        var type: Behandlingstyper? = null
        var tema: Behandlingstema? = null
        var behandlingsfrist: LocalDate? = null
        var dokumentasjonSvarfristDato: Instant? = null
        var initierendeJournalpostId: String? = null
        var initierendeDokumentId: String? = null
        var saksopplysninger: MutableSet<Saksopplysning> = mutableSetOf()
        var behandlingsårsak: Behandlingsaarsak? = null
        var mottatteOpplysninger: MottatteOpplysninger? = null
        var opprinneligBehandling: Behandling? = null
        var registrertDato: Instant? = null
        var endretDato: Instant? = null

        fun medId(id: Long?) = apply { this.id = id ?: 0 }

        fun medFagsak(fagsak: Fagsak?) = apply { this.fagsak = fagsak }

        fun medStatus(status: Behandlingsstatus?) = apply { this.status = status }

        fun medType(type: Behandlingstyper?) = apply { this.type = type }

        fun medTema(tema: Behandlingstema?) = apply { this.tema = tema }

        fun medBehandlingsfrist(behandlingsfrist: LocalDate?) = apply { this.behandlingsfrist = behandlingsfrist }

        fun medBehandlingsfrist(
            sakstema: Sakstemaer,
            behandlingstema: Behandlingstema,
            behandlingstype: Behandlingstyper,
            utgangspunktDato: LocalDate
        ) = apply {
            this.behandlingsfrist = BehandlingfristKriterier.hentBehandlingsFrist(sakstema, behandlingstema, behandlingstype, utgangspunktDato)
        }

        fun medInitierendeJournalpostId(initierendeJournalpostId: String?) = apply { this.initierendeJournalpostId = initierendeJournalpostId }

        fun medInitierendeDokumentId(initierendeDokumentId: String?) = apply { this.initierendeDokumentId = initierendeDokumentId }

        fun medDokumentasjonSvarfristDato(dokumentasjonSvarfristDato: Instant?) =
            apply { this.dokumentasjonSvarfristDato = dokumentasjonSvarfristDato }

        fun medSaksopplysninger(saksopplysninger: MutableSet<Saksopplysning>?) = apply { this.saksopplysninger = saksopplysninger ?: mutableSetOf() }

        fun medBehandlingsårsak(behandlingsårsak: Behandlingsaarsak?) = apply { this.behandlingsårsak = behandlingsårsak }

        fun medMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger?) = apply { this.mottatteOpplysninger = mottatteOpplysninger }

        fun medRegistrertDato(registrertDato: Instant) = apply { this.registrertDato = registrertDato }

        fun medEndretDato(endretDato: Instant) = apply { this.endretDato = endretDato }

        fun build(): Behandling = Behandling(
            id = id,
            fagsak = fagsak ?: error("Fagsak er påkrevd for Behandling"),
            status = status ?: error("Status er påkrevd for Behandling"),
            type = type ?: error("Type er påkrevd for Behandling"),
            tema = tema ?: error("Tema er påkrevd for Behandling"),
            behandlingsfrist = behandlingsfrist ?: error("Behandlingsfrist er påkrevd for Behandling"),
            dokumentasjonSvarfristDato = dokumentasjonSvarfristDato,
            initierendeJournalpostId = initierendeJournalpostId,
            initierendeDokumentId = initierendeDokumentId,
            saksopplysninger = saksopplysninger,
            mottatteOpplysninger = mottatteOpplysninger,
            opprinneligBehandling = opprinneligBehandling,
        ).also { behandling ->
            behandling.registrertDato = this@Builder.registrertDato ?: error("registrertDato er påkrevd for Behandling")
            behandling.endretDato = this@Builder.endretDato ?: error("endretDato er påkrevd for Behandling")

            this@Builder.behandlingsårsak?.let { behandling.settBehandlingsårsak(it) }
        }
    }
}
