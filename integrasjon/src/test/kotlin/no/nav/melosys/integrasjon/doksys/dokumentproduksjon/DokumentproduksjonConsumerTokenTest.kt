package no.nav.melosys.integrasjon.doksys.dokumentproduksjon

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.melosys.integrasjon.ConsumerWireMockTestBase
import no.nav.melosys.integrasjon.OAuthMockServer
import no.nav.melosys.integrasjon.reststs.RestTokenServiceClient
import no.nav.melosys.integrasjon.reststs.StsWebClientProducer
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Import(
    OAuthMockServer::class,

    StsWebClientProducer::class,
    RestTokenServiceClient::class,
    RestTokenServiceClient::class,

    DokumentproduksjonConsumerConfig::class,
    DokumentproduksjonConsumerProducer::class
)
@WebMvcTest
@AutoConfigureWebClient
@ActiveProfiles("wiremock-test")
class DokumentproduksjonConsumerTokenTest(
    @Autowired private val dokumentproduksjonConsumer: DokumentproduksjonConsumer,
    @Value("\${mockserver.port}") mockServiceUnderTestPort: Int,
    @Value("\${mockserver.security.port}") mockSecurityPort: Int,
    @Autowired oAuthMockServer: OAuthMockServer
) : ConsumerWireMockTestBase<String, ProduserIkkeredigerbartDokumentResponse>(
    mockServiceUnderTestPort,
    mockSecurityPort, oAuthMockServer
) {
    override fun createWireMock(): MappingBuilder = post("/soap/services/dokumentproduksjon/v3")

    override fun defaultStsWireMockStub() {}

    @AfterAll
    fun after() {
        SubjectHandler.set(SpringSubjectHandler(SpringTokenValidationContextHolder()))
    }

    @Test
    fun authorizationSkalKommeFraSystem() {
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappingForSystem()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")

                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeFromSystem()
    }

    @Test
    fun authorizationSkalKommeFraBruker() {
        SubjectHandler.set(object : SubjectHandler() {
            override fun getOidcTokenString() = "--token-from-user--"
            override fun getUserID() = ""
            override fun getUserName() = ""
            override fun getGroups() = mutableListOf<String>()
        })
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappingForBruker()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")

                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeFromController()
    }

    @Test
    fun authorizationSkalKommeFraSystemNårHverkenSystemEllerBrukerErKilde() {
        stsMockServer.stubFor(
            defaultSecurityServiceWireMockMappingForSystem()
                .withRequestBody(
                    notMatching(".*BinarySecurityToken.*")
                )
        )
        verifyHeaders(
            soapActionHeader()
        )
        executeRequest()
    }

    private fun soapActionHeader() = mapOf<String, StringValuePattern>(
        Pair(
            "SOAPAction",
            equalTo("\"http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3/Dokumentproduksjon_v3/produserIkkeredigerbartDokumentRequest\"")
        )
    )

    override fun getMockData(): String = """
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
            <SOAP-ENV:Header/>
            <SOAP-ENV:Body>
                <ns3:produserIkkeredigerbartDokumentResponse
                        xmlns:ns3="http://nav.no/tjeneste/virksomhet/dokumentproduksjon/v3">
                    <response>
                        <journalpostId>989808</journalpostId>
                        <dokumentId>22616</dokumentId>
                    </response>
                </ns3:produserIkkeredigerbartDokumentResponse>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    override fun executeRequest() =
        dokumentproduksjonConsumer.produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest())

    private fun defaultSecurityServiceWireMockMappingForSystem(): MappingBuilder =
        get("/samltoken")
            .withHeader("Authorization", EqualToPattern("Basic dGVzdDp0ZXN0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(sts_response)
            )

    private fun defaultSecurityServiceWireMockMappingForBruker(): MappingBuilder =
        post("/token/exchange?grant_type=urn:ietf:params:oauth:grant-type:token-exchange&requested_token_type=urn:ietf:params:oauth:token-type:saml2&subject_token_type=urn:ietf:params:oauth:token-type:access_token&subject_token=--token-from-user--")
            .withHeader("Authorization", EqualToPattern("Basic dGVzdDp0ZXN0"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(sts_response)
            )

    private val sts_response = """{
          "access_token": "PHNhbWwyOkFzc2VydGlvbiB4bWxuczpzYW1sMj0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvbiIgSUQ9IlNBTUwtMzc3Y2Q5OTgtN2YxZC00NDhlLTlmMGItNzQ0ZjM0N2NmMDk1IiBJc3N1ZUluc3RhbnQ9IjIwMjMtMTItMDNUMjM6NDY6MzYuODc5WiIgVmVyc2lvbj0iMi4wIj48c2FtbDI6SXNzdWVyPklTMDI8L3NhbWwyOklzc3Vlcj48U2lnbmF0dXJlIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48U2lnbmVkSW5mbz48Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjxTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz48UmVmZXJlbmNlIFVSST0iI1NBTUwtMzc3Y2Q5OTgtN2YxZC00NDhlLTlmMGItNzQ0ZjM0N2NmMDk1Ij48VHJhbnNmb3Jtcz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI2VudmVsb3BlZC1zaWduYXR1cmUiLz48VHJhbnNmb3JtIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8xMC94bWwtZXhjLWMxNG4jIi8-PC9UcmFuc2Zvcm1zPjxEaWdlc3RNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjc2hhMSIvPjxEaWdlc3RWYWx1ZT5BSlNIOEpCNlZUZHFGa01ZY1piQkFzc1dWUVE9PC9EaWdlc3RWYWx1ZT48L1JlZmVyZW5jZT48L1NpZ25lZEluZm8-PFNpZ25hdHVyZVZhbHVlPnI4bWJqTWZqZ01WYW9ZTnZralQ0aVAyV0hicUZxZXNrRnBQSjNVSktSRldiVllYd0lodjV0K3dxRDBnbThjVVErWDdtVXFmVjZURWsmIzEzOwoxWkpIVzJzU05BUC83Wloyc1NJUWJzbTE4RjA5WG95UXI2NWFYK1ljSng3U0h2b3VFRmRNcVRDREZQcGlEcHloSzBZUTZqc2FVNDBvJiMxMzsKSkpLSEE0REViM2EvYVlQTjNjZGRFaGVYbUpEN1hlZHh4aVlNN25FZmpJR3VWUGpPMU80c0R6RTdWT0JhVHNodC9NZ1FwcmRtbDFuOCYjMTM7CkNGZ2dBTHI4Y0tCZXBHczlkT2lDZUZ0bzhZM2xDQlloY0RpakF1TW1xaTNqd1E0YUdsNzhDTUpQU2lCVW9oWVlCbWt6Mmo0RjRMVWYmIzEzOwpmdXlVVVVzRElDSk1uUm1IMzMxcE0vcTlqeW8yVGVReDF6SDdaUT09PC9TaWduYXR1cmVWYWx1ZT48S2V5SW5mbz48WDUwOURhdGE-PFg1MDlDZXJ0aWZpY2F0ZT5NSUlHc3pDQ0JadWdBd0lCQWdJVGFnQUFYbjhmamdHTE05UEh2QUFCQUFCZWZ6QU5CZ2txaGtpRzl3MEJBUXNGQURCUU1SVXdFd1lLJiMxMzsKQ1pJbWlaUHlMR1FCR1JZRmJHOWpZV3d4RnpBVkJnb0praWFKay9Jc1pBRVpGZ2R3Y21Wd2NtOWtNUjR3SEFZRFZRUURFeFZDTWpjZyYjMTM7ClNYTnpkV2x1WnlCRFFTQkpiblJsY200d0hoY05Nak13TVRBNU1EZzFOekl5V2hjTk1qVXdNVEE1TURrd056SXlXakJUTVFzd0NRWUQmIzEzOwpWUVFHRXdKT1R6RU5NQXNHQTFVRUNCTUVUMU5NVHpFTk1Bc0dBMVVFQnhNRVQxTk1UekVNTUFvR0ExVUVDaE1EVGtGV01SZ3dGZ1lEJiMxMzsKVlFRRERBOHFMbkJ5WlhCeWIyUXViRzlqWVd3d2dnRWlNQTBHQ1NxR1NJYjNEUUVCQVFVQUE0SUJEd0F3Z2dFS0FvSUJBUURTU3ViWSYjMTM7CjFVdGliNlFaczlFMFdFeXhacGRNamx3UnZjWktScGNWTjdFK29RdTFsbmZTU2NJa1lJQVEyTy9CTDQ4UytMMGtXRCswYURVdzRENVQmIzEzOwpnVlVBTjVaSW9SdkNkY2dGSDdNQ1JubVRIYVJwcGlFdlorYlVhaGhZVk9TK0tMajNoRnFHYkpRSWRvMElwc2tHc2V0RUYvOXVkVjRNJiMxMzsKcmRzSWNjUlNkZ0Z0c3dqRGhKa0d1SVBTaXhwa08yVzRzZzdlUWQzYnFyS1doTnFzWXVHV005aGxnNEswNmd6cXhjcEtXNWNPSklxTyYjMTM7CmxSTVY2M2N0ZUorbFNLbEtDWFJZM2llZVRxV0VCeHVzK2hqRUlJWDF0UmVZVkdjTGdkb0NKVWNKU2hSdEw5SWlOYTA3VlhseXhMS04mIzEzOwpUZmhHT3duMFh3OHV3VnplTndiQSt5WGM3SDl6aEpzTkFnTUJBQUdqZ2dPQk1JSURmVEFhQmdOVkhSRUVFekFSZ2c4cUxuQnlaWEJ5JiMxMzsKYjJRdWJHOWpZV3d3SFFZRFZSME9CQllFRkZOYVlmZk5kd2t3ZHJlWjlNTnppY1VTQWcwOE1COEdBMVVkSXdRWU1CYUFGT05vWTFXOSYjMTM7CjIyamJOelhrWUtsU0I2c2dtcXVOTUlJQklRWURWUjBmQklJQkdEQ0NBUlF3Z2dFUW9JSUJES0NDQVFpR2djZHNaR0Z3T2k4dkwyTnUmIzEzOwpQVUl5TnlVeU1FbHpjM1ZwYm1jbE1qQkRRU1V5TUVsdWRHVnliaXhEVGoxQ01qZEVVbFpYTURBNExFTk9QVU5FVUN4RFRqMVFkV0pzJiMxMzsKYVdNbE1qQnJaWGtsTWpCVFpYSjJhV05sY3l4RFRqMVRaWEoyYVdObGN5eERUajFEYjI1bWFXZDFjbUYwYVc5dUxFUkRQWEJ5WlhCeSYjMTM7CmIyUXNSRU05Ykc5allXdy9ZMlZ5ZEdsbWFXTmhkR1ZTWlhadlkyRjBhVzl1VEdsemREOWlZWE5sUDI5aWFtVmpkRU5zWVhOelBXTlMmIzEzOwpURVJwYzNSeWFXSjFkR2x2YmxCdmFXNTBoanhvZEhSd09pOHZZM0pzTG5CeVpYQnliMlF1Ykc5allXd3ZRM0pzTDBJeU55VXlNRWx6JiMxMzsKYzNWcGJtY2xNakJEUVNVeU1FbHVkR1Z5Ymk1amNtd3dnZ0ZqQmdnckJnRUZCUWNCQVFTQ0FWVXdnZ0ZSTUlHOEJnZ3JCZ0VGQlFjdyYjMTM7CkFvYUJyMnhrWVhBNkx5OHZZMjQ5UWpJM0pUSXdTWE56ZFdsdVp5VXlNRU5CSlRJd1NXNTBaWEp1TEVOT1BVRkpRU3hEVGoxUWRXSnMmIzEzOwphV01sTWpCclpYa2xNakJUWlhKMmFXTmxjeXhEVGoxVFpYSjJhV05sY3l4RFRqMURiMjVtYVdkMWNtRjBhVzl1TEVSRFBYQnlaWEJ5JiMxMzsKYjJRc1JFTTliRzlqWVd3L1kwRkRaWEowYVdacFkyRjBaVDlpWVhObFAyOWlhbVZqZEVOc1lYTnpQV05sY25ScFptbGpZWFJwYjI1QiYjMTM7CmRYUm9iM0pwZEhrd0tnWUlLd1lCQlFVSE1BR0dIbWgwZEhBNkx5OXZZM053TG5CeVpYQnliMlF1Ykc5allXd3ZiMk56Y0RCa0JnZ3ImIzEzOwpCZ0VGQlFjd0FvWllhSFIwY0RvdkwyTnliQzV3Y21Wd2NtOWtMbXh2WTJGc0wwTnliQzlDTWpkRVVsWlhNREE0TG5CeVpYQnliMlF1JiMxMzsKYkc5allXeGZRakkzSlRJd1NYTnpkV2x1WnlVeU1FTkJKVEl3U1c1MFpYSnVLREVwTG1OeWREQU9CZ05WSFE4QkFmOEVCQU1DQmFBdyYjMTM7Ck93WUpLd1lCQkFHQ054VUhCQzR3TEFZa0t3WUJCQUdDTnhVSWdkYlZYSU9BcDF5RTlaMGttNlJUb0xKNWdTU0h2cDFGa1lNaUFnRmsmIzEzOwpBZ0VDTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQlFjREFqQW5CZ2tyQmdFRUFZSTNGUW9FR2pBWU1Bb0dDQ3NHJiMxMzsKQVFVRkJ3TUJNQW9HQ0NzR0FRVUZCd01DTUEwR0NTcUdTSWIzRFFFQkN3VUFBNElCQVFBcHR5djdRbk4xWVVBSXJBRkVmU2ZaT3hvciYjMTM7Ck9sN0NJZktPSXNyY1k3SVhKR281K0JSV01qU1RpQm0xRE5JZXphV1RBSTlOUHJZdkpDbXBVcENvbmdLbDZZdUw3SGx2WUdzck1RNkomIzEzOwpadUU1Nlk4WDUxNzZMN1oxYnpMd1NDQ3JnZXJ1Q0V0dHlVdVBETzc4Mjd1T0srMFZSZ0ZyalpUaFpxNlU5eHp3ek5tbUxzUDNsVlVDJiMxMzsKZk9NTHl1VmM2bEt4SjZ0blV6NkhDY3Y3dVk3NkpXbWFzd0lQbjlzdEZJN1FmL0lEU2JFUVVXUXFoSUxEUHUzQ2J1a0dHTkpaeVRvWSYjMTM7Cm80TmVOYSszRlBPVmlEcTVlQ0d3WVRFa2NIcVJBM1IxYk5rUitBRkpraU9SZVdnQnZseVQxc3BDb1FXaVdZM3Vkb0J3K3JpbWtJSWsmIzEzOwpFSEk0VXdkZWhuRlA8L1g1MDlDZXJ0aWZpY2F0ZT48WDUwOUlzc3VlclNlcmlhbD48WDUwOUlzc3Vlck5hbWU-Q049QjI3IElzc3VpbmcgQ0EgSW50ZXJuLERDPXByZXByb2QsREM9bG9jYWw8L1g1MDlJc3N1ZXJOYW1lPjxYNTA5U2VyaWFsTnVtYmVyPjIzNjM4NzkxMTY2NTE3MzkzNjMzOTMzNjE5NzIwOTQ4MjY2MDk5MjI1NjM3MTE8L1g1MDlTZXJpYWxOdW1iZXI-PC9YNTA5SXNzdWVyU2VyaWFsPjwvWDUwOURhdGE-PC9LZXlJbmZvPjwvU2lnbmF0dXJlPjxzYW1sMjpTdWJqZWN0PjxzYW1sMjpOYW1lSUQgRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoxLjE6bmFtZWlkLWZvcm1hdDp1bnNwZWNpZmllZCI-c3J2bWVsb3N5czwvc2FtbDI6TmFtZUlEPjxzYW1sMjpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI-PHNhbWwyOlN1YmplY3RDb25maXJtYXRpb25EYXRhIE5vdEJlZm9yZT0iMjAyMy0xMi0wM1QyMzo0NjozNi44NzlaIiBOb3RPbk9yQWZ0ZXI9IjIwMjMtMTItMDRUMDA6Mzk6MjIuODc5WiIvPjwvc2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbj48L3NhbWwyOlN1YmplY3Q-PHNhbWwyOkNvbmRpdGlvbnMgTm90QmVmb3JlPSIyMDIzLTEyLTAzVDIzOjQ2OjM2Ljg3OVoiIE5vdE9uT3JBZnRlcj0iMjAyMy0xMi0wNFQwMDozOToyMi44NzlaIi8-PHNhbWwyOkF0dHJpYnV0ZVN0YXRlbWVudD48c2FtbDI6QXR0cmlidXRlIE5hbWU9ImlkZW50VHlwZSIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDp1cmkiPjxzYW1sMjpBdHRyaWJ1dGVWYWx1ZT5TeXN0ZW1yZXNzdXJzPC9zYW1sMjpBdHRyaWJ1dGVWYWx1ZT48L3NhbWwyOkF0dHJpYnV0ZT48c2FtbDI6QXR0cmlidXRlIE5hbWU9ImF1dGhlbnRpY2F0aW9uTGV2ZWwiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6dXJpIj48c2FtbDI6QXR0cmlidXRlVmFsdWU-MDwvc2FtbDI6QXR0cmlidXRlVmFsdWU-PC9zYW1sMjpBdHRyaWJ1dGU-PHNhbWwyOkF0dHJpYnV0ZSBOYW1lPSJjb25zdW1lcklkIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OnVyaSI-PHNhbWwyOkF0dHJpYnV0ZVZhbHVlPnNydm1lbG9zeXM8L3NhbWwyOkF0dHJpYnV0ZVZhbHVlPjwvc2FtbDI6QXR0cmlidXRlPjxzYW1sMjpBdHRyaWJ1dGUgTmFtZT0iYXVkaXRUcmFja2luZ0lkIiBOYW1lRm9ybWF0PSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OnVyaSI-PHNhbWwyOkF0dHJpYnV0ZVZhbHVlPjA0M2JlNjY3LTg3MGEtNDZmYi1iMmNkLTNhNzA0ZTc1ZDRkNjwvc2FtbDI6QXR0cmlidXRlVmFsdWU-PC9zYW1sMjpBdHRyaWJ1dGU-PC9zYW1sMjpBdHRyaWJ1dGVTdGF0ZW1lbnQ-PC9zYW1sMjpBc3NlcnRpb24-",
          "issued_token_type": "urn:ietf:params:oauth:token-type:saml2",
          "token_type": "Bearer",
          "expires_in": 3162
        }
        """.trimIndent()
}
