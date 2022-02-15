package no.nav.melosys.melosysmock.pdl

import no.nav.melosys.generated.graphql.api.HentIdenterQueryResolver
import no.nav.melosys.generated.graphql.model.IdentGruppeDto
import no.nav.melosys.generated.graphql.model.IdentInformasjonDto
import no.nav.melosys.generated.graphql.model.IdentlisteDto
import no.nav.melosys.melosysmock.person.PersonRepo
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.context.annotation.SessionScope

@Component
@RequestScope
class HentIdenterQuery : HentIdenterQueryResolver {

    override fun hentIdenter(ident: String, grupper: List<IdentGruppeDto>?, historikk: Boolean?) =
        PersonRepo.finnVedIdent(ident)?.let {
            IdentlisteDto(
                listOf(
                    IdentInformasjonDto(
                        ident = it.ident,
                        gruppe = IdentGruppeDto.FOLKEREGISTERIDENT,
                        historisk = false
                    ),
                    IdentInformasjonDto(
                        ident = it.aktørId,
                        gruppe = IdentGruppeDto.AKTORID,
                        historisk = false
                    )
                )
            )
        } ?: IdentlisteDto(emptyList())
}
