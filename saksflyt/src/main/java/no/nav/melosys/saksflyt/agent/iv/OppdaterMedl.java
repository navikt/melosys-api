package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
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

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;
import static no.nav.melosys.domain.ProsessSteg.IV_SEND_BREV;

/**
 * Oppdaterer medlemskap periode i MEDL.
 *
 * Transisjoner:
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_OPPDATER_MEDL -> IV_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterMedl extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMedl.class);

    private final MedlFasade medlFasade;
    private final TpsFasade tpsFasade;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;


    @Autowired
    public OppdaterMedl(MedlFasade medlFasade,
                        TpsFasade tpsFasade,
                        BehandlingsresultatRepository behandlingsresultatRepository,
                        LovvalgsperiodeRepository lovvalgsperiodeRepository) {

        log.info("IverksetteVedtakOppdaterMEDL initialisert");
        this.medlFasade = medlFasade;
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_OPPDATER_MEDL;
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

        if (behandlingsresultat == null ) {
            throw new IkkeFunnetException("Opprettelse av periode i MEDL feilet fordi behandlingsresultat med behandling ID " + behandling.getId() + " ikke finnes.");
        }

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.isEmpty()) {
            throw new FunksjonellException("Lovvalgsperiode mangler for behandling " + behandling.getId());
        }

        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();

        Long medlPeriodeID;
        if (erPeriodeEndelig(behandlingsresultat, lovvalgsperiode)) {
            medlPeriodeID = medlFasade.opprettPeriodeEndelig(fnr, lovvalgsperiode);
        } else if (erPeriodeUnderAvklaring(behandlingsresultat)) {
            medlPeriodeID = medlFasade.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode);
        } else {
            throw new FunksjonellException("Opprettelse av Periode i MEDL støttes ikke for behandlingsresultat type "
                + behandlingsresultat.getType() + " og InnvilgelsesResultat type" + lovvalgsperiode.getInnvilgelsesresultat().getKode());
        }

        lagreMedlPeriodeId(medlPeriodeID, lovvalgsperiode, behandling.getId());

        prosessinstans.setSteg(IV_SEND_BREV);
    }

    public boolean erPeriodeEndelig(Behandlingsresultat behandlingsresultat, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultat.getType() == BehandlingsresultatType.FASTSATT_LOVVALGSLAND && lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET;
    }

    public boolean erPeriodeUnderAvklaring(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getType() == BehandlingsresultatType.ANMODNING_OM_UNNTAK;
    }

    private void lagreMedlPeriodeId(Long medlPeriodeID, Lovvalgsperiode lovvalgsperiode, long behandlingID) throws FunksjonellException {
        if (medlPeriodeID == null) {
            throw new FunksjonellException("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID fra MEDL tjeneste for behandling " + behandlingID);
        }
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiodeRepository.save(lovvalgsperiode);
    }
}
