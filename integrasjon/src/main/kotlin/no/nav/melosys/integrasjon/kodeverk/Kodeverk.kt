package no.nav.melosys.integrasjon.kodeverk

/**
 * Ett kodeverk, internt representert som en map der kode gir en liste med en eller flere navn (en for hver gyldighetsperiode)
 */
data class Kodeverk(val navn: String, val koder: Map<String, List<Kode>>)
