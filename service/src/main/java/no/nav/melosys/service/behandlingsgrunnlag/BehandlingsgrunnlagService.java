package no.nav.melosys.service.behandlingsgrunnlag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.*;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN;

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

    public void opprettSedGrunnlag(long behandlingID,
                                   SedGrunnlag sedGrunnlag) throws FunksjonellException {
        opprettBehandlingsgrunnlag(behandlingID, sedGrunnlag, Behandlingsgrunnlagtyper.SED, VERSJON_SED_GRUNNLAG);
    }

    public void opprettSøknadYrkesaktiveEøs(long behandlingID,
                                            Soeknad soeknad) throws FunksjonellException {
        opprettBehandlingsgrunnlag(behandlingID, soeknad, Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    public void opprettSøknadUtsendteArbeidstakereEøs(long behandlingID,
                                                      String orginalData,
                                                      Soeknad soeknad,
                                                      String eksternReferanseID) throws FunksjonellException {
        opprettBehandlingsgrunnlag(behandlingID, orginalData, soeknad,
            Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS, VERSJON_SOEKNAD_GRUNNLAG,
            eksternReferanseID);
    }

    public void opprettSøknadFolketrygden(long behandlingID,
                                          SoeknadFtrl soeknad) throws FunksjonellException {
        opprettBehandlingsgrunnlag(behandlingID, soeknad, SØKNAD_FOLKETRYGDEN,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    private void opprettBehandlingsgrunnlag(long behandlingID,
                                            BehandlingsgrunnlagData behandlingsgrunnlagData,
                                            Behandlingsgrunnlagtyper type,
                                            String versjon) throws FunksjonellException {

        opprettBehandlingsgrunnlag(behandlingID, null, behandlingsgrunnlagData, type, versjon, null);
    }

    private void opprettBehandlingsgrunnlag(long behandlingID,
                                            String originalData,
                                            BehandlingsgrunnlagData behandlingsgrunnlagData,
                                            Behandlingsgrunnlagtyper type,
                                            String versjon,
                                            String eksternReferanseID) throws FunksjonellException {

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
        behandlingsgrunnlag.setMottaksdato(hentMottaksdato(type, behandling.getInitierendeJournalpostId()));
        behandlingsgrunnlag.setOriginalData(originalData);
        behandlingsgrunnlag.setEksternReferanseID(eksternReferanseID);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);
        behandlingsgrunnlagRepository.save(behandlingsgrunnlag);
    }

    // Gjør mottaksdato nødvendig for kun folketrygden da nåværende journalpost-oppslag kun støtter inngående
    // dokumenter og fordi mottaksdato ikke er like viktig for andre grunnlagstyper.
    private LocalDate hentMottaksdato(Behandlingsgrunnlagtyper grunnlagtype, String journalpostID) throws
        FunksjonellException {
        if (grunnlagtype == SØKNAD_FOLKETRYGDEN) {
            return finnMottaksdato(journalpostID).orElseThrow(
                () -> new FunksjonellException("Mottaksdato trenges for " + SØKNAD_FOLKETRYGDEN.getKode()));
        } else {
            return finnMottaksdato(journalpostID).orElse(null);
        }
    }

    private Optional<LocalDate> finnMottaksdato(String journalpostID) throws FunksjonellException {
        LocalDate mottaksDatoFraJournalpostID;
        if (journalpostID == null) {
            return Optional.empty();
        }
        try {
             mottaksDatoFraJournalpostID = joarkFasade.hentMottaksDatoForJournalpost(journalpostID);
        } catch (IkkeInngaaendeJournalpostException e) {
            return Optional.empty();
        } catch (IntegrasjonException e) {
            throw new IllegalStateException(e);
        }
        return Optional.of(mottaksDatoFraJournalpostID);
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
        return !behandlingsgrunnlagRepository.findByEksternReferanseID(eksternReferanseID).isEmpty();
    }
}
