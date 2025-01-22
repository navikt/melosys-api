package no.nav.melosys.domain.brev

enum class StandardvedleggType(
    val malnavn: String,
    val journalføringstittel: String,
    val frontendTittel: String
) {
    VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_AVSLAG(
        malnavn = "info_om_rettigheter_avslag",
        journalføringstittel = "Viktig informasjon om rettigheter og plikter",
        frontendTittel = "Avslag: Rettigheter og plikter"
    ),
    VIKTIG_INFORMASJON_RETTIGHETER_PLIKTER_INNVILGELSE(
        malnavn = "info_om_rettigheter_innvilgelse",
        journalføringstittel = "Viktig informasjon om rettigheter og plikter",
        frontendTittel = "Innvilgelse: Rettigheter og plikter"
    )
}
