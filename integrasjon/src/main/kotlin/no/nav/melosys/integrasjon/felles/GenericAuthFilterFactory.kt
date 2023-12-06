package no.nav.melosys.integrasjon.felles

import no.nav.melosys.integrasjon.reststs.RestSTSService
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Component

@Component
class GenericAuthFilterFactory(
    private val restSTSService: RestSTSService,
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) {
    fun getStsAzureFilter(clientName: String): GenericContextExchangeFilter {
        return StsAzureContextExchangeFilter(
            restSTSService,
            clientConfigurationProperties,
            oAuth2AccessTokenService,
            clientName
        )
    }

    fun getAzureFilter(clientName: String): GenericContextExchangeFilter {
        return AzureContextExchangeFilter(
            clientConfigurationProperties,
            oAuth2AccessTokenService,
            clientName
        )
    }
}
