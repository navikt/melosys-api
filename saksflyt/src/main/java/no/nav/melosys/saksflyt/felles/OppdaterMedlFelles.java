package no.nav.melosys.saksflyt.felles;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OppdaterMedlFelles {
    private static final Logger log = LoggerFactory.getLogger(OppdaterMedlFelles.class);

    private final TpsFasade tpsFasade;
    private final MedlFasade medlFasade;
    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;

    public OppdaterMedlFelles(TpsFasade tpsFasade,
                              MedlFasade medlFasade,
                              BehandlingsresultatService behandlingsresultatService,
                              LovvalgsperiodeRepository lovvalgsperiodeRepository,
                              AnmodningsperiodeRepository anmodningsperiodeRepository) {
        this.tpsFasade = tpsFasade;
        this.medlFasade = medlFasade;
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
    }

    public String hentFnr(Behandling behandling) throws TekniskException, IkkeFunnetException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentBruker();
        return tpsFasade.hentIdentForAktørId(bruker.getAktørId());
    }

    public Lovvalgsperiode hentLovvalgsperiode(Behandling behandling) throws FunksjonellException {
        return hentBehandlingsresultat(behandling).hentValidertLovvalgsperiode();
    }

    private Optional<Lovvalgsperiode> finnLovvalgsperiode(Behandling behandling) throws IkkeFunnetException {
        return hentBehandlingsresultat(behandling).getLovvalgsperioder().stream().findFirst();
    }

    public Anmodningsperiode hentAnmodningsperiode(Behandling behandling) throws FunksjonellException {
        return hentBehandlingsresultat(behandling).hentValidertAnmodningsperiode();
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

    public void lagreMedlPeriodeId(Long medlPeriodeID, Anmodningsperiode anmodningsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling " + behandlingID);
        }
        anmodningsperiode.setMedlPeriodeID(medlPeriodeID);
        anmodningsperiodeRepository.save(anmodningsperiode);
    }

    public void avsluttTidligerMedlPeriode(Fagsak fagsak) throws FunksjonellException {
        Behandling tidligereBehandling = fagsak.getTidligsteInaktiveBehandling();

        if (tidligereBehandling != null) {
            Optional<Lovvalgsperiode> lovvalgsperiode = finnLovvalgsperiode(tidligereBehandling);
            if (lovvalgsperiode.isPresent() && lovvalgsperiode.get().getMedlPeriodeID() != null) {
                log.info("Avslutter tidligere periode for fagsak {}", fagsak.getSaksnummer());
                medlFasade.avvisPeriode(lovvalgsperiode.get().getMedlPeriodeID(), StatusaarsakMedl.AVVIST);
            }
        }
    }
}
