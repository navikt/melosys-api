package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument
import no.nav.melosys.domain.kodeverk.Landkoder
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
open class Behandling(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
) : RegistreringsInfo() {

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    lateinit var fagsak: Fagsak

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    lateinit var status: Behandlingsstatus

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_type", nullable = false)
    lateinit var type: Behandlingstyper

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_tema", nullable = false)
    lateinit var tema: Behandlingstema

    @Column(name = "behandlingsfrist", nullable = false)
    lateinit var behandlingsfrist: LocalDate

    @Column(name = "siste_opplysninger_hentet_dato")
    var sisteOpplysningerHentetDato: Instant? = null

    @Column(name = "dokumentasjon_svarfrist_dato")
    var dokumentasjonSvarfristDato: Instant? = null

    @Column(name = "initierende_journalpost_id")
    lateinit var initierendeJournalpostId: String

    @Column(name = "initierende_dokument_id")
    var initierendeDokumentId: String? = null

    @Column(name = "oppgave_id")
    var oppgaveId: String? = null

    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var saksopplysninger: MutableSet<Saksopplysning> = mutableSetOf()

    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var behandlingsnotater: MutableSet<Behandlingsnotat> = mutableSetOf()

    @OneToOne(mappedBy = "behandling", cascade = [CascadeType.ALL], orphanRemoval = true)
    var behandlingsårsak: Behandlingsaarsak? = null

    @OneToOne(mappedBy = "behandling", cascade = [CascadeType.ALL], orphanRemoval = true)
    var mottatteOpplysninger: MottatteOpplysninger? = null

    @ManyToOne
    @JoinColumn(name = "opprinnelig_behandling_id")
    var opprinneligBehandling: Behandling ? = null


    fun settBehandlingsårsak(behandlingsårsak: Behandlingsaarsak?) {
        if (behandlingsårsak == null) {
            this.behandlingsårsak?.setBehandling(null)
        } else {
            behandlingsårsak!!.setBehandling(this)
        }
        this.behandlingsårsak = behandlingsårsak
    }

    @Deprecated("Persondata skal ikke lagres under saksopplysning ifm. PDL.")
    fun hentPersonDokument(): PersonDokument {
        val saksopplysning = finnDokument(SaksopplysningType.PERSOPL)
        return saksopplysning.orElseThrow { TekniskException("Finner ikke persondokument") } as PersonDokument
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

    fun harSøknadsland(): Boolean {
        val mottatteOpplysningerData = mottatteOpplysninger?.mottatteOpplysningerData
        return mottatteOpplysningerData?.soeknadsland?.erGyldig() == true
    }

    fun hentPeriode(): ErPeriode =
        finnPeriode().orElseThrow { IkkeFunnetException("Finner ikke periode for behandling $id") }

    fun finnPeriode(): Optional<ErPeriode> {
        if (erÅrsavregning()) {
            throw FunksjonellException("Kan ikke hente periode for årsavregning $id")
        }

        val optionalSeddokument = finnSedDokument()
        if (optionalSeddokument.isPresent) {
            return Optional.of(optionalSeddokument.get().lovvalgsperiode)
        }

        val mottatteOpplysningerData = mottatteOpplysninger?.mottatteOpplysningerData
        return mottatteOpplysningerData?.let { Optional.of(it.periode) } ?: Optional.empty()
    }

    fun hentSøknadsLand(): Collection<String> =
        if (erNorgeUtpekt()) {
            val utenlandskeArbeidsstederLandkoder = mottatteOpplysninger!!.mottatteOpplysningerData!!.hentUtenlandskeArbeidsstederLandkode()
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

        if (thisId != null && thisId != 0L && otherId != null && otherId != 0L) {
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

    fun erRegisteringAvUnntak(): Boolean = erRegistreringAvUnntak(tema?.kode)

    fun erAnmodningOmUnntak(): Boolean = erAnmodningOmUnntak(tema?.kode)

    fun erPensjonist(): Boolean = tema == PENSJONIST

    fun erBehandlingAvSed(): Boolean = tema != null && fagsak != null && (
        erRegistreringAvUnntak(tema!!) ||
            erAnmodningOmUnntakOgSakstypeEuEøs(tema!!, fagsak!!.type) ||
            BESLUTNING_LOVVALG_NORGE == tema)

    fun erÅrsavregning(): Boolean = type == Behandlingstyper.ÅRSAVREGNING

    fun harStatus(status: Behandlingsstatus): Boolean = this.status == status

    fun manglerSaksopplysningerAvType(saksopplysningTyper: List<SaksopplysningType>): Boolean =
        Collections.disjoint(saksopplysningTyper, saksopplysninger.map { it.type })

    override fun toString(): String = "Behandling{id=$id, fagsak=${fagsak?.saksnummer}, type=$type, status=$status}"

    companion object {
        @JvmStatic
        fun erBehandlingAvSøknadUtsendtArbeidstaker(behandlingstemaKode: String?): Boolean =
            UTSENDT_ARBEIDSTAKER.kode.equals(behandlingstemaKode, ignoreCase = true) ||
                UTSENDT_SELVSTENDIG.kode.equals(behandlingstemaKode, ignoreCase = true)

        @JvmStatic
        fun erBehandlingAvSøknadArbeidIFlereLand(behandlingstemaKode: String?): Boolean =
            ARBEID_FLERE_LAND.kode.equals(behandlingstemaKode, ignoreCase = true)

        private fun erAnmodningOmUnntak(behandlingstemaKode: String?): Boolean =
            ANMODNING_OM_UNNTAK_HOVEDREGEL.kode.equals(behandlingstemaKode, ignoreCase = true)

        private fun erAnmodningOmUnntakOgSakstypeEuEøs(behandlingstema: Behandlingstema, sakstype: Sakstyper): Boolean =
            ANMODNING_OM_UNNTAK_HOVEDREGEL == behandlingstema && Sakstyper.EU_EOS == sakstype

        @JvmStatic
        fun erRegistreringAvUnntak(behandlingstema: Behandlingstema): Boolean =
            erRegistreringAvUnntak(behandlingstema.kode)

        private fun erRegistreringAvUnntak(behandlingstemaKode: String?): Boolean =
            REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.kode.equals(behandlingstemaKode, ignoreCase = true) ||
                REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.kode.equals(behandlingstemaKode, ignoreCase = true) ||
                BESLUTNING_LOVVALG_ANNET_LAND.kode.equals(behandlingstemaKode, ignoreCase = true)

        @JvmStatic
        fun utledBehandlingsfrist(behandling: Behandling, utgangspunktDato: LocalDate?): LocalDate {
            val sakstema = behandling.fagsak.tema
            val behandlingstema = behandling.tema
            val behandlingstype = behandling.type
            return BehandlingfristKriterier.hentBehandlingsFrist(
                sakstema,
                behandlingstema,
                behandlingstype,
                utgangspunktDato ?: error("utgangspunktDatokan ikke være null")
            ).also { behandlingsfrist ->
                behandling.behandlingsfrist = behandlingsfrist
            }
        }
    }

    class Builder {
        private var id: Long = 0
        private var fagsak: Fagsak? = null
        private var status: Behandlingsstatus? = null
        private var type: Behandlingstyper? = null
        private var tema: Behandlingstema? = null
        private var sisteOpplysningerHentetDato: Instant? = null
        private var dokumentasjonSvarfristDato: Instant? = null
        private var initierendeJournalpostId: String? = null
        private var initierendeDokumentId: String? = null
        private var behandlingsfrist: LocalDate? = null
        private var oppgaveId: String? = null
        private var saksopplysninger: MutableSet<Saksopplysning> = mutableSetOf()
        private var behandlingsnotater: MutableSet<Behandlingsnotat> = mutableSetOf()
        private var behandlingsårsak: Behandlingsaarsak? = null
        private var mottatteOpplysninger: MottatteOpplysninger? = null
        private var opprinneligBehandling: Behandling? = null

        fun medId(id: Long?) = apply { this.id = id ?: 0 }
        fun medFagsak(fagsak: Fagsak?) = apply { this.fagsak = fagsak }
        fun medStatus(status: Behandlingsstatus?) = apply { this.status = status }
        fun medType(type: Behandlingstyper?) = apply { this.type = type }
        fun medTema(tema: Behandlingstema?) = apply { this.tema = tema }
        fun medSisteOpplysningerHentetDato(sisteOpplysningerHentetDato: Instant?) =
            apply { this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato }

        fun medDokumentasjonSvarfristDato(dokumentasjonSvarfristDato: Instant?) =
            apply { this.dokumentasjonSvarfristDato = dokumentasjonSvarfristDato }

        fun medInitierendeJournalpostId(initierendeJournalpostId: String?) = apply { this.initierendeJournalpostId = initierendeJournalpostId }
        fun medInitierendeDokumentId(initierendeDokumentId: String?) = apply { this.initierendeDokumentId = initierendeDokumentId }
        fun medBehandlingsfrist(behandlingsfrist: LocalDate?) = apply { this.behandlingsfrist = behandlingsfrist }
        fun medOppgaveId(oppgaveId: String?) = apply { this.oppgaveId = oppgaveId }
        fun medSaksopplysninger(saksopplysninger: MutableSet<Saksopplysning>?) = apply { this.saksopplysninger = saksopplysninger ?: mutableSetOf() }
        fun medBehandlingsnotater(behandlingsnotater: MutableSet<Behandlingsnotat>?) =
            apply { this.behandlingsnotater = behandlingsnotater ?: mutableSetOf() }

        fun medBehandlingsårsak(behandlingsårsak: Behandlingsaarsak?) = apply { this.behandlingsårsak = behandlingsårsak }
        fun medMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger?) = apply { this.mottatteOpplysninger = mottatteOpplysninger }
        fun medOpprinneligBehandling(opprinneligBehandling: Behandling?) = apply { this.opprinneligBehandling = opprinneligBehandling }

        fun build(): Behandling {
            requireNotNull(fagsak) { "Fagsak er påkrevd for Behandling" }
            requireNotNull(status) { "Status er påkrevd for Behandling" }
            requireNotNull(type) { "Type er påkrevd for Behandling" }
            requireNotNull(tema) { "Tema er påkrevd for Behandling" }
            requireNotNull(behandlingsfrist) { "Behandlingsfrist er påkrevd for Behandling" }

            return Behandling(id = id).apply {
                this.fagsak = this@Builder.fagsak!!
                this.status = this@Builder.status!!
                this.type = this@Builder.type!!
                this.tema = this@Builder.tema!!
                this.behandlingsfrist = this@Builder.behandlingsfrist!!
                this.sisteOpplysningerHentetDato = this@Builder.sisteOpplysningerHentetDato!!
                this.dokumentasjonSvarfristDato = this@Builder.dokumentasjonSvarfristDato!!
                this.initierendeJournalpostId = this@Builder.initierendeJournalpostId!!
                this.initierendeDokumentId = this@Builder.initierendeDokumentId!!
                this.oppgaveId = this@Builder.oppgaveId!!
                this.saksopplysninger = this@Builder.saksopplysninger
                this.behandlingsnotater = this@Builder.behandlingsnotater
                this.behandlingsårsak = this@Builder.behandlingsårsak!!
                this.mottatteOpplysninger = this@Builder.mottatteOpplysninger!!
                this.opprinneligBehandling = this@Builder.opprinneligBehandling!!
            }
        }
    }
}
