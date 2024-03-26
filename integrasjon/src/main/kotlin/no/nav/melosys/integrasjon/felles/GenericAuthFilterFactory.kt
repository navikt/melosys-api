package no.nav.melosys.integrasjon.felles

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Component

@Component
class GenericAuthFilterFactory(
    private val clientConfigurationProperties: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) {

    fun getAzureFilter(clientName: String): GenericContextExchangeFilter {
        return AzureContextExchangeFilter(
            clientConfigurationProperties,
            oAuth2AccessTokenService,
            clientName
        )
    }
}
