package no.nav.melosys.service.behandlingsgrunnlag;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.service.BehandlingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsgrunnlagService {

    private static final String VERSJON_SED_GRUNNLAG = "1";
    private static final String VERSJON_SOEKNAD_GRUNNLAG = "1.2";
    private static final String VERSJON_GENERELT_GRUNNLAG = "1";

    private final BehandlingsgrunnlagRepository behandlingsgrunnlagRepository;
    private final BehandlingService behandlingService;

    public BehandlingsgrunnlagService(BehandlingsgrunnlagRepository behandlingsgrunnlagRepository, BehandlingService behandlingService) {
        this.behandlingsgrunnlagRepository = behandlingsgrunnlagRepository;
        this.behandlingService = behandlingService;
    }

    @Transactional(readOnly = true)
    public Behandlingsgrunnlag hentBehandlingsgrunnlag(long behandlingID) throws IkkeFunnetException {
        return behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandlingsgrunnlag for behandling " + behandlingID));
    }

    public Behandlingsgrunnlag opprettSedGrunnlag(long behandlingID,
                                                  SedGrunnlag sedGrunnlag) throws FunksjonellException {
        return opprettBehandlingsgrunnlag(behandlingID, sedGrunnlag, BehandlingsGrunnlagType.SED, VERSJON_SED_GRUNNLAG);
    }

    public Behandlingsgrunnlag opprettSøknadGrunnlag(long behandlingID,
                                                     SoeknadDokument soeknadDokument) throws FunksjonellException {
        return opprettBehandlingsgrunnlag(behandlingID, soeknadDokument, BehandlingsGrunnlagType.SØKNAD, VERSJON_SOEKNAD_GRUNNLAG);
    }

    public Behandlingsgrunnlag opprettBehandlingsgrunnlag(long behandlingID,
                                                          BehandlingsgrunnlagData behandlingsgrunnlagData) throws FunksjonellException {
        return opprettBehandlingsgrunnlag(behandlingID, behandlingsgrunnlagData, BehandlingsGrunnlagType.GENERELT, VERSJON_GENERELT_GRUNNLAG);
    }

    private Behandlingsgrunnlag opprettBehandlingsgrunnlag(long behandlingID, BehandlingsgrunnlagData behandlingsgrunnlagData,
                                                           BehandlingsGrunnlagType type, String versjon) throws FunksjonellException {

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        if (behandling.getBehandlingsgrunnlag() != null) {
            throw new FunksjonellException("Finnes allerede behandlingsgrunnlag for behandling " + behandling.getId());
        }

        Instant nå = Instant.now();
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandling(behandling);
        behandlingsgrunnlag.setRegistrertDato(nå);
        behandlingsgrunnlag.setEndretDato(nå);
        behandlingsgrunnlag.setType(type);
        behandlingsgrunnlag.setVersjon(versjon);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        return behandlingsgrunnlagRepository.save(behandlingsgrunnlag);
    }

    @Transactional
    public Behandlingsgrunnlag oppdaterBehandlingsgrunnlag(long behandlingID, JsonNode behandlingsgrunnlagDataJson) throws IkkeFunnetException {

        Behandlingsgrunnlag behandlingsgrunnlag = behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandlingsgrunnlag for behandling " + behandlingID));

        behandlingsgrunnlag.setJsonData(behandlingsgrunnlagDataJson.toString());
        return behandlingsgrunnlagRepository.saveAndFlush(behandlingsgrunnlag);
    }

    public Optional<Behandlingsgrunnlag> finnBehandlingsgrunnlag(Long behandlingID) {
        return behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID);
    }
}
