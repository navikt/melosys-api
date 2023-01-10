package no.nav.melosys.service.medl;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl;
import no.nav.melosys.repository.AnmodningsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.ANMODNING_OM_UNNTAK;

@Service
public class MedlAnmodningsperiodeService {
    private static final Logger log = LoggerFactory.getLogger(MedlAnmodningsperiodeService.class);
    private final MedlService medlService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final AnmodningsperiodeRepository anmodningsperiodeRepository;

    public MedlAnmodningsperiodeService(MedlService medlService,
                                        BehandlingsresultatService behandlingsresultatService,
                                        AnmodningsperiodeRepository anmodningsperiodeRepository) {
        this.medlService = medlService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.anmodningsperiodeRepository = anmodningsperiodeRepository;
    }

    public void lagreAnmodningsperiode(Anmodningsperiode anmodningsperiode) {
        anmodningsperiodeRepository.save(anmodningsperiode);
    }

    public void avsluttTidligereAnmodningsperiode(Behandling behandling) {
        Anmodningsperiode forrigeAnmodningsPeriode = finnAnmodningsperiodeForForrigeA001(behandling);
        Long medlPeriodeID = forrigeAnmodningsPeriode.getMedlPeriodeID();
        log.info("Avslutter tidligere anmodningsperiode med MedlPeriodeID: {}", medlPeriodeID);
        medlService.avvisPeriode(medlPeriodeID, StatusaarsakMedl.AVVIST);
    }

    public void avsluttTidligereSendtAnmodningPeriode(Behandling nyBehandling) {
        Fagsak fagsak = nyBehandling.getFagsak();
        Behandling forrigeBehandling = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato().stream()
            .filter(behandling -> !behandling.getId().equals(nyBehandling.getId()))
            .filter(behandling -> {
                Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
                return ANMODNING_OM_UNNTAK == behandlingsresultat.getType();
            })
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Fant ikke tidligere periode på en ny vurdering med saksnummer: %s"
                .formatted(fagsak.getSaksnummer()))
            );

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(forrigeBehandling.getId());
        Long medlPeriodeID = behandlingsresultat.hentAnmodningsperiode().getMedlPeriodeID();
        log.info("Avslutter tidligere anmodningsperiode med MedlPeriodeID: {}", medlPeriodeID);
        medlService.avvisPeriode(medlPeriodeID, StatusaarsakMedl.AVVIST);
    }

    private Anmodningsperiode finnAnmodningsperiodeForForrigeA001(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var a001Behandling = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato().stream()
            .filter(Behandling::erAnmodningOmUnntak)
            .filter(beh -> !beh.getId().equals(behandling.getId()))
            .findFirst()
            .orElseThrow(() -> new FunksjonellException("Fant ikke tidligere periode på en oppdatert sed med saksnummer: %s"
                .formatted(fagsak.getSaksnummer()))
            );
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(a001Behandling.getId());
        return behandlingsresultat.hentAnmodningsperiode();
    }
}
