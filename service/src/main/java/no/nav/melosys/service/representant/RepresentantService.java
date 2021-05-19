package no.nav.melosys.service.representant;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.avgiftoverforing.AvgiftOverforingConsumer;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.representant.dto.RepresentantDataDto;
import no.nav.melosys.service.representant.dto.RepresentantDto;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepresentantService {
    private final AvgiftOverforingConsumer avgiftOverforingConsumer;
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    private final AktoerRepository aktoerRepository;
    private final BehandlingService behandlingService;
    private final KontaktopplysningService kontaktopplysningService;

    public RepresentantService(AvgiftOverforingConsumer avgiftOverforingConsumer, MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository, AktoerRepository aktoerRepository, BehandlingService behandlingService, KontaktopplysningService kontaktopplysningService) {
        this.avgiftOverforingConsumer = avgiftOverforingConsumer;
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
        this.aktoerRepository = aktoerRepository;
        this.behandlingService = behandlingService;
        this.kontaktopplysningService = kontaktopplysningService;
    }

    public List<RepresentantDto> hentRepresentantListe() {
        return avgiftOverforingConsumer.hentRepresentantListe().stream()
            .map(RepresentantDto::av)
            .collect(Collectors.toList());
    }

    public RepresentantDataDto hentRepresentant(String representantId) {
        return RepresentantDataDto.av(avgiftOverforingConsumer.hentRepresentant(representantId));
    }

    @Transactional
    public ValgtRepresentant oppdaterValgtRepresentant(long behandlingID, ValgtRepresentant valgtRepresentant) {
        validerValgtRepresentantRequest(valgtRepresentant);

        var medlemAvFolketrygden = medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingID));

        var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();
        var fagsak = behandlingService.hentBehandling(behandlingID).getFagsak();

        fastsattTrygdeavgift.setRepresentantNr(valgtRepresentant.getRepresentantnummer());
        fastsattTrygdeavgift.setBetalesAv(oppdaterEllerOpprettAktoer(fastsattTrygdeavgift.getBetalesAv(), valgtRepresentant.isSelvbetalende(), fagsak, valgtRepresentant.getOrgnr()));

        if (valgtRepresentant.getKontaktperson() != null) {
            kontaktopplysningService.lagEllerOppdaterKontaktopplysning(fagsak.getSaksnummer(), valgtRepresentant.getOrgnr(), null, valgtRepresentant.getKontaktperson(), null);
        }

        medlemAvFolketrygdenRepository.save(medlemAvFolketrygden);
        return valgtRepresentant;
    }

    private void validerValgtRepresentantRequest(ValgtRepresentant valgtRepresentant) {
        if (StringUtils.isEmpty(valgtRepresentant.getRepresentantnummer())) {
            throw new FunksjonellException("Representantnummer må være utfylt");
        }
        if (!valgtRepresentant.isSelvbetalende() && StringUtils.isEmpty(valgtRepresentant.getOrgnr())) {
            throw new FunksjonellException("Når representant ikke er selvbetalende, må organisasjonsnummer være utfylt");
        }
    }

    private Aktoer oppdaterEllerOpprettAktoer(Aktoer gammelAktoer, boolean selvbetalende, Fagsak fagsak, String orgnr) {
        Aktoersroller rolle = selvbetalende ? Aktoersroller.BRUKER : Aktoersroller.REPRESENTANT_TRYGDEAVGIFT;
        Aktoer oppdatertAktoer;

        if (gammelAktoer != null && gammelAktoer.getRolle() == rolle) {
            oppdatertAktoer = gammelAktoer;
            oppdatertAktoer.setOrgnr(selvbetalende ? null : orgnr);
        } else if (rolle == Aktoersroller.BRUKER) {
            slettLagretAktoer(gammelAktoer, fagsak);
            oppdatertAktoer = fagsak.hentBruker();
        } else {
            oppdatertAktoer = new Aktoer();
            oppdatertAktoer.setRolle(rolle);
            oppdatertAktoer.setOrgnr(orgnr);
        }

        oppdatertAktoer.setFagsak(fagsak);

        return aktoerRepository.save(oppdatertAktoer);
    }

    private void slettLagretAktoer(Aktoer aktoer, Fagsak fagsak) {
        if (aktoer != null) {
            fagsak.getAktører().remove(aktoer);
            aktoerRepository.deleteById(aktoer.getId());
        }
    }

    public ValgtRepresentant hentValgtRepresentant(long behandlingID) {
        var medlemAvFolketrygden = medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingID));

        var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();

        if (fastsattTrygdeavgift.getBetalesAv() == null || fastsattTrygdeavgift.getBetalesAv().getRolle() == Aktoersroller.BRUKER) {
            return new ValgtRepresentant(fastsattTrygdeavgift.getRepresentantNr(), true, null, null);
        }

        var kontaktopplysninger = kontaktopplysningService.hentKontaktopplysning(
            fastsattTrygdeavgift.getBetalesAv().getFagsak().getSaksnummer(),
            fastsattTrygdeavgift.getBetalesAv().getOrgnr()).orElse(new Kontaktopplysning());

        return new ValgtRepresentant(fastsattTrygdeavgift.getRepresentantNr(), false, fastsattTrygdeavgift.getBetalesAv().getOrgnr(), kontaktopplysninger.getKontaktNavn());
    }
}
