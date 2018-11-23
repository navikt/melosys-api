package no.nav.melosys.service;

import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Service
public class SaksopplysningerService {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningerService.class);

    // FIXME : Injektere feltene i constructor MELOSYS-1635
    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}")
    private Integer arbeidsforholdhistorikkAntallMåneder;

    @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}")
    private Integer inntektshistorikkAntallMåneder;

    @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}")
    private Integer medlemskaphistorikkAntallÅr;

    private final TpsFasade tpsFasade;

    private final AaregFasade aaregFasade;

    private final ProsessinstansRepository prosessinstansRepository;

    private final Binge binge;

    private final BehandlingRepository behandlingRepository;

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public SaksopplysningerService(TpsFasade tpsFasade,
                                   AaregFasade aaregFasade,
                                   ProsessinstansRepository prosessinstansRepository,
                                   Binge binge,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingsresultatService behandlingsresultatService) {
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.prosessinstansRepository = prosessinstansRepository;
        this.binge = binge;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatService = behandlingsresultatService;

    }

    public ArbeidsforholdDokument hentArbeidsforholdHistorikk(Long arbeidsforholdsID) throws SikkerhetsbegrensningException, IntegrasjonException, IkkeFunnetException {
        Saksopplysning saksopplysning = aaregFasade.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        return (ArbeidsforholdDokument) saksopplysning.getDokument();
    }

    @Transactional
    public void oppfriskSaksopplysning(long behandlingsid) throws IkkeFunnetException, TekniskException {
        log.info("Starter oppfrisking av behandlingsid: {} ", behandlingsid);

        Optional<Prosessinstans> aktivProsessinstans = prosessinstansRepository.findByStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessSteg.FEILET_MASKINELT, behandlingsid);
        if (aktivProsessinstans.isPresent()) {
            log.warn("Aktiv prosessinstans finnes allerede. Ikke mulig å oppfriske saksopplysning.");
            return;
        }

        Behandling behandling = behandlingRepository.findOneWithSaksopplysningerById(behandlingsid);
        if (behandling == null) {
            log.error("Behandling ikke funnet med behandlingsid {}", behandlingsid);
            throw new IkkeFunnetException("Behandling ikke funnet med behandlingsid: " + behandlingsid);
        }

        SoeknadDokument søknadDokument;
        Optional<SaksopplysningDokument> opt = hentDokument(behandling, SaksopplysningType.SØKNAD);
        if (opt.isPresent()) {
            søknadDokument = (SoeknadDokument) opt.get();
        } else {
            throw new TekniskException("Oppfriskning feilet på grunn av manglende søknad opplysning");
        }

        behandling.getSaksopplysninger().removeIf(saksopplysning -> saksopplysning.getType() != SaksopplysningType.SØKNAD);
        behandlingRepository.save(behandling);

        behandlingsresultatService.tømBehandlingsresultat(behandlingsid);

        opprettOppfriskningsprosess(behandling, søknadDokument);
    }

    private void opprettOppfriskningsprosess(Behandling behandling, SoeknadDokument søknadDokument) throws TekniskException, IkkeFunnetException {
        Prosessinstans nyprosessinstans = new Prosessinstans();
        nyprosessinstans.setBehandling(behandling);
        nyprosessinstans.setType(ProsessType.OPPFRISKNING);

        String aktør_Id = behandling.getFagsak().hentAktørMedRolleType(RolleType.BRUKER).getAktørId();
        nyprosessinstans.setData(ProsessDataKey.AKTØR_ID, aktør_Id);
        nyprosessinstans.setData(ProsessDataKey.BRUKER_ID, tpsFasade.hentIdentForAktørId(aktør_Id));

        nyprosessinstans.setData(ProsessDataKey.SØKNADSPERIODE, hentPeriode(søknadDokument));
        nyprosessinstans.setData(ProsessDataKey.OPPHOLDSLAND, hentLand(søknadDokument));

        nyprosessinstans.setSteg(ProsessSteg.JFR_HENT_PERS_OPPL);

        LocalDateTime nå = LocalDateTime.now();
        nyprosessinstans.setRegistrertDato(nå);
        nyprosessinstans.setEndretDato(nå);

        prosessinstansRepository.save(nyprosessinstans);
        binge.leggTil(nyprosessinstans);
    }
}
