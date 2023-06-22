package no.nav.melosys.sikkerhet.logging

data class AuditEvent(
    val type: AuditEventType,
    val sourceUserId: String,
    val destinationUserId: String,
    val message: String?,
    val sourceProcessName: String?,
)

enum class AuditEventType() {
    READ,
    UPDATE,
}
