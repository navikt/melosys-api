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
import org.apache.commons.lang3.StringUtils
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
    @Column(name = "uuid")
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

    companion object {
        @JvmStatic
        private val dataMapper: ObjectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(SimpleModule().addDeserializer(LovvalgBestemmelse::class.java, LovvalgBestemmelseDeserializer()))
            .registerModule(KotlinModule.Builder().build())

        @JvmStatic
        fun getDataMapper(): ObjectMapper = dataMapper

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    fun getData(): Properties = data

    fun setData(data: Properties) {
        this.data.putAll(data)
    }

    fun hasData(key: ProsessDataKey): Boolean = !isEmpty(getData(key))

    /**
     * Returnerer et dataelement som String
     */
    fun getData(key: ProsessDataKey): String? = data.getProperty(key.kode)

    fun getDataOrFail(key: ProsessDataKey): String = data.getProperty(key.kode) ?: error("Data for key ${key.kode} is not set")

    fun <T> getDataOrFail(key: ProsessDataKey, type: Class<T>): T = getData(key, type)
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

    fun <T> getDataNotNull(key: ProsessDataKey, type: Class<T>, defaultVerdi: T): T =
        getData(key, type) ?: defaultVerdi

    fun <T> getData(key: ProsessDataKey, type: TypeReference<T>): T? {
        val dataString = getData(key) ?: return null
        return try {
            dataMapper.readValue(dataString, type)
        } catch (e: JsonProcessingException) {
            if (e is JsonParseException) {
                throw IllegalStateException("Feil ved deserialisering")
            } else {
                throw IllegalStateException("Feil ved deserialisering", e)
            }
        }
    }

    fun <T> getData(key: ProsessDataKey, type: TypeReference<T>, defaultVerdi: T): T =
        getData(key, type) ?: defaultVerdi

    fun setDataHvisIkkeTom(key: ProsessDataKey, value: String?) {
        if (StringUtils.isNotEmpty(value)) {
            data.setProperty(key.kode, value)
        }
    }

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


    fun behandlingOrFail(): Behandling {
        return behandling ?: throw IllegalStateException("Behandling er ikke satt for prosessinstans med ID: $id")
    }

    fun hentJournalpostID(): String? {
        return getData(ProsessDataKey.JOURNALPOST_ID) ?: behandling?.initierendeJournalpostId
    }

    fun hentSaksbehandlerHvisTilordnes(): String? {
        val skalTilordnes = getData(ProsessDataKey.SKAL_TILORDNES, Boolean::class.java) ?: false
        return if (skalTilordnes) getData(ProsessDataKey.SAKSBEHANDLER) else null
    }

    fun hentAktørIDFraDataEllerSED(): String? {
        return getData(ProsessDataKey.AKTØR_ID)
            ?: getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java, MelosysEessiMelding())!!.aktoerId
    }

    fun hentMelosysEessiMelding(): MelosysEessiMelding? {
        return getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java)
    }

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

    override fun toString(): String {
        return "Prosessinstans(id=$id, type=$type, status=$status, behandling=$behandling, " +
            "sistFullførtSteg=$sistFullførtSteg, registrertDato=$registrertDato, " +
            "endretDato=$endretDato, hendelser=$hendelser, låsReferanse='$låsReferanse')"
    }

    fun toBuilder(): Builder {
        val builder = Builder()
            .medId(id)
            .medType(type)
            .medStatus(status)
            .medBehandling(behandling)
            .medSistFullførtSteg(sistFullførtSteg)
            .medRegistrertDato(registrertDato)
            .medEndretDato(endretDato)
            .medLåsReferanse(låsReferanse)

        // Copy all data properties
        builder.medData(data)
        return builder
    }

    class Builder {
        private var id: UUID? = null
        private var type: ProsessType? = null
        private var status: ProsessStatus? = null
        private var behandling: Behandling? = null
        private var sistFullførtSteg: ProsessSteg? = null
        private var registrertDato: LocalDateTime? = null
        private var endretDato: LocalDateTime? = null
        private var låsReferanse: String? = null
        private val data: Properties = Properties()

        fun medId(id: UUID?): Builder {
            this.id = id
            return this
        }

        fun medType(type: ProsessType?): Builder {
            this.type = type
            return this
        }

        fun medStatus(status: ProsessStatus?): Builder {
            this.status = status
            return this
        }

        fun medBehandling(behandling: Behandling?): Builder {
            this.behandling = behandling
            return this
        }

        fun medSistFullførtSteg(sistFullførtSteg: ProsessSteg?): Builder {
            this.sistFullførtSteg = sistFullførtSteg
            return this
        }

        fun medRegistrertDato(registrertDato: LocalDateTime?): Builder {
            this.registrertDato = registrertDato
            return this
        }

        fun medEndretDato(endretDato: LocalDateTime?): Builder {
            this.endretDato = endretDato
            return this
        }

        fun medLåsReferanse(låsReferanse: String?): Builder {
            this.låsReferanse = låsReferanse
            return this
        }

        fun medData(key: ProsessDataKey, value: String?): Builder {
            if (value != null) {
                data.setProperty(key.kode, value)
            }
            return this
        }

        fun medData(key: ProsessDataKey, value: Any?): Builder {
            if (value != null) {
                try {
                    val dataString = dataMapper.writeValueAsString(value)
                    data.setProperty(key.kode, dataString)
                } catch (e: JsonProcessingException) {
                    throw IllegalStateException("Feil ved serialisering", e)
                }
            }
            return this
        }

        fun medData(properties: Properties): Builder {
            data.putAll(properties)
            return this
        }

        fun build(): Prosessinstans {
            if (type == null) {
                throw IllegalStateException("Type er påkrevd for Prosessinstans")
            }
            if (status == null) {
                throw IllegalStateException("Status er påkrevd for Prosessinstans")
            }

            val prosessinstans = Prosessinstans()
            prosessinstans.id = this.id
            prosessinstans.type = this.type
            prosessinstans.status = this.status
            prosessinstans.behandling = this.behandling
            prosessinstans.sistFullførtSteg = this.sistFullførtSteg
            prosessinstans.registrertDato = this.registrertDato ?: LocalDateTime.now()
            prosessinstans.endretDato = this.endretDato ?: LocalDateTime.now()
            prosessinstans.låsReferanse = this.låsReferanse

            // Set data properties
            prosessinstans.setData(this.data)

            return prosessinstans
        }
    }
}
