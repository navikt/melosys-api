package no.nav.melosys.service.saksopplysninger;


import java.util.List;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretStatusEvent;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SaksoppplysningEventListener {

    private final SaksopplysningerService saksopplysningerService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public SaksoppplysningEventListener(SaksopplysningerService saksopplysningerService,
                                        BehandlingService behandlingService,
                                        @Qualifier("system") PersondataFasade persondataFasade,
                                        Unleash unleash) {
        this.saksopplysningerService = saksopplysningerService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    @EventListener
    @Transactional
    public void lagrePersonopplysninger(BehandlingEndretStatusEvent event) {
        if (unleash.isEnabled("melosys.pdl.aktiv")) {
            if (List.of(Behandlingsstatus.AVSLUTTET, Behandlingsstatus.IVERKSETTER_VEDTAK, Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING).contains(event.getBehandlingsstatus())) {
                Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(event.getBehandling().getId());

                if (behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERSOPL))) {
                    Persondata persondata = persondataFasade.hentPerson(behandling.getFagsak().hentAktørID(), Informasjonsbehov.MED_FAMILIERELASJONER);
                    saksopplysningerService.lagrePersonopplysninger(behandling, persondata);
                }

                if (behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERS_SAKS))) {
                    PersonMedHistorikk personMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.getFagsak().hentAktørID());
                    saksopplysningerService.lagrePersonMedHistorikk(behandling, personMedHistorikk);
                }
            }
        }
    }
}
