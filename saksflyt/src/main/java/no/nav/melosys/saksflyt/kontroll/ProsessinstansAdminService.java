package no.nav.melosys.saksflyt.kontroll;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.BehandleProsessinstansDelegate;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
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
        return new HentProsessinstansDto(prosessinstans);
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
