package no.nav.melosys.service.saksopplysninger

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.person.Informasjonsbehov
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.persondata.PersondataFasade
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Felles komponent for lagring av personopplysninger (PDL_PERSOPL, PDL_PERS_SAKS) på en behandling.
 *
 * Brukes av både [SaksoppplysningEventListener] og saga-steget LagrePersonopplysninger
 * for å unngå kodeduplisering og sikre konsistent oppførsel.
 */
@Component
class PersonopplysningerLagrer(
    private val saksopplysningerService: SaksopplysningerService,
    private val persondataFasade: PersondataFasade,
    private val avklartefaktaService: AvklartefaktaService
) {
    /**
     * Lagrer personopplysninger på behandlingen hvis de mangler.
     *
     * @param behandling Behandlingen som skal få lagret personopplysninger
     * @return true hvis minst én saksopplysning ble lagret, false ellers
     */
    fun lagreHvisMangler(behandling: Behandling): Boolean {
        if (behandling.fagsak.hovedpartRolle != Aktoersroller.BRUKER) {
            log.debug { "Hopper over lagring av personopplysninger for behandling ${behandling.id} - hovedpart er ikke BRUKER" }
            return false
        }

        var lagret = false

        if (behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL))) {
            val persondata = hentPersondata(behandling)
            saksopplysningerService.lagrePersonopplysninger(behandling, persondata)
            log.info { "Lagret PDL_PERSOPL saksopplysning for behandling ${behandling.id}" }
            lagret = true
        }

        if (behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS))) {
            val personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.fagsak.hentBrukersAktørID())
            saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)
            log.info { "Lagret PDL_PERS_SAKS saksopplysning for behandling ${behandling.id}" }
            lagret = true
        }

        return lagret
    }

    private fun hentPersondata(behandling: Behandling) =
        if (harMedfølgendeFamilie(behandling.id)) {
            persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER)
        } else {
            persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID())
        }

    private fun harMedfølgendeFamilie(behandlingId: Long): Boolean =
        avklartefaktaService.hentAvklarteMedfølgendeBarn(behandlingId).finnes() ||
            avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandlingId).finnes()
}
