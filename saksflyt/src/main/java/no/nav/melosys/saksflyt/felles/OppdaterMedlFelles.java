package no.nav.melosys.saksflyt.felles;

import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import org.springframework.stereotype.Component;

@Component
public class OppdaterMedlFelles {
    private TpsFasade tpsFasade;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;

    public OppdaterMedlFelles(TpsFasade tpsFasade,
                              BehandlingsresultatRepository behandlingsresultatRepository,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository) {
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
    }

    public String hentFnr(Behandling behandling) throws TekniskException, IkkeFunnetException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentAktørMedRolleType(Aktoersroller.BRUKER);
        return tpsFasade.hentIdentForAktørId(bruker.getAktørId());
    }

    public Lovvalgsperiode hentLovvalgsperiode(Behandling behandling) throws FunksjonellException {
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandling);

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Det er enten ingen eller for mange Lovvalgsperioder for behandling " + behandling.getId());
        }
        return lovvalgsperioder.iterator().next();
    }

    public Behandlingsresultat hentBehandlingsresultat(Behandling behandling) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new IkkeFunnetException("Opprettelse av periode i MEDL feilet fordi behandlingsresultat med behandling ID " + behandling.getId() + " ikke finnes."));
        return behandlingsresultat;
    }

    public void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling " + behandlingID);
        }
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }
}
