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
 * Saga step that saves personopplysninger (PDL_PERSOPL, PDL_PERS_SAKS) to the behandling.
 *
 * This step replaces the logic from [SaksoppplysningEventListener.lagrePersonopplysninger]
 * for IVERKSETTER_VEDTAK status to eliminate race conditions between the synchronous
 * event listener and async saga execution.
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
        // Always reload behandling to get fresh state after HTTP transaction committed
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)

        // Only process if hovedpart is BRUKER (same condition as event listener)
        if (behandling.fagsak.hovedpartRolle != Aktoersroller.BRUKER) {
            log.debug { "Skipping LAGRE_PERSONOPPLYSNINGER for behandling $behandlingId - hovedpart is not BRUKER" }
            return
        }

        // Save PDL_PERSOPL if missing
        if (behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERSOPL))) {
            val persondata = hentPersondata(behandling)
            saksopplysningerService.lagrePersonopplysninger(behandling, persondata)
            log.info { "Saved PDL_PERSOPL saksopplysning for behandling $behandlingId" }
        }

        // Save PDL_PERS_SAKS if missing
        if (behandling.manglerSaksopplysningerAvType(listOf(SaksopplysningType.PDL_PERS_SAKS))) {
            val personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.fagsak.hentBrukersAktørID())
            saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk)
            log.info { "Saved PDL_PERS_SAKS saksopplysning for behandling $behandlingId" }
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
