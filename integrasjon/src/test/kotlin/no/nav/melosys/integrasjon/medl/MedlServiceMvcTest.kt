package no.nav.melosys.integrasjon.medl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.assertions.extracting
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningKildesystem
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.felles.MedlGenericContextExchangeFilter
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@Import(
    StsWebClientProducer::class,
    RestTokenServiceClient::class,
    OAuthMockServer::class,

    MedlGenericContextExchangeFilter::class,
    MedlemskapRestConsumerProducer::class,
)
@WebMvcTest
@AutoConfigureWebClient
@ActiveProfiles("wiremock-test")
class MedlServiceMvcTest(
    @Autowired private val medlemskapRestConsumer: MedlemskapRestConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, Saksopplysning>(mockServiceUnderTestPort, mockSecurityPort, oAuthMockServer) {

    private val objectMapper = ObjectMapper().apply { registerModule(JavaTimeModule()) }

    private val medlService = MedlService(medlemskapRestConsumer, objectMapper)

    @Test
    fun hentPeriodeListe() {
        setupWireMock()
        executeFromSystem { saksopplysning ->
            saksopplysning.type.shouldBe(SaksopplysningType.MEDL)

            extracting(saksopplysning.kilder) { kilde }
                .shouldHaveSize(1)
                .shouldContainExactly(SaksopplysningKildesystem.MEDL)

            val medlemskapDokument = saksopplysning.dokument as MedlemskapDokument
            medlemskapDokument.medlemsperiode
                .shouldHaveSize(1)
                .first()
                .shouldBeEqualToComparingFields(Medlemsperiode().apply {
                    id = 0
                    type = "PMMEDSKP"
                    status = "status"
                    grunnlagstype = "dekning"
                    land = "lovvalgsland"
                    lovvalg = "lovvalg"
                    trygdedekning = "dekning"
                    kildedokumenttype = "kildedokument"
                    kilde = "kilde"
                    periode = Periode(FOM, TOM)
                })
        }
    }


    override fun getMockData(): String {
        return """
            [
              {
                "dekning": "dekning",
                "fraOgMed": "${FOM}",
                "tilOgMed": "${TOM}",
                "grunnlag": "dekning",
                "helsedel": true,
                "ident": "${FNR}",
                "lovvalg": "lovvalg",
                "lovvalgsland": "lovvalgsland",
                "medlem": true,
                "sporingsinformasjon": {
                  "besluttet": "2022-07-26",
                  "kilde": "kilde",
                  "kildedokument": "kildedokument",
                  "opprettet": "2022-07-26T11:26:02.594Z",
                  "opprettetAv": "opprettetAv",
                  "registrert": "2022-07-26",
                  "sistEndret": "2022-07-26T11:26:02.594Z",
                  "sistEndretAv": "sistEndretAv",
                  "versjon": 0
                },
                "status": "status",
                "statusaarsak": "statusaarsak",
                "studieinformasjon": {
                  "delstudie": true,
                  "soeknadInnvilget": true,
                  "statsborgerland": "statsborgerland",
                  "studieland": "studieland"
                },
                "unntakId": 0
              }]"""
    }

    override fun executeRequest() = medlService.hentPeriodeListe(FNR, FOM, TOM)

    companion object {
        private const val FNR = "12345678990"
        private val FOM = LocalDate.of(2020, 1, 1)
        private val TOM = LocalDate.of(2021, 2, 2)

    }
}
