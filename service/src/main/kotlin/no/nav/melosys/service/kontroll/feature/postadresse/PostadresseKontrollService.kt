package no.nav.melosys.service.kontroll.feature.postadresse

import no.nav.melosys.service.kontroll.regler.PersonRegler
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import org.springframework.stereotype.Service

@Service
class PostadresseKontrollService(
    private val persondataService: PersondataService,
    private val organisasjonOppslagService: OrganisasjonOppslagService,
) {
    fun harRegistrertAdresse(kontekst: PostadressesjekkKontekst): Boolean {
        if (!kontekst.brukerID.isNullOrBlank()) {
            val person = persondataService.hentPerson(kontekst.brukerID)
            return PersonRegler.harRegistrertAdresse(person)
        }

        if (!kontekst.orgnr.isNullOrEmpty()) {
            val organisasjon = organisasjonOppslagService.hentOrganisasjon(kontekst.orgnr)
            return organisasjon.harRegistrertAdresse()
        }

        return true
    }
}
