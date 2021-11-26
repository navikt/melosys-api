package no.nav.melosys.service.saksopplysninger;


import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SaksoppplysningEventListener {

    private final SaksopplysningerService saksopplysningerService;
    private final PersondataFasade persondataFasade;

    public SaksoppplysningEventListener(SaksopplysningerService saksopplysningerService, PersondataFasade persondataFasade) {
        this.saksopplysningerService = saksopplysningerService;
        this.persondataFasade = persondataFasade;
    }

    @EventListener
    public void lagrePersonopplysninger(BehandlingEndretStatusEvent event) {
        if (List.of(Behandlingsstatus.AVSLUTTET, Behandlingsstatus.IVERKSETTER_VEDTAK, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING).contains(event.getBehandlingsstatus())) {
            Behandling behandling = event.getBehandling();
            Persondata persondata = persondataFasade.hentPerson(behandling.getFagsak().hentAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER);
            PersonMedHistorikk personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.getFagsak().hentAktørID());
            saksopplysningerService.lagrePersonopplysninger(behandling, persondata);
            saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk);
        }
    }
}
