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
    // TODO: kan bruke immutable og nonnullable typer, men krever litt refaktorering

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "behandling_id")
    var behandlingID: Long? = null,

    var lagringsdato: LocalDateTime = LocalDateTime.now(),

    @Column(name = "lagret_av_saksbehandler")
    var lagretAvSaksbehandler: String? = null,
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
}
