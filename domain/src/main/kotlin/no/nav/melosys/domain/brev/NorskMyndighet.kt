package no.nav.melosys.domain.brev

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class NorskMyndighet(
    val navn: String,
    val orgnr: String
) {
    HELFO("Helfo", "986965610"),
    SKATTEETATEN("Skatteetaten", "974761076"),
    SKATTEINNKREVER_UTLAND("Skatteinnkrever utland", "992187298")
}
