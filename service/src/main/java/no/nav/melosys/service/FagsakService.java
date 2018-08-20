package no.nav.melosys.service;

import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Service
public class FagsakService {

    private static final Logger log = LoggerFactory.getLogger(FagsakService.class);

    private static final String FAGSAKID_PREFIX = "MEL-";

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    private final SaksopplysningerService saksopplysningerService;

    private final TpsFasade tpsFasade;

    private final ProsessinstansRepository prosessinstansRepository;

    private final Binge binge;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository,
                         BehandlingRepository behandlingRepository,
                         SaksopplysningerService saksopplysningerService,
                         TpsFasade tpsFasade,
                         ProsessinstansRepository prosessinstansRepository,
                         Binge binge) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.saksopplysningerService = saksopplysningerService;
        this.tpsFasade = tpsFasade;
        this.prosessinstansRepository = prosessinstansRepository;
        this.binge = binge;
    }

    public Fagsak hentFagsak(String saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    public List<Fagsak> hentFagsakerMedAktør(RolleType rolleType, String ident) throws IkkeFunnetException {
        String aktørID = tpsFasade.hentAktørIdForIdent(ident);
        return fagsakRepository.findByRolleAndAktør(rolleType, aktørID);
    }

    @Transactional
    public void lagre(Fagsak sak) {
        if (sak.getSaksnummer() == null) {
            sak.setSaksnummer(hentNesteSaksnummer());
        }
        fagsakRepository.save(sak);
    }

    /**
     * Oppretter en fagsak og en behandling ut fra et fødselsnummer.
     */
    @Transactional
    public Fagsak nyFagsakOgBehandling(String aktørID, BehandlingType behandlingType) {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(hentNesteSaksnummer());

        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId(aktørID);
        aktoer.setFagsak(fagsak);
        aktoer.setRolle(RolleType.BRUKER);

        LocalDateTime dato = LocalDateTime.now();

        fagsak.setAktører(new HashSet<>(Collections.singletonList(aktoer)));
        fagsak.setRegistrertDato(dato);
        fagsak.setEndretDato(dato);
        fagsak.setStatus(FagsakStatus.OPPRETTET);

        lagre(fagsak);

        Behandling behandling = new Behandling();
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        behandling.setFagsak(fagsak);
        behandling.setRegistrertDato(dato);
        behandling.setEndretDato(dato);

        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandling.setType(behandlingType);
        behandlingRepository.save(behandling);
        return fagsak;
    }

    // FIXME Trenger test en metode for å opprette fagsaker utenom saksflyt?
    @Deprecated
    @Transactional
    public Fagsak testFagsakOgBehandling(String ident, BehandlingType behandlingType) throws SikkerhetsbegrensningException, IkkeFunnetException {
        String aktørID = tpsFasade.hentAktørIdForIdent(ident);

        Fagsak fagsak = nyFagsakOgBehandling(aktørID, behandlingType);
        fagsak.setType(FagsakType.EU_EØS);
        Set<Saksopplysning> saksopplysninger = saksopplysningerService.hentSaksopplysninger(aktørID);
        Behandling behandling = fagsak.getAktivBehandling();
        saksopplysninger.forEach(x -> x.setBehandling(behandling));
        behandling.setSaksopplysninger(saksopplysninger);

        return fagsakRepository.save(fagsak);
    }

    public void oppfriskSaksopplysning(long behandlingsid) throws IkkeFunnetException, TekniskException {
        log.debug("Starter oppfrisking av behandlingsid {}", behandlingsid);

        List<Prosessinstans> prosessinstans = prosessinstansRepository.findByBehandling_Id(behandlingsid);
        Behandling behandling = behandlingRepository.findOne(behandlingsid);
        String aktør_Id = behandling.getFagsak().getBruker().getAktørId();

        SoeknadDokument soeknadDokument;
        Optional<SaksopplysningDokument> opt = hentDokument(behandling, SaksopplysningType.SØKNAD);
        if (opt.isPresent()) {
            soeknadDokument = (SoeknadDokument) opt.get();
        } else
            throw new TekniskException("Uventet feilsituasjon har oppstått");

        LocalDateTime nå = LocalDateTime.now();

        Optional<Prosessinstans> aktivProsessinstans = prosessinstans.stream().filter(prosessinstans1 -> prosessinstans1.getSteg() != null).findAny();

        if (!aktivProsessinstans.isPresent()) {
            behandling.getSaksopplysninger().removeIf(saksopplysning -> saksopplysning.getType() != SaksopplysningType.SØKNAD);

            Prosessinstans nyprosessinstans = new Prosessinstans();
            nyprosessinstans.setBehandling(behandling);
            nyprosessinstans.setType(ProsessType.SØKNAD_A1);
            nyprosessinstans.setData(ProsessDataKey.AKTØR_ID, aktør_Id);
            nyprosessinstans.setData(ProsessDataKey.BRUKER_ID, tpsFasade.hentIdentForAktørId(aktør_Id));

            nyprosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, hentPeriode(soeknadDokument));
            nyprosessinstans.setData(ProsessDataKey.LAND, hentLand(soeknadDokument));

            nyprosessinstans.setSteg(ProsessSteg.JFR_HENT_PERS_OPPL);
            nyprosessinstans.setData(ProsessDataKey.OPPFRISK_SAKSOPPLYSNING, true);
            nyprosessinstans.setRegistrertDato(nå);

            prosessinstansRepository.save(nyprosessinstans);
            binge.leggTil(nyprosessinstans);
        } else {
            log.info("Aktiv prosessinstans finnes allerede. Ikke mulig å oppfriske saksopplysning.");
        }
    }

    private String hentNesteSaksnummer() {
        Long sekvensVerdi = fagsakRepository.hentNesteSekvensVerdi();
        if (sekvensVerdi == null) {
            throw new RuntimeException("Henting av neste SekvensVerdi fra sekvensen feilet.");
        } else {
            return FAGSAKID_PREFIX + Long.toString(sekvensVerdi);
        }
    }
}
