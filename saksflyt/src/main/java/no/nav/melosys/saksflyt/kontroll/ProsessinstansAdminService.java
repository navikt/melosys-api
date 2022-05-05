package no.nav.melosys.saksflyt.kontroll;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansHendelse;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.BehandleProsessinstansDelegate;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import org.springframework.stereotype.Service;

@Service
public class ProsessinstansAdminService {

    private final BehandleProsessinstansDelegate behandleProsessinstansDelegate;
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansAdminService(BehandleProsessinstansDelegate behandleProsessinstansDelegate, ProsessinstansRepository prosessinstansRepository) {
        this.behandleProsessinstansDelegate = behandleProsessinstansDelegate;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    public List<HentProsessinstansDto> hentFeiledeProsessinstanser() {
        return prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET).stream()
            .map(this::mapTilHentProsessinstansDto)
            .sorted(Comparator.comparing(HentProsessinstansDto::endretDato))
            .collect(Collectors.toList());
    }

    public List<HentProsessinstansDto> restartAlleFeiledeProsessinstanser() {
        Collection<Prosessinstans> prosessinstanser = prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET);

        setStatusRestartet(prosessinstanser);

        return prosessinstanser.stream().map(this::mapTilHentProsessinstansDto).collect(Collectors.toList());
    }

    public void restartProsessinstanser(Iterable<UUID> uuids) {
        Collection<Prosessinstans> prosessinstanser = prosessinstansRepository.findAllById(uuids);

        for (var prosessinstans : prosessinstanser) {
            if (prosessinstans.getStatus() != ProsessStatus.FEILET) {
                throw new FunksjonellException("Prosessinstans " + prosessinstans.getId() + " har status " + prosessinstans.getStatus());
            }
        }

        setStatusRestartet(prosessinstanser);
    }

    public ProsessSteg hoppOverStegProsessinstans(UUID uuid) {
        var prosessinstans = prosessinstansRepository.findById(uuid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke prosessinstans med ID %s".formatted(uuid)));
        var nesteSteg = hentNesteSteg(prosessinstans)
            .orElseThrow(() -> new TekniskException("Fant ikke neste steg for prosessinstans med ID %s".formatted(uuid)));

        prosessinstans.setSistFullførtSteg(nesteSteg);
        prosessinstansRepository.save(prosessinstans);

        return nesteSteg;
    }

    public void ferdigstillProsessinstans(UUID uuid) {
        var prosessinstans = prosessinstansRepository.findById(uuid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke prosessinstans med ID %s".formatted(uuid)));
        prosessinstans.setStatus(ProsessStatus.FERDIG);

        prosessinstansRepository.save(prosessinstans);
    }

    private HentProsessinstansDto mapTilHentProsessinstansDto(Prosessinstans prosessinstans) {
        Long behandlingID = Optional.ofNullable(prosessinstans.getBehandling())
            .map(Behandling::getId)
            .orElse(null);
        String saksnummer = Optional.ofNullable(prosessinstans.getBehandling())
            .map(behandling -> behandling.getFagsak().getSaksnummer())
            .orElse(null);

        return new HentProsessinstansDto(
            prosessinstans.getId(),
            behandlingID,
            saksnummer,
            prosessinstans.getType().getKode(),
            prosessinstans.getEndretDato(),
            hentNesteSteg(prosessinstans)
                .map(ProsessSteg::getKode)
                .orElse(null),
            prosessinstans.getHendelser()
                .stream()
                .max(Comparator.comparing(ProsessinstansHendelse::getDato))
                .map(ProsessinstansHendelse::getMelding)
                .orElse(null));
    }

    private Optional<ProsessSteg> hentNesteSteg(Prosessinstans prosessinstans) {
        return ProsessflytDefinisjon.finnFlytForProsessType(prosessinstans.getType())
            .map(it -> it.nesteSteg(prosessinstans.getSistFullførtSteg()));
    }

    private void setStatusRestartet(Collection<Prosessinstans> prosessinstanser) {
        prosessinstanser.forEach(p -> p.setStatus(ProsessStatus.RESTARTET));

        prosessinstansRepository
            .saveAll(prosessinstanser)
            .stream()
            .sorted(Comparator.comparing(Prosessinstans::getRegistrertDato))
            .forEach(behandleProsessinstansDelegate::behandleProsessinstans);
    }
}
