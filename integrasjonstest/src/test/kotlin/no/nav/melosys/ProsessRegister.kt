package no.nav.melosys

import org.springframework.stereotype.Component
import java.util.*

@Component
class ProsessRegister {
    private val idToName = mutableMapOf<UUID, String>()

    fun registrer(name: String, block: () -> UUID): UUID {
        idToName.values.firstOrNull { it == name }?.let { throw IllegalStateException("$name er alt registrert ") }
        return block().apply {
            idToName[this] = name
        }
    }

    fun count() = idToName.size

    fun nameFromId(uuid: UUID) = idToName[uuid]

    fun prosessIdStringToName(): Map<String, String> = idToName.map { it.key.toString() to it.value }.toMap()

    fun clear() = idToName.clear()
}
