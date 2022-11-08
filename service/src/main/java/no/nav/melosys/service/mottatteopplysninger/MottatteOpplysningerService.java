package no.nav.melosys.service.mottatteopplysninger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.mottatteopplysninger.*;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.MottatteOpplysningerRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper.SØKNAD_FOLKETRYGDEN;
import static no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper.SØKNAD_TRYGDEAVTALE;

@Service
public class MottatteOpplysningerService {
    private static final Logger log = LoggerFactory.getLogger(MottatteOpplysningerService.class);

    private static final String VERSJON_SED_GRUNNLAG = "1";
    private static final String VERSJON_SOEKNAD_GRUNNLAG = "1.2";

    private final MottatteOpplysningerRepository mottatteOpplysningerRepository;
    private final BehandlingService behandlingService;
    private final JoarkFasade joarkFasade;

    private final Unleash unleash;

    public MottatteOpplysningerService(MottatteOpplysningerRepository mottatteOpplysningerRepository,
                                       BehandlingService behandlingService,
                                       JoarkFasade joarkFasade,
                                       Unleash unleash) {
        this.mottatteOpplysningerRepository = mottatteOpplysningerRepository;
        this.behandlingService = behandlingService;
        this.joarkFasade = joarkFasade;
        this.unleash = unleash;
    }

    @Transactional(readOnly = true)
    public MottatteOpplysninger hentMottatteOpplysninger(long behandlingID) {
        return finnMottatteOpplysninger(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke mottatteOpplysninger for behandling " + behandlingID));
    }

    @Transactional(readOnly = true)
    public Optional<MottatteOpplysningerData> finnMottatteOpplysningerData(long behandlingID) {
        var mottatteOpplysninger = finnMottatteOpplysninger(behandlingID).orElse(null);
        if (mottatteOpplysninger == null) {
            return Optional.empty();
        }
        return Optional.of(mottatteOpplysninger.getMottatteOpplysningerData());
    }

    public void opprettSedGrunnlag(long behandlingID,
                                   SedGrunnlag sedGrunnlag) {
        opprettMottatteOpplysninger(behandlingID, sedGrunnlag, Mottatteopplysningertyper.SED, VERSJON_SED_GRUNNLAG);
    }

    public void opprettSøknad(Prosessinstans prosessinstans) {
        Behandling behandling = prosessinstans.getBehandling();
        Soeknadsland soeknadsland;
        Periode periode;
        if (unleash.isEnabled("melosys.tom_periode_og_land")) {
            soeknadsland = prosessinstans.getData(
                ProsessDataKey.SØKNADSLAND,
                new TypeReference<>() {
                },
                new Soeknadsland());
            periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class, new Periode());
        } else {
            soeknadsland = prosessinstans.getData(
                ProsessDataKey.SØKNADSLAND,
                new TypeReference<>() {
                });
            periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        }
        opprettSøknad(behandling, periode, soeknadsland);
    }

    public void opprettSøknad(Behandling behandling, Periode periode, Soeknadsland soeknadsland) {
        long behandlingID = behandling.getId();
        boolean behandleAlleSakerEnabled = unleash.isEnabled("melosys.behandle_alle_saker");

        // Toggle for å opprette søknad for folketrygdloven, fjernes når vi ikke lenger skal ha tom flyt for FTRL
        boolean skalOppretteSoknadForFolketrygdloven = unleash.isEnabled("melosys.folketrygden.mvp") && behandling.getFagsak().getType().equals(Sakstyper.FTRL);

        if ((behandleAlleSakerEnabled ? !SaksbehandlingRegler.harTomFlyt(behandling) : behandling.erBehandlingAvSøknadGammel())
            || skalOppretteSoknadForFolketrygdloven) {
            Sakstyper sakstype = behandling.getFagsak().getType();
            switch (sakstype) {
                case EU_EOS -> opprettSøknadYrkesaktiveEøs(behandlingID, periode, soeknadsland);
                case FTRL -> opprettSøknadFolketrygden(behandlingID);
                case TRYGDEAVTALE -> opprettSøknadTrygdeavtale(behandlingID);
            }
            log.info("Opprettet søknad for behandling {}.", behandlingID);
        } else {
            log.info("Søknad trengs ikke og opprettes ikke for behandling {} med tema {}", behandlingID,
                behandling.getTema());
        }
    }

