package no.nav.melosys.saksflyt.kontroll;

import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansHendelse;
import no.nav.melosys.exception.FunksjonellException;
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
            hentFeiletSteg(prosessinstans),
            prosessinstans.getHendelser()
                .stream()
                .max(Comparator.comparing(ProsessinstansHendelse::getDato))
                .map(ProsessinstansHendelse::getMelding)
                .orElse(null));
    }

    private String hentFeiletSteg(Prosessinstans prosessinstans) {
        var sisteFullførtSteg = Optional.ofNullable(prosessinstans.getSistFullførtSteg())
            .orElse(null);

        return ProsessflytDefinisjon.finnFlytForProsessType(prosessinstans.getType())
            .map(it -> it.nesteSteg(sisteFullførtSteg))
            .map(ProsessSteg::getKode)
            .orElse(null);
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
