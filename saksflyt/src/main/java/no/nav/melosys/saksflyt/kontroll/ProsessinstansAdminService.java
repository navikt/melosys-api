package no.nav.melosys.saksflyt.kontroll;

import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.ProsessinstansBehandlerDelegate;
import no.nav.melosys.saksflyt.ProsessinstansRepository;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import no.nav.melosys.saksflytapi.domain.*;
import org.springframework.stereotype.Service;

import static java.time.LocalDateTime.now;

@Service
public class ProsessinstansAdminService {

    public static final int ANTALL_TIMER_FØR_RESTART = 24;
    private final ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate;
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansAdminService(ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate, ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansBehandlerDelegate = prosessinstansBehandlerDelegate;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    public HentProsessinstansDto hentProsessinstansDto(UUID uuid) {
        Prosessinstans prosessinstans = prosessinstansRepository.findById(uuid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke prosessinstans med ID %s".formatted(uuid)));
        return mapTilHentProsessinstansDto(prosessinstans);
    }

    public List<HentProsessinstansDto> hentFeiledeProsessinstanser() {
        return prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET).stream()
            .map(this::mapTilHentProsessinstansDto)
            .sorted(Comparator.comparing(HentProsessinstansDto::registrertDato))
            .toList();
    }

    public List<HentProsessinstansDto> hentFastlåsteProsessinstanser() {
        return prosessinstansRepository.findAllByStatusIn(ProsessStatus.hentAktiveStatuser()).stream()
            .filter(prosessinstans -> prosessinstans.getEndretDato().isBefore(now().minusHours(ANTALL_TIMER_FØR_RESTART)))
            .map(this::mapTilHentProsessinstansDto)
            .sorted(Comparator.comparing(HentProsessinstansDto::endretDato))
            .toList();
    }

    public List<HentProsessinstansDto> restartAlleFeiledeProsessinstanser() {
        Collection<Prosessinstans> prosessinstanser = prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET);

        setStatusRestartet(prosessinstanser);
        behandle(prosessinstanser);

        return prosessinstanser.stream().map(this::mapTilHentProsessinstansDto).toList();
    }

    public void restartProsessinstanser(Iterable<UUID> uuids) {
        Collection<Prosessinstans> prosessinstanser = prosessinstansRepository.findAllById(uuids);

        for (var prosessinstans : prosessinstanser) {
            if (prosessinstans.getStatus() == ProsessStatus.FERDIG) {
                throw new FunksjonellException(
                    "Prosessinstans %s har status %s".formatted(prosessinstans.getId(), prosessinstans.getStatus()));
            }
            if (ProsessStatus.hentAktiveStatuser().contains(
                prosessinstans.getStatus()) && prosessinstans.getRegistrertDato().isAfter(
                now().minusHours(ANTALL_TIMER_FØR_RESTART))) {
                throw new FunksjonellException(
                    "Prosessinstans %s er registrert %s, for mindre enn %s timer siden".formatted(
                        prosessinstans.getId(), prosessinstans.getRegistrertDato(), ANTALL_TIMER_FØR_RESTART));
            }
        }

        setStatusRestartet(prosessinstanser);
        behandle(prosessinstanser);
    }

    public ProsessSteg hoppOverStegProsessinstans(UUID uuid) {
        var prosessinstans = prosessinstansRepository.findById(uuid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke prosessinstans med ID %s".formatted(uuid)));
        var nesteSteg = ProsessflytDefinisjon.hentNesteSteg(prosessinstans.getType(), prosessinstans.getSistFullførtSteg())
            .orElseThrow(() -> new TekniskException("Fant ikke neste steg for prosessinstans med ID %s".formatted(uuid)));

        prosessinstans.setSistFullførtSteg(nesteSteg);
        prosessinstansRepository.save(prosessinstans);

        return nesteSteg;
    }

    public void ferdigstillProsessinstans(UUID uuid) {
        var prosessinstans = prosessinstansRepository.findById(uuid)
            .orElseThrow(() -> new IkkeFunnetException("Fant ikke prosessinstans med ID %s".formatted(uuid)));
        prosessinstans.setStatus(ProsessStatus.FERDIG);
        prosessinstans.setEndretDato(now());

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
            prosessinstans.getRegistrertDato(),
            ProsessflytDefinisjon.hentNesteSteg(prosessinstans.getType(), prosessinstans.getSistFullførtSteg())
                .map(ProsessSteg::getKode)
                .orElse(null),
            prosessinstans.getHendelser()
                .stream()
                .max(Comparator.comparing(ProsessinstansHendelse::getDato))
                .map(ProsessinstansHendelse::getMelding)
                .orElse(null),
            prosessinstans.getStatus(),
            prosessinstans.getData(ProsessDataKey.CORRELATION_ID_SAKSFLYT));
    }

    private void setStatusRestartet(Collection<Prosessinstans> prosessinstanser) {
        prosessinstanser.forEach(prosessinstans -> prosessinstans.setStatus(ProsessStatus.RESTARTET));
        LocalDateTime nå = now();
        prosessinstanser.forEach(prosessinstans -> prosessinstans.setEndretDato(nå));
        prosessinstansRepository.saveAll(prosessinstanser);
    }

    private void behandle(Collection<Prosessinstans> prosessinstanser) {
        prosessinstanser.stream()
            .sorted(Comparator.comparing(Prosessinstans::getRegistrertDato))
            .forEach(prosessinstansBehandlerDelegate::behandleProsessinstans);
    }
}
