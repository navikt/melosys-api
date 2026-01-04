package no.nav.melosys.saksflyt.steg.saksopplysninger

import mu.KotlinLogging
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.person.Informasjonsbehov
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

/**
 * Saga-steg som lagrer personopplysninger (PDL_PERSOPL, PDL_PERS_SAKS) på behandlingen.
 *
 * Dette steget erstatter logikken fra [SaksoppplysningEventListener.lagrePersonopplysninger]
 * for IVERKSETTER_VEDTAK-status for å eliminere race conditions mellom den synkrone
 * event-listeneren og asynkron saga-kjøring.
 *
 * @see no.nav.melosys.service.saksopplysninger.SaksoppplysningEventListener
 */
@Component
class LagrePersonopplysninger(
    private val behandlingService: BehandlingService,
    private val saksopplysningerService: SaksopplysningerService,
    private val persondataFasade: PersondataFasade,
    private val avklartefaktaService: AvklartefaktaService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.LAGRE_PERSONOPPLYSNINGER

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandlingId = prosessinstans.hentBehandling.id
        // Last alltid behandling på nytt for å få oppdatert tilstand etter at HTTP-transaksjonen er committet
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)

        // Behandle kun hvis hovedpart er BRUKER (samme betingelse som i event-listeneren)
        if (behandling.fagsak.hovedpartRolle != Aktoersroller.BRUKER) {
            log.debug { "Hopper over LAGRE_PERSONOPPLYSNINGER for behandling $behandlingId - hovedpart er ikke BRUKER" }
            return
        }

        // Lagre PDL_PERSOPL hvis den mangler
        if (behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL))) {
            val persondata = hentPersondata(behandling)
            saksopplysningerService.lagrePersonopplysninger(behandling, persondata)
            log.info { "Lagret PDL_PERSOPL saksopplysning for behandling $behandlingId" }
        }

        // Lagre PDL_PERS_SAKS hvis den mangler
        if (behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS))) {
            val personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.fagsak.hentBrukersAktørID())
            saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)
            log.info { "Lagret PDL_PERS_SAKS saksopplysning for behandling $behandlingId" }
        }
    }

    private fun hentPersondata(behandling: no.nav.melosys.domain.Behandling) =
        if (avklartefaktaService.hentAvklarteMedfølgendeBarn(behandling.id).finnes() ||
            avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandling.id).finnes()
        ) {
            persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER)
        } else {
            persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID())
        }
}
