package no.nav.melosys.domain.person

enum class Informasjonsbehov(private val beskrivelse: String) {
    INGEN("Uten tilleggsopplysninger"),
    STANDARD("Med adresseopplysninger"),
    MED_FAMILIERELASJONER("Med adresseopplysninger og familierelasjoner");
}
