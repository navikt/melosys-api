package no.nav.melosys.domain

import jakarta.persistence.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.TekniskException
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "fagsak")
@EntityListeners(AuditingEntityListener::class)
class Fagsak(
    @Id
    @Column(name = "saksnummer", nullable = false)
    val saksnummer: String,

    @Column(name = "gsak_saksnummer")
    var gsakSaksnummer: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "fagsak_type", nullable = false)
    var type: Sakstyper,

    @Enumerated(EnumType.STRING)
    @Column(name = "tema", nullable = false)
    var tema: Sakstemaer,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Saksstatuser,

    @OneToMany(mappedBy = "fagsak", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    val aktører: MutableSet<Aktoer> = mutableSetOf(),

    @OneToMany(mappedBy = "fagsak", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    val behandlinger: MutableList<Behandling> = mutableListOf()
) : RegistreringsInfo() {
    constructor(
        saksnummer: String,
        gsakSaksnummer: Long? = null,
        type: Sakstyper,
        tema: Sakstemaer,
        status: Saksstatuser
    ) : this(
        saksnummer,
        gsakSaksnummer,
        type,
        tema,
        status,
        mutableSetOf(),
        mutableListOf()
    )

    @Deprecated("Er nødvendig med tom constructor med default verdier for JPA og kotlin")
    constructor(): this(
        "",
        null,
        Sakstyper.EU_EOS,
        Sakstemaer.MEDLEMSKAP_LOVVALG,
        Saksstatuser.OPPRETTET
    )

    companion object {
        private const val FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK = "Finner ikke behandlinger for fagsak "

    }

    fun leggTilAktør(aktør: Aktoer) = aktører.add(aktør)

    fun leggTilBehandling(behandling: Behandling) = behandlinger.add(behandling)

    fun harAktivBehandling(): Boolean = finnAktivBehandling() != null

    fun finnAktivBehandling(): Behandling? {
        val aktiveBehandlinger = behandlinger.filter { it.erAktiv() }
        if (aktiveBehandlinger.size > 1)
            throw TekniskException("Det finnes mer enn en aktiv behandling for sak $saksnummer")

        return aktiveBehandlinger.firstOrNull()
    }

    fun hentAktivBehandling(): Behandling = finnAktivBehandling()
        ?: throw FunksjonellException("Finner ingen aktiv behandling på fagsak $saksnummer")

    fun hentInaktiveBehandlinger(): List<Behandling> = behandlinger.filter { it.erInaktiv() }

    fun hentBehandlingerSortertSynkendePåRegistrertDato(): List<Behandling> = behandlinger.sortedByDescending { it.registrertDato }
    fun hentSistRegistrertBehandling(): Behandling = hentBehandlingerSortertSynkendePåRegistrertDato().firstOrNull()
        ?: throw IkkeFunnetException(FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK + saksnummer)

    fun hentSistOppdatertBehandling(): Behandling = behandlinger.maxByOrNull { it.endretDato }
        ?: throw IkkeFunnetException(FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK + saksnummer)

    fun hentSistAktivBehandling(): Behandling = finnAktivBehandling() ?: hentSistOppdatertBehandling()

    fun finnTidligstInaktivBehandling(): Behandling? = behandlinger.filter { it.erInaktiv() }.minByOrNull { it.getRegistrertDato() }

    fun hentTidligstInaktivBehandling(): Behandling = finnTidligstInaktivBehandling()
        ?: throw FunksjonellException("Finner ingen inaktiv behandling på fagsak $saksnummer")

    fun hentBruker(): Aktoer? = hentAktørMedRolleType(Aktoersroller.BRUKER)

    fun hentBrukersAktørID(): String = hentBruker()?.aktørId
        ?: throw FunksjonellException("Finner ikke bruker på fagsak $saksnummer")

    fun finnBrukersAktørID(): String? = hentBruker()?.aktørId

    fun hentMyndigheter(): List<Aktoer> = hentAktørerMedRolleType(Aktoersroller.TRYGDEMYNDIGHET)

    /**
     * Henter arbeidsgiver i tilfeller hvor det er forventet at det kun finnes en eller ingen
     */
    fun hentUnikArbeidsgiver(): Aktoer? = hentAktørMedRolleType(Aktoersroller.ARBEIDSGIVER)

    fun hentVirksomhet(): Aktoer? = hentAktørMedRolleType(Aktoersroller.VIRKSOMHET)

    fun finnVirksomhetsOrgnr(): String? = hentVirksomhet()?.orgnr

    private fun hentAktørMedRolleType(rolleType: Aktoersroller): Aktoer? {
        val kandidater = hentAktørerMedRolleType(rolleType)
        if (kandidater.size > 1) {
            throw TekniskException("Det finnes mer enn en aktør med rollen ${rolleType.beskrivelse} for sak $saksnummer")
        }
        return kandidater.firstOrNull()
    }

    private fun hentAktørerMedRolleType(rolleType: Aktoersroller?): List<Aktoer> =
        if (rolleType == null)
            emptyList()
        else aktører
            .filter { rolleType == it.rolle }

    fun harAktørMedRolleType(rolleType: Aktoersroller?): Boolean = hentAktørerMedRolleType(rolleType).isNotEmpty()

    /**
     * Henter myndighetens landkode fra institusjonsID som har format landkode:institusjonskode.
     */
    fun hentMyndighetLandkode(): Land_iso2 = hentMyndighet().hentMyndighetLandkode()

    fun hentMyndighet(): Aktoer {
        val myndigheter = hentMyndigheter()
        if (myndigheter.isEmpty()) {
            throw TekniskException("Finner ingen aktør med rolle ${Aktoersroller.TRYGDEMYNDIGHET} for fagsak $saksnummer")
        }
        if (myndigheter.size > 1) {
            throw TekniskException("Kan ikke hente landkode for en bestemt myndighet fordi finnes flere myndigheter")
        }
        return myndigheter.first()
    }

    fun finnFullmektig(fullmaktstype: Fullmaktstype): Aktoer? =
        aktører
            .filter { Aktoersroller.FULLMEKTIG == it.rolle }
            .firstOrNull { it.fullmaktstyper.contains(fullmaktstype) }

    fun kanEndreTypeOgTema(): Boolean = harAktivBehandling() && behandlinger.size == 1

    val hovedpartRolle: Aktoersroller
        get() =
            if (harAktørMedRolleType(Aktoersroller.BRUKER)) {
                Aktoersroller.BRUKER
            } else if (harAktørMedRolleType(Aktoersroller.VIRKSOMHET)) {
                Aktoersroller.VIRKSOMHET
            } else {
                throw FunksjonellException("Fagsak må ha hovedpart - enten BRUKER eller VIRKSOMHET")
            }

    fun harBrukerFullmektig(): Boolean = finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD) != null

    fun erSakstypeEøs(): Boolean = Sakstyper.EU_EOS == type

    fun erSakstypeTrygdeavtale(): Boolean = Sakstyper.TRYGDEAVTALE == type

    fun erSakstypeFtrl(): Boolean = Sakstyper.FTRL == type

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is Fagsak) {
            false
        } else saksnummer == other.saksnummer
    }

    override fun hashCode(): Int = 31
}
