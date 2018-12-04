package no.nav.melosys.saksflyt.agent.iv;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.LovvalgMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
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
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;


    @Autowired
    public OppdaterMedl(MedlFasade medlFasade,
                        TpsFasade tpsFasade,
                        LovvalgsperiodeRepository lovvalgsperiodeRepository,
                        BehandlingsresultatRepository behandlingsresultatRepository) {

        log.info("IverksetteVedtakOppdaterMEDL initialisert");
        this.medlFasade = medlFasade;
        this.tpsFasade = tpsFasade;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
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
        String aktørID = bruker.getAktørId();
        String fnr = tpsFasade.hentIdentForAktørId(aktørID);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findOne(behandling.getId());

        if (behandlingsresultat == null ) {
            throw new IkkeFunnetException("Opprettelse av MEDL Periode feilet fordi behandlingsresulat med ID " + behandling.getId() + " er ikke funnet.");
        }

        List<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandling.getId());
        if (lovvalgsperioder.isEmpty()) {
            throw new FunksjonellException("Lovvalgsperiode mangler for behandling " + behandling.getId());
        }
        // FIXME: Støtte for flere perioder må legges til.
        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.get(0);

        PeriodestatusMedl periodestatusMedl = null;
        LovvalgMedl lovvalgMedl = null;

        if (behandlingsresultat.getType() == BehandlingsresultatType.FASTSATT_LOVVALGSLAND && lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET) {
            //lagre periode som 'Endelig'
            periodestatusMedl = PeriodestatusMedl.GYLD;
            lovvalgMedl = LovvalgMedl.ENDL;
        } else if (behandlingsresultat.getType() == BehandlingsresultatType.ANMODNING_OM_UNNTAK ) {
            //lagre periode som 'Under avklaring'
            periodestatusMedl = PeriodestatusMedl.UAVK;
            lovvalgMedl = LovvalgMedl.UAVK;
        }

        medlFasade.opprettPeriode(fnr, lovvalgsperiode, periodestatusMedl, lovvalgMedl);
        prosessinstans.setSteg(IV_SEND_BREV);
    }
}
