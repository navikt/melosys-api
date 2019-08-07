package no.nav.melosys.saksflyt.felles;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import org.springframework.stereotype.Component;

@Component
public class OppdaterMedlFelles {
    private final TpsFasade tpsFasade;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;

    public OppdaterMedlFelles(TpsFasade tpsFasade,
                              BehandlingsresultatService behandlingsresultatService,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository) {
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
    }

    public String hentFnr(Behandling behandling) throws TekniskException, IkkeFunnetException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);
        return tpsFasade.hentIdentForAktørId(bruker.getAktørId());
    }

    public Lovvalgsperiode hentLovvalgsperiode(Behandling behandling) throws FunksjonellException {
        return hentBehandlingsresultat(behandling).hentValidertLovvalgsperiode();
    }

    public Behandlingsresultat hentBehandlingsresultat(Behandling behandling) throws IkkeFunnetException {
        return behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
    }

    public void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling " + behandlingID);
        }
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }
}
