package no.nav.melosys.melosysmock.dokprod

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.melosysmock.utils.lagRandomId
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.ProduserDokumentutkast
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.ProduserIkkeredigerbartDokument
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.ws.server.endpoint.annotation.Endpoint
import org.springframework.ws.server.endpoint.annotation.PayloadRoot
import org.springframework.ws.server.endpoint.annotation.RequestPayload
import org.springframework.ws.server.endpoint.annotation.ResponsePayload
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition
import org.springframework.xml.xsd.SimpleXsdSchema
import org.springframework.xml.xsd.XsdSchema
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.ProduserDokumentutkastResponse as ProduserDokumentutkastResponseWrapper
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.ProduserIkkeredigerbartDokumentResponse as ProduserIkkeredigerbartDokumentResponseWrapper

@Endpoint
class DokumentproduksjonV3(
    @Autowired private val objectMapper: ObjectMapper
) {
    @PayloadRoot(
        namespace = "http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3",
        localPart = "produserDokumentutkast"
    )
    @ResponsePayload
    fun produserDokumentutkast(
        @RequestPayload produserDokumentutkast: ProduserDokumentutkast
    ): ProduserDokumentutkastResponseWrapper =
        ProduserDokumentutkastResponseWrapper().withResponse(
            ProduserDokumentutkastResponse().withDokumentutkast(
                genererPDF(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(produserDokumentutkast)
                )
            )
        )

    @PayloadRoot(
        namespace = "http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3",
        localPart = "produserIkkeredigerbartDokument"
    )
    @ResponsePayload
    fun produserIkkeredigerbartDokument(
        @RequestPayload produserIkkeredigerbartDokument: ProduserIkkeredigerbartDokument
    ): ProduserIkkeredigerbartDokumentResponseWrapper =
        ProduserIkkeredigerbartDokumentResponseWrapper().withResponse(
            ProduserIkkeredigerbartDokumentResponse()
                .withDokumentId(lagRandomId())
                .withJournalpostId(lagRandomId())
        )
}

@Configuration
class DokumentproduksjonV3Config {
    @Bean(name = ["dokumentproduksjon"])
    fun dokumentproduksjonWsdlDefinition(hentDokumentproduksjonSchema: XsdSchema) =
        DefaultWsdl11Definition()
            .apply {
                setPortTypeName("Dokumentproduksjon_v3")
                setLocationUri("/soap/services/dokumentproduksjon/v3")
                setTargetNamespace("http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3")
                setSchema(hentDokumentproduksjonSchema)
            }

    @Bean
    fun hentDokumentproduksjonSchema(): XsdSchema =
        SimpleXsdSchema(
            ClassPathResource(
                "wsdl/no/nav/tjeneste/virksomhet/dokumentproduksjon/v3/meldinger/meldinger.xsd"
            )
        )
}

fun genererPDF(tekst: String): ByteArray = """
    %PDF-1.1
    %¥±ë

    1 0 obj
      << /Type /Catalog
         /Pages 2 0 R
      >>
    endobj

    2 0 obj
      << /Type /Pages
         /Kids [3 0 R]
         /Count 1
         /MediaBox [0 0 595 842]
      >>
    endobj

    3 0 obj
      <<  /Type /Page
          /Parent 2 0 R
          /Resources
           << /Font
               << /F1
                   << /Type /Font
                      /Subtype /Type1
                      /BaseFont /Courier
                   >>
               >>
           >>
          /Contents 4 0 R
      >>
    endobj

    4 0 obj
      << /Length 55 >>
    stream
      BT
        /F1 10 Tf
        0 822 Td
        (${tekst}) Tj
      ET
    endstream
    endobj

    xref
    0 5
    0000000000 65535 f
    0000000018 00000 n
    0000000077 00000 n
    0000000178 00000 n
    0000000457 00000 n
    trailer
      <<  /Root 1 0 R
          /Size 5
      >>
    startxref
    565
    %%EOF
""".trimIndent().encodeToByteArray()
