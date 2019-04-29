package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.Behandlingsmaate;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.AvklarteFaktaRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BestemBehandlingsMaate extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(BestemBehandlingsMaate.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final AvklarteFaktaRepository avklarteFaktaRepository;

    @Autowired
    public BestemBehandlingsMaate(BehandlingsresultatRepository behandlingsresultatRepository, AvklarteFaktaRepository avklarteFaktaRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.avklarteFaktaRepository = avklarteFaktaRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(prosessinstans.getBehandling().getId())
            .orElseThrow(() -> new TekniskException("Finner ikke behandlingsresultat for behandling " + prosessinstans.getBehandling().getId()));

        Set<Avklartefakta> treffRegisterKontroll = avklarteFaktaRepository
            .findAllByBehandlingsresultatIdAndType(behandlingsresultat.getId(), Avklartefaktatype.VURDERING_UNNTAK_PERIODE);

        boolean harTreffFraRegisterkontroll = treffRegisterKontroll.stream()
            .map(Avklartefakta::getRegistreringer).anyMatch(CollectionUtils::isNotEmpty);

        if (!harTreffFraRegisterkontroll) {
            behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.AUTOMATISERT);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
        } else {
            log.info("Kan ikke registrere perioder for behandling {} automatisk. Flyttet til manuell behandling.", prosessinstans.getBehandling().getId());
            behandlingsresultat.setBehandlingsmåte(Behandlingsmaate.DELVIS_AUTOMATISERT);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_OPPGAVE);
        }

        behandlingsresultatRepository.save(behandlingsresultat);
    }
}
