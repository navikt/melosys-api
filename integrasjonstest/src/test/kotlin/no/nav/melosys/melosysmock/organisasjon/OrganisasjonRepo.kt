package no.nav.melosys.melosysmock.organisasjon

object OrganisasjonRepo {
    val repo: MutableMap<String, Organisasjon> = mutableMapOf()

    init {
        repo["999999999"] = Organisasjon(
            orgnr = "999999999",
            navn = "Ståles Stål AS",
            forretningsadresse = SemistrukturertAdresse(
                adresselinje1 = "Adresselinje1",
                poststed = "Oslo",
                postnummer = "0010",
                kommunenr = "0301",
                landkode = "NO"
            ),
            postadresse = SemistrukturertAdresse(
                adresselinje1 = "Adresselinje1",
                poststed = "Oslo",
                postnummer = "0010",
                kommunenr = "0301",
                landkode = "NO"
            )
        )
        repo["888888888"] = Organisasjon(
            orgnr = "888888888",
            navn = "Steinars Stein AS",
            forretningsadresse = SemistrukturertAdresse(
                adresselinje1 = "Adresselinje1",
                poststed = "Oslo",
                postnummer = "0010",
                kommunenr = "0301",
                landkode = "NO"
            ),
            postadresse = SemistrukturertAdresse(
                adresselinje1 = "Adresselinje1",
                poststed = "Adresselinje3",
                postnummer = "0010",
                kommunenr = "0301",
                landkode = "NO"
            )
        )
        repo["974761076"] = Organisasjon(
            orgnr = "974761076",
            navn = "Skatteetaten",
            forretningsadresse = SemistrukturertAdresse(
                adresselinje1 = "Fakturamottak DFØ",
                poststed = "Trondheim",
                postnummer = "7468",
                kommunenr = "5001",
                landkode = "NO"
            ),
            postadresse = SemistrukturertAdresse(
                adresselinje1 = "Fakturamottak DFØ",
                poststed = "Trondheim",
                postnummer = "7468",
                kommunenr = "5001",
                landkode = "NO"
            )
        )
    }
}

data class Organisasjon(
    val orgnr: String,
    val navn: String,
    val forretningsadresse: SemistrukturertAdresse,
    val postadresse: SemistrukturertAdresse
)

data class SemistrukturertAdresse(
    val adresselinje1: String,
    val poststed: String,
    val postnummer: String,
    val kommunenr: String,
    val landkode: String
)
