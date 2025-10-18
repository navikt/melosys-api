package no.nav.melosys.saksflytapi.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.persistence.*
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.MelosysTestDsl
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
class Prosessinstans(
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "uuid", nullable = false)
    var id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "prosess_type", nullable = false)
    var type: ProsessType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ProsessStatus,

    @ManyToOne
    @JoinColumn(name = "behandling_id")
    var behandling: Behandling? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "sist_fullfort_steg")
    var sistFullførtSteg: ProsessSteg? = null,

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    var registrertDato: LocalDateTime,

    @Column(name = "endret_dato", nullable = false)
    var endretDato: LocalDateTime,

    @Column(name = "sed_laas_referanse")
    var låsReferanse: String? = null,

    @Lob
    @Column(name = "data")
    @Convert(converter = PropertiesConverter::class)
    private val data: Properties = Properties(),

    @OneToMany(mappedBy = "prosessinstans", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var hendelser: MutableList<ProsessinstansHendelse> = ArrayList()
) {
    fun getData(): Properties = Properties().apply { putAll(data) }

    fun hasData(key: ProsessDataKey): Boolean = !isEmpty(getData(key))

    /**
     * Returnerer et dataelement som String
     */
    fun getData(key: ProsessDataKey): String? = data.getProperty(key.kode)

    private inline fun <R> decodeOrThrow(key: ProsessDataKey, target: String, decode: () -> R): R =
        try {
            decode()
        } catch (e: JsonParseException) {
            throw IllegalStateException("Ugyldig JSON for $key ved deserialisering til $target", e)
        } catch (e: JsonMappingException) {
            throw IllegalStateException("Mapping-feil for $key ved deserialisering til $target", e)
        } catch (e: IOException) {
            throw IllegalStateException("I/O-feil ved lesing av data for $key", e)
        }

    private fun targetName(type: Class<*>) = type.simpleName
    private fun targetName(typeRef: TypeReference<*>) = dataMapper.typeFactory.constructType(typeRef).toString()

    // Kun for bruk fra Java-kode
    fun <T : Any> getData(key: ProsessDataKey, type: Class<T>): T? =
        getData(key)?.let { json ->
            decodeOrThrow(key, targetName(type)) { dataMapper.readValue(json, type) }
        }

    fun <T : Any> getData(key: ProsessDataKey, type: Class<T>, defaultVerdi: T?): T? =
        getData(key, type) ?: defaultVerdi

    fun <T> getData(key: ProsessDataKey, type: TypeReference<T>): T? =
        getData(key)?.let { json ->
            decodeOrThrow(key, targetName(type)) { dataMapper.readValue(json, type) }
        }

    fun <T> getData(key: ProsessDataKey, type: TypeReference<T>, defaultVerdi: T): T =
        getData(key, type) ?: defaultVerdi

    // Disse brukes fra Kotlin-kode
    inline fun <reified T : Any> hentData(key: ProsessDataKey): T =
        getData(key, object : TypeReference<T>() {})
            ?: error("Data for key ${key.kode} is not set or cannot be deserialized to ${T::class.java.simpleName}")

    fun hentData(key: ProsessDataKey): String = data.getProperty(key.kode) ?: error("Data for key ${key.kode} is not set")

    inline fun <reified T : Any> finnData(key: ProsessDataKey, default: T): T =
        getData(key, object : TypeReference<T>() {}) ?: default

    inline fun <reified T : Any> finnData(key: ProsessDataKey): T? =
        getData(key, object : TypeReference<T>() {})

    fun <T : Any> finnData(key: ProsessDataKey, type: Class<T>, defaultVerdi: T): T =
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

    fun hentAktørIDFraDataEllerSED(): String? = finnData(ProsessDataKey.AKTØR_ID)
        ?: finnData<MelosysEessiMelding>(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding()).aktoerId

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
        return id == other.id
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
        val dataMapper: ObjectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(SimpleModule().addDeserializer(LovvalgBestemmelse::class.java, LovvalgBestemmelseDeserializer()))
            .registerModule(KotlinModule.Builder().build())

        @JvmStatic
        fun builder() = Builder()
    }

    @MelosysTestDsl
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

        fun build() = Prosessinstans(
            id = id,
            type = type ?: error("Type er påkrevd for Prosessinstans"),
            status = status ?: error("Status er påkrevd for Prosessinstans"),
            behandling = behandling,
            sistFullførtSteg = sistFullførtSteg,
            registrertDato = registrertDato ?: LocalDateTime.now(), // disse settes på nytt ved ProsessinstansService.lagre
            endretDato = endretDato ?: LocalDateTime.now(), // disse settes på nytt ved ProsessinstansService.lagre
            låsReferanse = låsReferanse
        ).apply {
            this.data.putAll(this@Builder.data)
        }
    }
}
