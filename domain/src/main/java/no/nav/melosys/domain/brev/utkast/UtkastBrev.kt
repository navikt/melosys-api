package no.nav.melosys.domain.brev.utkast

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.*
import no.nav.melosys.exception.TekniskException
import java.time.LocalDateTime


@Entity
@Table(name = "utkast_brev")
class UtkastBrev(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "behandling_id")
    val behandlingID: Long,

    val lagringsdato: LocalDateTime = LocalDateTime.now(),

    @Column(name = "lagret_av_saksbehandler")
    val lagretAvSaksbehandler: String,
) {
    @Lob
    private lateinit var brevbestillingUtkast: String

    fun getBrevbestillingUtkast(): BrevbestillingUtkast = try {
        objectMapper.readValue<BrevbestillingUtkast>(brevbestillingUtkast)
    } catch (e: JsonProcessingException) {
        throw TekniskException("Klarte ikke lese brevbestillingUtkast med ID $id", e)
    }

    fun setBrevbestillingUtkast(brevBestillingUtkast: BrevbestillingUtkast?) {
        try {
            this.brevbestillingUtkast = objectMapper.writeValueAsString(brevBestillingUtkast)
        } catch (e: JsonProcessingException) {
            throw TekniskException("Klarte ikke skrive brevbestillingUtkast med ID $id", e)
        }
    }

    companion object {
        val objectMapper: ObjectMapper = jacksonObjectMapper()
    }

    class Builder {
        private var id: Long = 0
        private var behandlingID: Long = 0
        private var lagringsdato: LocalDateTime = LocalDateTime.now()
        private var lagretAvSaksbehandler: String = ""
        private var brevbestillingUtkast: BrevbestillingUtkast? = null

        fun id(id: Long) = apply { this.id = id }

        fun behandlingID(behandlingID: Long) = apply { this.behandlingID = behandlingID }

        fun lagringsdato(lagringsdato: LocalDateTime) = apply { this.lagringsdato = lagringsdato }

        fun lagretAvSaksbehandler(lagretAvSaksbehandler: String) = apply { this.lagretAvSaksbehandler = lagretAvSaksbehandler }

        fun brevbestillingUtkast(brevBestillingUtkast: BrevbestillingUtkast) = apply { this.brevbestillingUtkast = brevBestillingUtkast }

        fun build(): UtkastBrev {
            return UtkastBrev(
                id = id,
                behandlingID = behandlingID,
                lagringsdato = lagringsdato,
                lagretAvSaksbehandler = lagretAvSaksbehandler

            ).also {
                it.setBrevbestillingUtkast(brevbestillingUtkast)
            }
        }
    }
}
