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
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*
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
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    val fagsak: Fagsak,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Behandlingsstatus,

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_type", nullable = false)
    val type: Behandlingstyper,

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_tema", nullable = false)
    val tema: Behandlingstema,

    @Column(name = "behandlingsfrist", nullable = false)
    var behandlingsfrist: LocalDate,

    // Nullable fields
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

    @ManyToOne
    @JoinColumn(name = "opprinnelig_behandling_id")
    var opprinneligBehandling: Behandling? = null,

    // Relationships
    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val saksopplysninger: MutableSet<Saksopplysning> = mutableSetOf(),

    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    val behandlingsnotater: MutableSet<Behandlingsnotat> = mutableSetOf(),

    @OneToOne(mappedBy = "behandling", cascade = [CascadeType.ALL], orphanRemoval = true)
    var behandlingsårsak: Behandlingsaarsak? = null,

    @OneToOne(mappedBy = "behandling", cascade = [CascadeType.ALL], orphanRemoval = true)
    var mottatteOpplysninger: MottatteOpplysninger? = null
) : RegistreringsInfo() {

    // Note: Kotlin automatically generates getter/setter methods for properties
    // The following are automatically available:
    // getId(), getFagsak(), getStatus(), setStatus(), etc.

    /**
     * Sets the behandlingsårsak with proper bidirectional relationship management
     */
    fun settBehandlingsårsak(behandlingsårsak: Behandlingsaarsak?) {
        if (behandlingsårsak == null) {
            this.behandlingsårsak?.behandling = null
        } else {
            behandlingsårsak.behandling = this
        }
        this.behandlingsårsak = behandlingsårsak
    }



    /**
     * @deprecated Persondata skal ikke lagres under saksopplysning ifm. PDL.
     */
    @Deprecated("Persondata skal ikke lagres under saksopplysning ifm. PDL.")
    fun hentPersonDokument(): PersonDokument {
        val saksopplysning = finnDokument(SaksopplysningType.PERSOPL)
        return saksopplysning.map { it as PersonDokument }
            .orElseThrow { TekniskException("Finner ikke persondokument") }
    }

    fun hentMedlemskapDokument(): MedlemskapDokument {
        val saksopplysning = finnDokument(SaksopplysningType.MEDL)
        return saksopplysning.map { it as MedlemskapDokument }
            .orElseThrow { TekniskException("Finner ikke medlemskapdokument") }
    }

    fun finnArbeidsforholdDokument(): Optional<ArbeidsforholdDokument> =
        finnDokument(SaksopplysningType.ARBFORH).map { it as ArbeidsforholdDokument }

    fun hentOrganisasjonDokumenter(): List<OrganisasjonDokument> =
        saksopplysninger
            .filter { it.type == SaksopplysningType.ORG }
            .map { it.dokument as OrganisasjonDokument }

    fun hentSedDokument(): SedDokument {
        val saksopplysning = finnDokument(SaksopplysningType.SEDOPPL)
        return saksopplysning.map { it as SedDokument }
            .orElseThrow { TekniskException("Finner ikke seddokument") }
    }

    fun finnSedDokument(): Optional<SedDokument> =
        finnDokument(SaksopplysningType.SEDOPPL).map { it as SedDokument }



    fun hentInntektDokument(): InntektDokument {
        val saksopplysning = finnDokument(SaksopplysningType.INNTK)
        return saksopplysning.map { it as InntektDokument }
            .orElseThrow { TekniskException("Finner ikke inntektdokument") }
    }

    fun finnUtbetalingDokument(): Optional<UtbetalingDokument> =
        finnDokument(SaksopplysningType.UTBETAL).map { it as UtbetalingDokument }

    fun finnMedlemskapDokument(): Optional<MedlemskapDokument> =
        finnDokument(SaksopplysningType.MEDL).map { it as MedlemskapDokument }

    fun finnDokument(saksopplysningType: SaksopplysningType): Optional<SaksopplysningDokument> =
        saksopplysninger.stream()
            .filter { it.type == saksopplysningType }
            .findFirst()
            .map { it.dokument }

    fun finnMottatteOpplysningerData(): Optional<no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData> =
        Optional.ofNullable(mottatteOpplysninger?.mottatteOpplysningerData)

    fun harPeriodeOgSøknadsland(): Boolean = harPeriode() && harSøknadsland()

    fun harPeriode(): Boolean {
        val optionalPeriode = finnPeriode()
        return optionalPeriode != null && optionalPeriode.fom != null
    }

    fun harLand(): Boolean {
        mottatteOpplysninger?.mottatteOpplysningerData?.let { data ->
            return if (data is AnmodningEllerAttest) {
                data.lovvalgsland != null
            } else {
                data.soeknadsland.erGyldig()
            }
        }
        return false
    }

    fun harSøknadsland(): Boolean {
        mottatteOpplysninger?.mottatteOpplysningerData?.let { data ->
            return data.soeknadsland.erGyldig()
        }
        return false
    }

    fun hentPeriode(): ErPeriode =
        finnPeriode() ?: throw IkkeFunnetException("Finner ikke periode for behandling $id")

    fun finnPeriode(): ErPeriode? {
        if (erÅrsavregning()) {
            throw FunksjonellException("Kan ikke hente periode for årsavregning $id")
        }

        val sedDokument = finnSedDokument()
        if (sedDokument.isPresent) {
            return sedDokument.get().lovvalgsperiode
        }

        mottatteOpplysninger?.mottatteOpplysningerData?.let { data ->
            return data.periode
        }

        return null
    }

    fun hentSøknadsLand(): Collection<String> =
        if (erNorgeUtpekt()) {
            val utenlandskeArbeidsstederLandkoder = mottatteOpplysninger?.mottatteOpplysningerData?.hentUtenlandskeArbeidsstederLandkode() ?: emptyList()
            if (utenlandskeArbeidsstederLandkoder.isEmpty()) {
                setOf(Landkoder.NO.kode)
            } else {
                utenlandskeArbeidsstederLandkoder
            }
        } else {
            mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland?.landkoder ?: emptySet()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Behandling) return false

        if (id != 0L && other.id != 0L) {
            return id == other.id
        }

        return registrertDato == other.registrertDato && fagsak == other.fagsak
    }

    override fun hashCode(): Int = Objects.hash(registrertDato, fagsak)

    fun kanResultereIVedtak(): Boolean = erNorgeUtpekt() || !erBehandlingAvSed()

    fun erAktiv(): Boolean = !erInaktiv()

    fun erInaktiv(): Boolean = erAvsluttet() || erMidlertidigLovvalgsbeslutning()

    fun erAvsluttet(): Boolean = status == Behandlingsstatus.AVSLUTTET

    private fun erMidlertidigLovvalgsbeslutning(): Boolean =
        status == Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING

    fun erRedigerbar(): Boolean =
        erAktiv() && status != Behandlingsstatus.IVERKSETTER_VEDTAK &&
        !(status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT && tema != IKKE_YRKESAKTIV)

    fun erVenterForDokumentasjon(): Boolean =
        status == Behandlingsstatus.AVVENT_DOK_PART ||
        status == Behandlingsstatus.AVVENT_DOK_UTL ||
        status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT

    fun erFørstegangsvurdering(): Boolean = type == Behandlingstyper.FØRSTEGANG

    fun erNyVurdering(): Boolean = type == Behandlingstyper.NY_VURDERING

    fun erManglendeInnbetalingTrygdeavgift(): Boolean =
        type == Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT

    fun erAndregangsbehandling(): Boolean = erNyVurdering() || erManglendeInnbetalingTrygdeavgift()

    fun erNorgeUtpekt(): Boolean = tema == BESLUTNING_LOVVALG_NORGE

    fun erBeslutningLovvalgAnnetLand(): Boolean = tema == BESLUTNING_LOVVALG_ANNET_LAND

    fun erUtsending(): Boolean = tema == UTSENDT_ARBEIDSTAKER || tema == UTSENDT_SELVSTENDIG

    fun erRegisteringAvUnntak(): Boolean = erRegistreringAvUnntak(tema.kode)

    fun erAnmodningOmUnntak(): Boolean = erAnmodningOmUnntak(tema.kode)

    fun erPensjonist(): Boolean = tema == PENSJONIST

    fun erBehandlingAvSed(): Boolean =
        erRegistreringAvUnntak(tema) ||
        erAnmodningOmUnntakOgSakstypeEuEøs(tema, fagsak.type) ||
        BESLUTNING_LOVVALG_NORGE == tema

    fun erÅrsavregning(): Boolean = type == Behandlingstyper.ÅRSAVREGNING

    fun harStatus(status: Behandlingsstatus): Boolean = this.status == status

    fun manglerSaksopplysningerAvType(saksopplysningTyper: List<SaksopplysningType>): Boolean {
        val eksisterendeTyper = saksopplysninger.map { it.type }.toSet()
        return saksopplysningTyper.none { it in eksisterendeTyper }
    }

    override fun toString(): String =
        "Behandling(id=$id, fagsak=${fagsak.saksnummer}, type=$type, status=$status)"

    companion object {
        fun erBehandlingAvSøknadUtsendtArbeidstaker(behandlingstemaKode: String): Boolean =
            UTSENDT_ARBEIDSTAKER.kode.equals(behandlingstemaKode, ignoreCase = true) ||
            UTSENDT_SELVSTENDIG.kode.equals(behandlingstemaKode, ignoreCase = true)

        fun erBehandlingAvSøknadArbeidIFlereLand(behandlingstemaKode: String): Boolean =
            ARBEID_FLERE_LAND.kode.equals(behandlingstemaKode, ignoreCase = true)

        private fun erAnmodningOmUnntak(behandlingstemaKode: String): Boolean =
            ANMODNING_OM_UNNTAK_HOVEDREGEL.kode.equals(behandlingstemaKode, ignoreCase = true)

        private fun erAnmodningOmUnntakOgSakstypeEuEøs(behandlingstema: Behandlingstema, sakstype: Sakstyper): Boolean =
            ANMODNING_OM_UNNTAK_HOVEDREGEL == behandlingstema && Sakstyper.EU_EOS == sakstype

        fun erRegistreringAvUnntak(behandlingstema: Behandlingstema): Boolean =
            erRegistreringAvUnntak(behandlingstema.kode)

        private fun erRegistreringAvUnntak(behandlingstemaKode: String): Boolean =
            REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.kode.equals(behandlingstemaKode, ignoreCase = true) ||
            REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.kode.equals(behandlingstemaKode, ignoreCase = true) ||
            BESLUTNING_LOVVALG_ANNET_LAND.kode.equals(behandlingstemaKode, ignoreCase = true)

        fun utledBehandlingsfrist(behandling: Behandling, utgangspunktDato: LocalDate): LocalDate {
            val sakstema = behandling.fagsak.tema
            val behandlingstema = behandling.tema
            val behandlingstype = behandling.type

            return BehandlingfristKriterier.hentBehandlingsFrist(sakstema, behandlingstema, behandlingstype, utgangspunktDato)
        }

        /**
         * Builder pattern for Java compatibility
         */
        class Builder(
            private val fagsak: Fagsak,
            private val status: Behandlingsstatus,
            private val type: Behandlingstyper,
            private val tema: Behandlingstema,
            private val behandlingsfrist: LocalDate
        ) {
            private var sisteOpplysningerHentetDato: Instant? = null
            private var dokumentasjonSvarfristDato: Instant? = null
            private var initierendeJournalpostId: String? = null
            private var initierendeDokumentId: String? = null
            private var oppgaveId: String? = null
            private var opprinneligBehandling: Behandling? = null

            fun sisteOpplysningerHentetDato(dato: Instant?): Builder {
                sisteOpplysningerHentetDato = dato
                return this
            }

            fun dokumentasjonSvarfristDato(dato: Instant?): Builder {
                dokumentasjonSvarfristDato = dato
                return this
            }

            fun initierendeJournalpostId(journalpostId: String?): Builder {
                initierendeJournalpostId = journalpostId
                return this
            }

            fun initierendeDokumentId(dokumentId: String?): Builder {
                initierendeDokumentId = dokumentId
                return this
            }

            fun oppgaveId(oppgaveId: String?): Builder {
                this.oppgaveId = oppgaveId
                return this
            }

            fun opprinneligBehandling(behandling: Behandling?): Builder {
                opprinneligBehandling = behandling
                return this
            }

            fun build(): Behandling = Behandling(
                fagsak = fagsak,
                status = status,
                type = type,
                tema = tema,
                behandlingsfrist = behandlingsfrist,
                sisteOpplysningerHentetDato = sisteOpplysningerHentetDato,
                dokumentasjonSvarfristDato = dokumentasjonSvarfristDato,
                initierendeJournalpostId = initierendeJournalpostId,
                initierendeDokumentId = initierendeDokumentId,
                oppgaveId = oppgaveId,
                opprinneligBehandling = opprinneligBehandling
            )
        }
    }
}
