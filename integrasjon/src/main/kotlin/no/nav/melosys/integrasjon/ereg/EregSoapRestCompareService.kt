package no.nav.melosys.integrasjon.ereg

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.SaksopplysningDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger { }

@Service
@Primary
// @Primary Flyttet fra EregService - Vi burde se om vi kan fjerne den helt
// når vi rydder bort toggle melosys.ereg.organisasjon
class EregSoapRestCompareService(
    private val eregService: EregService,
    private val eregRestService: EregRestService
) : EregFasade {
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun hentOrganisasjon(orgnr: String): Saksopplysning {
        val organisasjonRest = runAndLogErrors(orgnr) {
            eregRestService.hentOrganisasjon(orgnr)
        }
        val organisasjonSoap = eregService.hentOrganisasjon(orgnr)
        compareAndLog(organisasjonSoap.dokument, organisasjonRest?.dokument)

        return organisasjonSoap
    }

    override fun finnOrganisasjon(orgnr: String): Optional<Saksopplysning> {
        val organisasjonRest = runAndLogErrors(orgnr) {
            eregRestService.finnOrganisasjon(orgnr).orElse(null)
        }

        val organisasjonSoap = eregService.finnOrganisasjon(orgnr)

        if (organisasjonSoap.isEmpty && organisasjonRest != null) {
            log.warn("Ereg: organisasjonSoap er tom men rest gir svar for $orgnr")
            log.warn("rest:\n${objectMapper.writeValueAsString(organisasjonRest)}")
        }

        compareAndLog(organisasjonSoap.get().dokument, organisasjonRest?.dokument)

        return organisasjonSoap
    }

    override fun hentOrganisasjonNavn(orgnummer: String): String {
        val organisasjonNavnRest = runAndLogErrors(orgnummer) {
            eregRestService.hentOrganisasjonNavn(orgnummer)
        }
        val organisasjonNavnSoap = eregService.hentOrganisasjonNavn(orgnummer)
        if (organisasjonNavnSoap != organisasjonNavnRest) {
            log.warn("Ereg hentOrganisasjonNavn: Organisasjonsnavn fra SOAP og REST er ikke like for orgnummer $orgnummer")
        }

        return organisasjonNavnSoap
    }

    private fun <T> runAndLogErrors(orgnr: String, action: () -> T?): T? {
        return try {
            action()
        } catch (e: Exception) {
            log.warn("Ereg: Kall mot rest endepunkt feilet for $orgnr", e)
            null
        }
    }

    private fun compareAndLog(saksopplysningDokumentSoap: SaksopplysningDokument, saksopplysningDokumentRest: SaksopplysningDokument?) {
        val organisasjonDokumentSoap = saksopplysningDokumentSoap as OrganisasjonDokument

        if (saksopplysningDokumentRest == null) {
            log.warn("Ereg: rest request er null for ${organisasjonDokumentSoap.orgnummer}")
            return
        }

        val organisasjonDokumentRest = saksopplysningDokumentRest as OrganisasjonDokument

        val soapNode = objectMapper.valueToTree<JsonNode>(organisasjonDokumentSoap.apply { oppstartsdato = null })
        val restNode = objectMapper.valueToTree<JsonNode>(organisasjonDokumentRest)

        if (soapNode != restNode) {
            val differences = compareJsonNodes(soapNode, restNode)
            log.warn("Differences found for ${organisasjonDokumentSoap.orgnummer}\n" +
                "soap vs rest:\n" + differences.joinToString("\n") { it })
            log.warn("soap: $soapNode")
            log.warn("rest: $restNode")
        }
    }


    // Laget av chat GPT-4
    private fun compareJsonNodes(node1: JsonNode, node2: JsonNode, path: String = ""): List<String> {
        val differences = mutableListOf<String>()

        if (nodesHaveDifferentTypes(node1, node2)) {
            differences.add("Type mismatch at '$path': ${node1::class.simpleName}(${node1.textValue()}) vs. ${node2::class.simpleName}(${node2.textValue()})")
            return differences
        }

        when {
            node1.isObject -> differences.addAll(compareObjectNodes(node1, node2, path))
            node1.isArray -> differences.addAll(compareArrayNodes(node1, node2, path))
            else -> {
                if (node1 != node2) {
                    differences.add("Value mismatch at '$path': ${node1.asText()} vs. ${node2.asText()}")
                }
            }
        }

        return differences
    }

    private fun nodesHaveDifferentTypes(node1: JsonNode, node2: JsonNode): Boolean {
        return node1::class != node2::class
    }

    private fun compareObjectNodes(node1: JsonNode, node2: JsonNode, path: String): List<String> {
        val differences = mutableListOf<String>()
        val node1Fields = node1.fieldNames().asSequence().toSet()
        val node2Fields = node2.fieldNames().asSequence().toSet()

        differences.addAll(checkForMissingFields(node1Fields, node2Fields, path, "second"))
        differences.addAll(checkForMissingFields(node2Fields, node1Fields, path, "first"))

        for (field in node1Fields.intersect(node2Fields)) {
            differences.addAll(compareJsonNodes(node1[field], node2[field], "$path/$field"))
        }

        return differences
    }

    private fun checkForMissingFields(fieldsToCheck: Set<String>, otherFields: Set<String>, path: String, descriptor: String): List<String> {
        return fieldsToCheck.filterNot { it in otherFields }.map { "Missing field in $descriptor JSON at path: '$path/$it'" }
    }

    private fun compareArrayNodes(node1: JsonNode, node2: JsonNode, path: String): List<String> {
        val differences = mutableListOf<String>()

        if (node1.size() != node2.size()) {
            differences.add("Array size mismatch at '$path': ${node1.size()} vs. ${node2.size()}")
            return differences
        }

        for (i in 0 until node1.size()) {
            differences.addAll(compareJsonNodes(node1[i], node2[i], "$path[$i]"))
        }

        return differences
    }
}
