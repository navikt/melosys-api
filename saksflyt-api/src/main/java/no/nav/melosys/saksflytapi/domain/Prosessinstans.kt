package no.nav.melosys.saksflytapi.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.persistence.*
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.jpa.PropertiesConverter
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer
import org.apache.commons.lang3.exception.ExceptionUtils
import org.hibernate.annotations.GenericGenerator
import org.springframework.util.ObjectUtils.isEmpty
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

/**
 * Arbeidstabell for saksflyt.
 */
@Entity
@Table(name = "prosessinstans")
class Prosessinstans {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "uuid", nullable = false)
    var id: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "prosess_type", nullable = false)
    var type: ProsessType? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ProsessStatus? = null

    @ManyToOne
    @JoinColumn(name = "behandling_id")
    var behandling: Behandling? = null

    @Lob
    @Column(name = "data")
    @Convert(converter = PropertiesConverter::class)
    private val data: Properties = Properties()

    @Enumerated(EnumType.STRING)
    @Column(name = "sist_fullfort_steg")
    var sistFullførtSteg: ProsessSteg? = null

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    var registrertDato: LocalDateTime? = null

    @Column(name = "endret_dato", nullable = false)
    var endretDato: LocalDateTime? = null

    @OneToMany(mappedBy = "prosessinstans", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var hendelser: MutableList<ProsessinstansHendelse> = ArrayList()

    @Column(name = "sed_laas_referanse")
    var låsReferanse: String? = null

    fun getData(): Properties = data

    fun setData(data: Properties) {
        this.data.putAll(data)
    }

    fun hasData(key: ProsessDataKey): Boolean = !isEmpty(getData(key))

    /**
     * Returnerer et dataelement som String
     */
    fun getData(key: ProsessDataKey): String? = data.getProperty(key.kode)

    fun hentData(key: ProsessDataKey): String = data.getProperty(key.kode) ?: error("Data for key ${key.kode} is not set")

    fun <T> hentData(key: ProsessDataKey, type: Class<T>): T = getData(key, type)
        ?: error("Data for key ${key.kode} is not set or cannot be deserialized to ${type.simpleName}")

    /**
     * Returnerer et dataelement som et Object (etter JSON deserialisering)
     */
    fun <T> getData(key: ProsessDataKey, type: Class<T>): T? {
        val dataString = getData(key) ?: return null
        return try {
            dataMapper.readValue(dataString, type)
        } catch (e: IOException) {
            if (e is JsonParseException) {
                throw IllegalStateException("Feil ved deserialisering")
            } else {
                throw IllegalStateException("Feil ved deserialisering", e)
            }
        }
    }

    fun <T> getData(key: ProsessDataKey, type: Class<T>, defaultVerdi: T?): T? =
        getData(key, type) ?: defaultVerdi

    fun <T> finnData(key: ProsessDataKey, type: Class<T>, defaultVerdi: T): T =
        getData(key, type) ?: defaultVerdi

    fun <T> getData(key: ProsessDataKey, type: TypeReference<T>): T? {
        val dataString = getData(key) ?: return null
        return try {
            dataMapper.readValue(dataString, type)
        } catch (e: JsonProcessingException) {
            if (e is JsonParseException) { // TODO: Hvorfor har vi denne sjekken?
                throw IllegalStateException("Feil ved deserialisering")
            } else {
                throw IllegalStateException("Feil ved deserialisering", e)
            }
        }
    }

    fun <T> getData(key: ProsessDataKey, type: TypeReference<T>, defaultVerdi: T): T =
        getData(key, type) ?: defaultVerdi

    fun setData(key: ProsessDataKey, value: String?) {
        if (value != null) {
            data.setProperty(key.kode, value)
        }
    }

    /**
     * Setter et dataelement til et object (ved json serialisering)
     */
    fun setData(key: ProsessDataKey, value: Any?) {
        if (value != null) {
            try {
                val dataString = dataMapper.writeValueAsString(value)
                setData(key, dataString)
            } catch (e: JsonProcessingException) {
                throw IllegalStateException("Feil ved serialisering", e)
            }
        }
    }

    val hentBehandling: Behandling
        get() = behandling ?: error("behandling er ikke satt for prosessinstans med ID: $id")

    val hentLåsReferanse: String
        get() = låsReferanse ?: error("låsReferanse er ikke satt for prosessinstans med ID: $id")

    fun hentJournalpostID(): String? = getData(ProsessDataKey.JOURNALPOST_ID) ?: behandling?.initierendeJournalpostId

    fun hentSaksbehandlerHvisTilordnes(): String? {
        val skalTilordnes = getData(ProsessDataKey.SKAL_TILORDNES, Boolean::class.java) ?: false
        return if (skalTilordnes) getData(ProsessDataKey.SAKSBEHANDLER) else null
    }

    fun hentAktørIDFraDataEllerSED(): String? = getData(ProsessDataKey.AKTØR_ID)
        ?: getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java, MelosysEessiMelding())!!.aktoerId

    fun hentMelosysEessiMelding(): MelosysEessiMelding? =
        getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java)

    fun leggTilHendelse(steg: ProsessSteg, t: Throwable) {
        hendelser.add(
            ProsessinstansHendelse(
                this,
                LocalDateTime.now(),
                steg,
                t.javaClass.simpleName,
                ExceptionUtils.getStackTrace(t)
            )
        )
    }

    fun erFerdig(): Boolean = status == ProsessStatus.FERDIG

    fun erFeilet(): Boolean = status == ProsessStatus.FEILET

    fun erPåVent(): Boolean = status == ProsessStatus.PÅ_VENT

    fun erUnderBehandling(): Boolean = status == ProsessStatus.UNDER_BEHANDLING

    override fun hashCode(): Int = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Prosessinstans) return false
        return id != null && id == other.id
    }

    override fun toString(): String = "Prosessinstans(id=$id, type=$type, status=$status, behandling=$behandling, " +
        "sistFullførtSteg=$sistFullførtSteg, registrertDato=$registrertDato, " +
        "endretDato=$endretDato, hendelser=$hendelser, låsReferanse='$låsReferanse')"

    fun toBuilder(): Builder = Builder()
        .medId(id)
        .medType(type)
        .medStatus(status)
        .medBehandling(behandling)
        .medSistFullførtSteg(sistFullførtSteg)
        .medRegistrertDato(registrertDato)
        .medEndretDato(endretDato)
        .medLåsReferanse(låsReferanse)
        .medData(data)

    companion object {
        @JvmStatic
        private val dataMapper: ObjectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(SimpleModule().addDeserializer(LovvalgBestemmelse::class.java, LovvalgBestemmelseDeserializer()))
            .registerModule(KotlinModule.Builder().build())

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        var id: UUID? = null
        var type: ProsessType? = null
        var status: ProsessStatus? = null
        var behandling: Behandling? = null
        var sistFullførtSteg: ProsessSteg? = null
        var registrertDato: LocalDateTime? = null
        var endretDato: LocalDateTime? = null
        var låsReferanse: String? = null
        val data: Properties = Properties()

        fun medId(id: UUID?) = apply { this.id = id }

        fun medType(type: ProsessType?) = apply { this.type = type }

        fun medStatus(status: ProsessStatus?) = apply { this.status = status }

        fun medBehandling(behandling: Behandling?) = apply { this.behandling = behandling }

        fun medSistFullførtSteg(sistFullførtSteg: ProsessSteg?) = apply { this.sistFullførtSteg = sistFullførtSteg }

        fun medRegistrertDato(registrertDato: LocalDateTime?) = apply { this.registrertDato = registrertDato }

        fun medEndretDato(endretDato: LocalDateTime?) = apply { this.endretDato = endretDato }

        fun medLåsReferanse(låsReferanse: String?) = apply { this.låsReferanse = låsReferanse }

        fun medData(key: ProsessDataKey, value: String?) = apply {
            if (value != null) {
                data.setProperty(key.kode, value)
            }
        }

        fun medData(key: ProsessDataKey, value: Any?) = apply {
            if (value != null) {
                try {
                    val dataString = dataMapper.writeValueAsString(value)
                    data.setProperty(key.kode, dataString)
                } catch (e: JsonProcessingException) {
                    throw IllegalStateException("Feil ved serialisering", e)
                }
            }
        }

        fun medData(properties: Properties) = apply { data.putAll(properties) }

        fun build(): Prosessinstans = Prosessinstans().apply {
            id = this@Builder.id // TODO: bør ha en sjekk her også, testbuilder må ha id satt
            type = this@Builder.type ?: error("Type er påkrevd for Prosessinstans")
            status = this@Builder.status ?: error("Status er påkrevd for Prosessinstans")
            behandling = this@Builder.behandling
            sistFullførtSteg = this@Builder.sistFullførtSteg
            registrertDato = this@Builder.registrertDato ?: LocalDateTime.now()
            endretDato = this@Builder.endretDato ?: LocalDateTime.now()
            låsReferanse = this@Builder.låsReferanse
            setData(this@Builder.data)
        }
    }
}
