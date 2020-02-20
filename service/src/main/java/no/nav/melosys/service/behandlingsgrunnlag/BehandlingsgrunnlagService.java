package no.nav.melosys.service.behandlingsgrunnlag;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsgrunnlagService {

    private static final String VERSJON_SOEKNAD_GRUNNLAG = "1.2";

    private final BehandlingsgrunnlagRepository behandlingsgrunnlagRepository;

    public BehandlingsgrunnlagService(BehandlingsgrunnlagRepository behandlingsgrunnlagRepository) {
        this.behandlingsgrunnlagRepository = behandlingsgrunnlagRepository;
    }

    @Transactional(readOnly = true)
    public Behandlingsgrunnlag hentBehandlingsgrunnlag(long behandlingID) throws IkkeFunnetException {
        return behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandlingsgrunnlag for behandling " + behandlingID));
    }

    public Behandlingsgrunnlag opprettSøknadGrunnlag(Behandling behandling,
                                                     SoeknadDokument soeknadDokument) throws FunksjonellException {
        return opprettBehandlingsgrunnlag(behandling, soeknadDokument, BehandlingsGrunnlagType.SØKNAD, VERSJON_SOEKNAD_GRUNNLAG);
    }

    private Behandlingsgrunnlag opprettBehandlingsgrunnlag(Behandling behandling, BehandlingsgrunnlagData behandlingsgrunnlagData,
                                                           BehandlingsGrunnlagType type, String versjon) throws FunksjonellException {

        if (behandlingsgrunnlagRepository.findByBehandling_Id(behandling.getId()).isPresent()) {
            throw new FunksjonellException("Finnes allerede behandlingsgrunnlag for behandling " + behandling.getId());
        }

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandling(behandling);
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
