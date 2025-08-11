package no.nav.melosys.saksflytapi.domain

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import java.time.LocalDateTime
import java.util.*

/**
 * Wrapper around the built-in Prosessinstans.Builder to provide DSL syntax and test defaults.
 */
class ProsessinstansTestBuilder {
    var id: UUID? = null
    var type: ProsessType? = null
    var status: ProsessStatus? = null
    var behandling: Behandling? = null
    var sistFullførtSteg: ProsessSteg? = null
    var registrertDato: LocalDateTime? = null
    var endretDato: LocalDateTime? = null
    var låsReferanse: String? = null
    val data = Properties()

    fun id(id: UUID) = apply { this.id = id }
    fun type(type: ProsessType) = apply { this.type = type }
    fun status(status: ProsessStatus) = apply { this.status = status }
    fun behandling(behandling: Behandling?) = apply { this.behandling = behandling }
    fun sistFullførtSteg(sistFullførtSteg: ProsessSteg?) = apply { this.sistFullførtSteg = sistFullførtSteg }
    fun låsReferanse(låsReferanse: String?) = apply { this.låsReferanse = låsReferanse }
    fun registrertDato(registrertDato: LocalDateTime?) = apply { this.registrertDato = registrertDato }
    fun endretDato(endretDato: LocalDateTime?) = apply { this.endretDato = endretDato }

    fun behandling(init: Behandling.Builder.() -> Unit = {}) = apply {
        this.behandling = BehandlingTestFactory.builderWithDefaults().apply(init).build()
    }

    fun data(key: ProsessDataKey, value: String?) = apply {
        if (value != null) {
            this.data.setProperty(key.kode, value)
        }
    }

    fun data(key: ProsessDataKey, value: Any?) = apply {
        if (value != null) {
            try {
                val dataString = Prosessinstans.getDataMapper().writeValueAsString(value)
                this.data.setProperty(key.kode, dataString)
            } catch (e: JsonProcessingException) {
                throw IllegalStateException("Feil ved serialisering", e)
            }
        }
    }


    fun build(): Prosessinstans = Prosessinstans().also {
        it.id = this.id ?: UUID.randomUUID() // Ensure id is never null
        it.type = this.type ?: ProsessType.OPPRETT_SAK // Default type
        it.status = this.status ?: ProsessStatus.KLAR // Default status
        it.behandling = this.behandling
        it.sistFullførtSteg = this.sistFullførtSteg
        it.registrertDato = this.registrertDato
        it.endretDato = this.endretDato ?: LocalDateTime.now() // Default to now if not set
        it.låsReferanse = this.låsReferanse
        it.data = this.data
    }
}

/**
 * Oppretter en Prosessinstans med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val prosessinstans = prosessinstansForTest {
 *     type = ProsessType.OPPRETT_SAK
 *     status = ProsessStatus.FERDIG
 * }
 * ```
 */
fun prosessinstansForTest(init: ProsessinstansTestBuilder.() -> Unit = {}): Prosessinstans =
    ProsessinstansTestBuilder().apply(init).build()

object ProsessinstansTestFactory {
    @JvmStatic
    fun builder() = ProsessinstansTestBuilder()

    @JvmStatic
    fun lagProsessinstans() = builder().build()

    @JvmStatic
    fun lagProsessinstansMedBehandling(behandling: Behandling): Prosessinstans =
        builder().behandling(behandling).build()

    @JvmStatic
    fun lagProsessinstansMedType(type: ProsessType): Prosessinstans =
        builder().type(type).build()

    @JvmStatic
    fun lagFerdigProsessinstans(): Prosessinstans =
        builder().status(ProsessStatus.FERDIG).build()

    @JvmStatic
    fun lagFeiletProsessinstans(): Prosessinstans =
        builder().status(ProsessStatus.FEILET).build()
}