    private void opprettSøknadYrkesaktiveEøs(long behandlingID, Periode periode, Soeknadsland soeknadsland) {
        Soeknad soeknad = new Soeknad();
        soeknad.periode = periode;
        soeknad.soeknadsland = soeknadsland;
        opprettMottatteOpplysninger(behandlingID, soeknad, Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    public void opprettSøknadUtsendteArbeidstakereEøs(long behandlingID,
                                                      String orginalData,
                                                      Soeknad soeknad,
                                                      String eksternReferanseID) {
        opprettMottatteOpplysninger(behandlingID, orginalData, soeknad,
            Mottatteopplysningertyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS, VERSJON_SOEKNAD_GRUNNLAG,
            eksternReferanseID);
    }

    private void opprettSøknadFolketrygden(long behandlingID) {
        opprettMottatteOpplysninger(behandlingID, new SoeknadFtrl(), SØKNAD_FOLKETRYGDEN,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    private void opprettSøknadTrygdeavtale(long behandlingID) {
        opprettMottatteOpplysninger(behandlingID, new SoeknadTrygdeavtale(), SØKNAD_TRYGDEAVTALE,
            VERSJON_SOEKNAD_GRUNNLAG);
    }

    private void opprettMottatteOpplysninger(long behandlingID,
                                             MottatteOpplysningerData mottatteOpplysningerData,
                                             Mottatteopplysningertyper type,
                                             String versjon) {

        opprettMottatteOpplysninger(behandlingID, null, mottatteOpplysningerData, type, versjon, null);
    }

    private void opprettMottatteOpplysninger(long behandlingID,
                                             String originalData,
                                             MottatteOpplysningerData mottatteOpplysningerData,
                                             Mottatteopplysningertyper type,
                                             String versjon,
                                             String eksternReferanseID) {

        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        if (behandling.getMottatteOpplysninger() != null) {
            throw new FunksjonellException("Finnes allerede mottatteOpplysninger for behandling " + behandling.getId());
        }
        if (eksternReferanseID != null && harMottattSøknadMedEksternReferanseID(eksternReferanseID)) {
            throw new FunksjonellException("Det finnes allerede mottatteOpplysninger med eksterReferanseID " + eksternReferanseID);
        }

        Instant nå = Instant.now();
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setBehandling(behandling);
        mottatteOpplysninger.setRegistrertDato(nå);
        mottatteOpplysninger.setEndretDato(nå);
        mottatteOpplysninger.setType(type);
        mottatteOpplysninger.setVersjon(versjon);
        mottatteOpplysninger.setMottaksdato(hentMottaksdato(type, behandling.getInitierendeJournalpostId()));
        mottatteOpplysninger.setOriginalData(originalData);
        mottatteOpplysninger.setEksternReferanseID(eksternReferanseID);
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData);
        mottatteOpplysningerRepository.save(mottatteOpplysninger);
    }

    // Gjør mottaksdato nødvendig for kun folketrygden da nåværende journalpost-oppslag kun støtter inngående
    // dokumenter og fordi mottaksdato ikke er like viktig for andre grunnlagstyper.
    private LocalDate hentMottaksdato(Mottatteopplysningertyper grunnlagtype, String journalpostID) {
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
    public MottatteOpplysninger oppdaterMottatteOpplysninger(long behandlingID, JsonNode mottatteOpplysningerDataJson) {
        MottatteOpplysninger mottatteOpplysninger = hentMottatteOpplysninger(behandlingID);
        mottatteOpplysninger.setJsonData(mottatteOpplysningerDataJson.toPrettyString());
        return mottatteOpplysningerRepository.saveAndFlush(mottatteOpplysninger);
    }

    @Transactional
    public MottatteOpplysninger oppdaterMottatteOpplysninger(MottatteOpplysninger mottatteOpplysninger) {
        MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger);
        return mottatteOpplysningerRepository.saveAndFlush(mottatteOpplysninger);
    }

    @Transactional
    public MottatteOpplysninger oppdaterMottatteOpplysningerPeriodeOgLand(long behandlingID, Periode periode, Soeknadsland soeknadsland) {
        MottatteOpplysninger mottatteOpplysninger = hentMottatteOpplysninger(behandlingID);
        mottatteOpplysninger.getMottatteOpplysningerData().periode = periode;
        mottatteOpplysninger.getMottatteOpplysningerData().soeknadsland = soeknadsland;
        MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger);
        return mottatteOpplysningerRepository.saveAndFlush(mottatteOpplysninger);
    }

    public Optional<MottatteOpplysninger> finnMottatteOpplysninger(Long behandlingID) {
        return mottatteOpplysningerRepository.findByBehandling_Id(behandlingID);
    }

    public boolean harMottattSøknadMedEksternReferanseID(String eksternReferanseID) {
        return !mottatteOpplysningerRepository.findByEksternReferanseID(eksternReferanseID).isEmpty();
    }
}
