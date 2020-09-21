package no.nav.melosys.service.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.TemaFactory;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SobService {

    private final SakOgBehandlingFasade sakOgBehandlingFasade;
    private final TpsFasade tpsFasade;
    private final BehandlingService behandlingService;

    @Autowired
    public SobService(SakOgBehandlingFasade sakOgBehandlingFasade, TpsFasade tpsFasade, BehandlingService behandlingService) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
        this.tpsFasade = tpsFasade;
        this.behandlingService = behandlingService;
    }

    private static BehandlingStatusMapper lagBehandlingStatusMapper(String saksnummer, Behandling behandling, String aktørID) throws FunksjonellException {
        return new BehandlingStatusMapper.Builder()
            .medBehandlingsId(behandling.getId())
            .medSaksnummer(saksnummer)
            .medArkivtema(TemaFactory.fraBehandlingstema(behandling.getTema()).getKode())
            .medAktørID(aktørID)
            .build();
    }

    public Saksopplysning finnSakOgBehandlingskjedeListe(String aktørID) throws IntegrasjonException {
        return sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(aktørID);
    }

    public void sakOgBehandlingOpprettet(String saksnummer, Long behandlingId, String aktørID) throws IntegrasjonException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingId);
        sakOgBehandlingFasade.sendBehandlingOpprettet(lagBehandlingStatusMapper(saksnummer, behandling, aktørID));
    }

    public void sakOgBehandlingAvsluttet(long behandlingID) throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        Fagsak fagsak = behandling.getFagsak();
        sakOgBehandlingFasade.sendBehandlingAvsluttet(
            lagBehandlingStatusMapper(fagsak.getSaksnummer(), behandling, fagsak.hentBruker().getAktørId())
        );
    }

    @Deprecated(forRemoval = true)
    public void sakOgBehandlingAvsluttet(String saksnummer, Long behandlingId, String aktørID) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        if (aktørID == null) {
            aktørID = hentAktørIdFraTps(behandling);
        }

        sakOgBehandlingFasade.sendBehandlingAvsluttet(lagBehandlingStatusMapper(saksnummer, behandling, aktørID));
    }

    private String hentAktørIdFraTps(Behandling behandling) throws TekniskException, IkkeFunnetException {
        PersonDokument personDokument = behandling.hentPersonDokument();
        return tpsFasade.hentAktørIdForIdent(personDokument.fnr);
    }
}
