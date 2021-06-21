package no.nav.melosys.service.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.TemaFactory;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SobService {

    private final SakOgBehandlingFasade sakOgBehandlingFasade;
    private final PersondataFasade persondataFasade;
    private final BehandlingService behandlingService;

    @Autowired
    public SobService(SakOgBehandlingFasade sakOgBehandlingFasade, PersondataFasade persondataFasade, BehandlingService behandlingService) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        this.persondataFasade = persondataFasade;
        this.behandlingService = behandlingService;
    }

    private static BehandlingStatusMapper lagBehandlingStatusMapper(String saksnummer, Behandling behandling, String aktørID) {
        return new BehandlingStatusMapper.Builder()
            .medBehandlingsId(behandling.getId())
            .medSaksnummer(saksnummer)
            .medArkivtema(TemaFactory.fraBehandlingstema(behandling.getTema()).getKode())
            .medAktørID(aktørID)
            .build();
    }

    public Saksopplysning finnSakOgBehandlingskjedeListe(String aktørID) {
        return sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(aktørID);
    }

    public void sakOgBehandlingOpprettet(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        sakOgBehandlingFasade.sendBehandlingOpprettet(
            lagBehandlingStatusMapper(fagsak.getSaksnummer(), behandling, fagsak.hentBruker().getAktørId())
        );
    }

    @Deprecated(forRemoval = true)
    public void sakOgBehandlingOpprettet(String saksnummer, Long behandlingId, String aktørID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingId);
        sakOgBehandlingFasade.sendBehandlingOpprettet(lagBehandlingStatusMapper(saksnummer, behandling, aktørID));
    }

    public void sakOgBehandlingAvsluttet(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        sakOgBehandlingFasade.sendBehandlingAvsluttet(
            lagBehandlingStatusMapper(fagsak.getSaksnummer(), behandling, fagsak.hentBruker().getAktørId())
        );
    }

    @Deprecated(forRemoval = true)
    public void sakOgBehandlingAvsluttet(String saksnummer, Long behandlingId, String aktørID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        if (aktørID == null) {
            aktørID = hentAktørIdFraTps(behandling);
        }

        sakOgBehandlingFasade.sendBehandlingAvsluttet(lagBehandlingStatusMapper(saksnummer, behandling, aktørID));
    }

    private String hentAktørIdFraTps(Behandling behandling) {
        Persondata persondata = behandling.hentPersonDokument();
        return persondataFasade.hentAktørIdForIdent(persondata.hentFolkeregisterIdent());
    }
}
