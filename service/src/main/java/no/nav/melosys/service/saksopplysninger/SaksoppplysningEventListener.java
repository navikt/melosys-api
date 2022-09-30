package no.nav.melosys.service.saksopplysninger;


import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SaksoppplysningEventListener {

    private final SaksopplysningerService saksopplysningerService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final AvklartefaktaService avklartefaktaService;

    public SaksoppplysningEventListener(SaksopplysningerService saksopplysningerService,
                                        BehandlingService behandlingService,
                                        PersondataFasade persondataFasade,
                                        AvklartefaktaService avklartefaktaService) {
        this.saksopplysningerService = saksopplysningerService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.avklartefaktaService = avklartefaktaService;
    }

    @EventListener
    @Transactional
    public void lagrePersonopplysninger(BehandlingEndretStatusEvent event) {
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(event.getBehandling().getId());
        if (List.of(Behandlingsstatus.AVSLUTTET, Behandlingsstatus.IVERKSETTER_VEDTAK, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING).contains(event.getBehandlingsstatus())
            && behandling.getFagsak().getHovedpartRolle() == Aktoersroller.BRUKER
        ) {
            if (behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERSOPL))) {
                saksopplysningerService.lagrePersonopplysninger(behandling, hentPersondata(behandling));
            }

            if (behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERS_SAKS))) {
                PersonMedHistorikk personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.getFagsak().hentBrukersAktørID());
                saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk);
            }
        }
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (avklartefaktaService.hentAvklarteMedfølgendeBarn(behandling.getId()).finnes()
            || avklartefaktaService.hentAvklarteMedfølgendeEktefelle(behandling.getId()).finnes()) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER);
        } else {
            return persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
        }
    }
}
