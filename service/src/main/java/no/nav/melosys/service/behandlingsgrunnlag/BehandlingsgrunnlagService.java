package no.nav.melosys.service.behandlingsgrunnlag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.*;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehandlingsgrunnlagService {

    private static final String VERSJON_SED_GRUNNLAG = "1";
    private static final String VERSJON_SOEKNAD_GRUNNLAG = "1.2";

    private final BehandlingsgrunnlagRepository behandlingsgrunnlagRepository;
    private final BehandlingService behandlingService;
    private final JoarkFasade joarkFasade;

    public BehandlingsgrunnlagService(BehandlingsgrunnlagRepository behandlingsgrunnlagRepository,
                                      BehandlingService behandlingService,
                                      @Qualifier("system") JoarkFasade joarkFasade) {
        this.behandlingsgrunnlagRepository = behandlingsgrunnlagRepository;
        this.behandlingService = behandlingService;
        this.joarkFasade = joarkFasade;
    }

    @Transactional(readOnly = true)
    public Behandlingsgrunnlag hentBehandlingsgrunnlag(long behandlingID) throws IkkeFunnetException {
        return behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandlingsgrunnlag for behandling " + behandlingID));
    }

    public Behandlingsgrunnlag opprettSedGrunnlag(long behandlingID,
                                                  SedGrunnlag sedGrunnlag) throws FunksjonellException, IntegrasjonException {
        return opprettBehandlingsgrunnlag(behandlingID, sedGrunnlag, Behandlingsgrunnlagtyper.SED, VERSJON_SED_GRUNNLAG);
    }

    public Behandlingsgrunnlag opprettSøknadYrkesaktiveEøs(long behandlingID,
                                                           Soeknad soeknad) throws FunksjonellException, IntegrasjonException {
        return opprettBehandlingsgrunnlag(behandlingID, soeknad, Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS, VERSJON_SOEKNAD_GRUNNLAG);
    }

    public Behandlingsgrunnlag opprettSøknadUtsendteArbeidstakereEøs(long behandlingID,
                                                                     String orginalData,
                                                                     Soeknad soeknad,
                                                                     String eksternReferanseID) throws FunksjonellException, IntegrasjonException {
        return opprettBehandlingsgrunnlag(
            behandlingID,
            orginalData,
            soeknad,
            Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS,
            VERSJON_SOEKNAD_GRUNNLAG,
            eksternReferanseID
        );
    }

    public Behandlingsgrunnlag opprettSøknadFolketrygden(long behandlingID,
                                                         SoeknadFtrl soeknad) throws FunksjonellException, IntegrasjonException {
        return opprettBehandlingsgrunnlag(behandlingID, soeknad, Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN, VERSJON_SOEKNAD_GRUNNLAG);
    }

    private Behandlingsgrunnlag opprettBehandlingsgrunnlag(long behandlingID,
                                                           BehandlingsgrunnlagData behandlingsgrunnlagData,
                                                           Behandlingsgrunnlagtyper type,
                                                           String versjon) throws FunksjonellException, IntegrasjonException {

        return opprettBehandlingsgrunnlag(behandlingID, null, behandlingsgrunnlagData, type, versjon);
    }

    private Behandlingsgrunnlag opprettBehandlingsgrunnlag(long behandlingID,
                                                           String originalData,
                                                           BehandlingsgrunnlagData behandlingsgrunnlagData,
                                                           Behandlingsgrunnlagtyper type,
                                                           String versjon) throws FunksjonellException, IntegrasjonException {
        return opprettBehandlingsgrunnlag(behandlingID, originalData, behandlingsgrunnlagData, type, versjon, null);
    }

    private Behandlingsgrunnlag opprettBehandlingsgrunnlag(long behandlingID,
                                                           String originalData,
                                                           BehandlingsgrunnlagData behandlingsgrunnlagData,
                                                           Behandlingsgrunnlagtyper type,
                                                           String versjon,
                                                           String eksternReferanseID) throws FunksjonellException, IntegrasjonException {

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        if (behandling.getBehandlingsgrunnlag() != null) {
            throw new FunksjonellException("Finnes allerede behandlingsgrunnlag for behandling " + behandling.getId());
        }
        if (eksternReferanseID != null && harMottattSøknadMedEksternReferanseID(eksternReferanseID)) {
            throw new FunksjonellException("Det finnes allerede behandlingsgrunnlag med eksterReferanseID " + eksternReferanseID);
        }

        Instant nå = Instant.now();
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandling(behandling);
        behandlingsgrunnlag.setRegistrertDato(nå);
        behandlingsgrunnlag.setEndretDato(nå);
        behandlingsgrunnlag.setType(type);
        behandlingsgrunnlag.setVersjon(versjon);
        behandlingsgrunnlag.setMottaksdato(hentMottaksdato(behandling.getInitierendeJournalpostId()));
        behandlingsgrunnlag.setOriginalData(originalData);
        behandlingsgrunnlag.setEksternReferanseID(eksternReferanseID);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        return behandlingsgrunnlagRepository.save(behandlingsgrunnlag);
    }

    private LocalDate hentMottaksdato(String journalpostID) throws SikkerhetsbegrensningException, IntegrasjonException {
        return journalpostID != null ? joarkFasade.hentMottaksDatoForJournalpost(journalpostID) : null;
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

    public boolean harMottattSøknadMedEksternReferanseID(String eksternReferanseID) {
        return behandlingsgrunnlagRepository.findByEksternReferanseID(eksternReferanseID).isPresent();
    }
}
