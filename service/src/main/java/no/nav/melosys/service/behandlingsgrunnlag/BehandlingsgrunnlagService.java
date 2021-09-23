package no.nav.melosys.service.behandlingsgrunnlag;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.*;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
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
    public Behandlingsgrunnlag hentBehandlingsgrunnlag(long behandlingID) {
        return behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandlingsgrunnlag for behandling " + behandlingID));
    }

    public void opprettSedGrunnlag(long behandlingID,
                                   SedGrunnlag sedGrunnlag) {
        opprettBehandlingsgrunnlag(behandlingID, sedGrunnlag, Behandlingsgrunnlagtyper.SED, VERSJON_SED_GRUNNLAG);
    }

    public void opprettSøknadYrkesaktiveEøs(long behandlingID,
                                            Soeknad soeknad) {
        opprettBehandlingsgrunnlag(behandlingID, soeknad, Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    public void opprettSøknadUtsendteArbeidstakereEøs(long behandlingID,
                                                      String orginalData,
                                                      Soeknad soeknad,
                                                      String eksternReferanseID) {
        opprettBehandlingsgrunnlag(behandlingID, orginalData, soeknad,
            Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS, VERSJON_SOEKNAD_GRUNNLAG,
            eksternReferanseID);
    }

    public void opprettSøknadOmMedlemskapIFolketrygden(long behandlingID,
                                                       SoeknadFtrl soeknad) {
        opprettBehandlingsgrunnlag(behandlingID, soeknad, SØKNAD_FOLKETRYGDEN,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    private void opprettBehandlingsgrunnlag(long behandlingID,
                                            BehandlingsgrunnlagData behandlingsgrunnlagData,
                                            Behandlingsgrunnlagtyper type,
                                            String versjon) {

        opprettBehandlingsgrunnlag(behandlingID, null, behandlingsgrunnlagData, type, versjon, null);
    }

    private void opprettBehandlingsgrunnlag(long behandlingID,
                                            String originalData,
                                            BehandlingsgrunnlagData behandlingsgrunnlagData,
                                            Behandlingsgrunnlagtyper type,
                                            String versjon,
                                            String eksternReferanseID) {

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
    private LocalDate hentMottaksdato(Behandlingsgrunnlagtyper grunnlagtype, String journalpostID) {
        if (grunnlagtype == SØKNAD_FOLKETRYGDEN) {
            return finnMottaksdato(journalpostID).orElseThrow(
                () -> new FunksjonellException("Mottaksdato trenges for " + SØKNAD_FOLKETRYGDEN.getKode()));
        } else {
            return finnMottaksdato(journalpostID).orElse(null);
        }
    }

    private Optional<LocalDate> finnMottaksdato(String journalpostID) {
        return Optional.ofNullable(journalpostID)
            .map(joarkFasade::hentMottaksDatoForJournalpost);
    }

    @Transactional
    public Behandlingsgrunnlag oppdaterBehandlingsgrunnlag(long behandlingID, JsonNode behandlingsgrunnlagDataJson) {
        Behandlingsgrunnlag behandlingsgrunnlag = hentBehandlingsgrunnlag(behandlingID);
        behandlingsgrunnlag.setJsonData(behandlingsgrunnlagDataJson.toString());
        return behandlingsgrunnlagRepository.saveAndFlush(behandlingsgrunnlag);
    }

    @Transactional
    public Behandlingsgrunnlag oppdaterBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        BehandlingsgrunnlagKonverterer.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandlingsgrunnlagRepository.saveAndFlush(behandlingsgrunnlag);
    }

    @Transactional
    public Behandlingsgrunnlag oppdaterBehandlingsgrunnlagPeriodeOgLand(long behandlingID, Periode periode, Soeknadsland soeknadsland) {
        Behandlingsgrunnlag behandlingsgrunnlag = hentBehandlingsgrunnlag(behandlingID);
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode = periode;
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland = soeknadsland;
        BehandlingsgrunnlagKonverterer.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandlingsgrunnlagRepository.saveAndFlush(behandlingsgrunnlag);
    }

    public Optional<Behandlingsgrunnlag> finnBehandlingsgrunnlag(Long behandlingID) {
        return behandlingsgrunnlagRepository.findByBehandling_Id(behandlingID);
    }

    public boolean harMottattSøknadMedEksternReferanseID(String eksternReferanseID) {
        return !behandlingsgrunnlagRepository.findByEksternReferanseID(eksternReferanseID).isEmpty();
    }
}
