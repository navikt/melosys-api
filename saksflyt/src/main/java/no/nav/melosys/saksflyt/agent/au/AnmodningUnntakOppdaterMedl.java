package no.nav.melosys.saksflyt.agent.au;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.AU_OPPDATER_MEDL;
import static no.nav.melosys.domain.ProsessSteg.AU_SEND_BREV;

/**
 * Oppdaterer medlemskap periode i MEDL.
 *
 * Transisjoner:
 * ProsessType.ANMODNING_UNNTAK
 *  AU_OPPDATER_MEDL -> AU_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class AnmodningUnntakOppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakOppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final TpsFasade tpsFasade;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;


    @Autowired
    public AnmodningUnntakOppdaterMedl(MedlFasade medlFasade,
                                       TpsFasade tpsFasade,
                                       BehandlingsresultatRepository behandlingsresultatRepository,
                                       LovvalgsperiodeRepository lovvalgsperiodeRepository) {

        log.info("AnmodningUnntakOppdaterMEDL initialisert");
        this.medlFasade = medlFasade;
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AU_OPPDATER_MEDL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        String fnr = tpsFasade.hentIdentForAktørId(bruker.getAktørId());

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findOne(behandling.getId());

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.size() != 1) {
            throw new FunksjonellException("Det er enten ingen eller for mange Lovvalgsperioder for behandling " + behandling.getId());
        }

        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();
        Long medlPeriodeID = medlFasade.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode);
        lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandling.getId());

        prosessinstans.setSteg(AU_SEND_BREV);
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling " + behandlingID);
        }
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }
}
