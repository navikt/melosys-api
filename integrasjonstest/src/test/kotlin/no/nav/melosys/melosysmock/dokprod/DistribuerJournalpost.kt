package no.nav.melosys.melosysmock.dokprod

import no.nav.melosys.melosysmock.utils.lagRandomId
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rest/v1/distribuerjournalpost")
@Unprotected
class DistribuerJournalpostApi {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun distribuerJournalpost(
        @RequestBody request: DistribuerJournalpostRequest
    ): DistribuerJournalpostResponse =
        DistribuerJournalpostResponse(
            lagRandomId()
        ).also {
            log.info("Distribuerer journalpost $request")
        }
}

data class DistribuerJournalpostRequest(
    val journalpostId: String? = null,
    val batchId: String? = null,
    val bestillendeFagsystem: String? = null,
    val dokumentProdApp: String? = null,
    var distribusjonstype: String? = null,
    val distribusjonstidspunkt: String? = null,
    val adresse: Adresse? = null
)

data class Adresse(
    val adresseType: String? = null,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val land: String? = null
)

data class DistribuerJournalpostResponse(
    val bestillingsId: String
)
