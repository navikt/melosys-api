package no.nav.melosys.service.representant;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.folketrygden.ValgtRepresentant;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.avgiftoverforing.AvgiftOverforingConsumer;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDataDto;
import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import no.nav.melosys.repository.AktoerRepository;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class RepresentantService {

    private final AvgiftOverforingConsumer avgiftOverforingConsumer;
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    private final AktoerRepository aktoerRepository;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final KontaktopplysningService kontaktopplysningService;

    public RepresentantService(AvgiftOverforingConsumer avgiftOverforingConsumer, MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository, AktoerRepository aktoerRepository, BehandlingsgrunnlagService behandlingsgrunnlagService, KontaktopplysningService kontaktopplysningService) {
        this.avgiftOverforingConsumer = avgiftOverforingConsumer;
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
        this.aktoerRepository = aktoerRepository;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.kontaktopplysningService = kontaktopplysningService;
    }

    public List<AvgiftOverforingRepresentantDto> hentRepresentantListe() {
        return Arrays.asList(avgiftOverforingConsumer.hentRepresentantListe());
    }

    public AvgiftOverforingRepresentantDataDto hentRepresentant(String representantId) {
        return avgiftOverforingConsumer.hentRepresentant(representantId);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public ValgtRepresentant oppdaterValgtRepresentant(long behandlingID, ValgtRepresentant valgtRepresentant) throws FunksjonellException {
        validerValgtRepresentantRequest(valgtRepresentant);

        var medlemAvFolketrygden = medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingID));

        var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();
        var fagsak = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandling().getFagsak();
        var lagretAktoer = fastsattTrygdeavgift.getBetalesAv();

        fastsattTrygdeavgift.setRepresentantNr(valgtRepresentant.getRepresentantnummer());
        fastsattTrygdeavgift.setBetalesAv(valgtRepresentant.isSelvbetalende() ? null : oppdaterAktoer(lagretAktoer, fagsak, valgtRepresentant.getOrgnr()));

        if (valgtRepresentant.isSelvbetalende() && lagretAktoer != null) {
            fagsak.getAktører().remove(lagretAktoer);
            aktoerRepository.deleteById(lagretAktoer.getId());
        }
        if (valgtRepresentant.getKontaktperson() != null) {
            kontaktopplysningService.lagEllerOppdaterKontaktopplysning(fagsak.getSaksnummer(), valgtRepresentant.getOrgnr(), null, valgtRepresentant.getKontaktperson(), null);
        }

        medlemAvFolketrygdenRepository.save(medlemAvFolketrygden);
        return valgtRepresentant;
    }

    private void validerValgtRepresentantRequest(ValgtRepresentant valgtRepresentant) throws FunksjonellException {
        if (valgtRepresentant.getRepresentantnummer() == null || valgtRepresentant.getRepresentantnummer().isEmpty()) {
            throw new FunksjonellException("Representantnummer må være utfylt");
        }
        if (!valgtRepresentant.isSelvbetalende() && (valgtRepresentant.getOrgnr() == null || valgtRepresentant.getOrgnr().isEmpty())) {
            throw new FunksjonellException("Når representant ikke er selvbetalende, må organisasjonsnummer være utfylt");
        }
    }

    private Aktoer oppdaterAktoer (Aktoer lagretAktoer, Fagsak fagsak, String orgnr) {
        var nyAktoer = lagretAktoer != null ? lagretAktoer : new Aktoer();
        nyAktoer.setFagsak(fagsak);
        nyAktoer.setOrgnr(orgnr);
        nyAktoer.setRolle(Aktoersroller.REPRESENTANT_TRYGDEAVGIFT);
        return aktoerRepository.save(nyAktoer);
    }

    public ValgtRepresentant hentValgtRepresentant(long behandlingID) throws IkkeFunnetException {
        var medlemAvFolketrygden = medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingID));

        var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();

        if (fastsattTrygdeavgift.getBetalesAv() == null) {
            return new ValgtRepresentant(fastsattTrygdeavgift.getRepresentantNr(), true, null, null);
        }

        var kontaktopplysninger = kontaktopplysningService.hentKontaktopplysning(
            fastsattTrygdeavgift.getBetalesAv().getFagsak().getSaksnummer(),
            fastsattTrygdeavgift.getBetalesAv().getOrgnr()).orElse(new Kontaktopplysning());

        return new ValgtRepresentant(fastsattTrygdeavgift.getRepresentantNr(), false, fastsattTrygdeavgift.getBetalesAv().getOrgnr(), kontaktopplysninger.getKontaktNavn());
    }
}
