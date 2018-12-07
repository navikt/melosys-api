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


    @Autowired
    public OppdaterMedl(MedlFasade medlFasade,
                        TpsFasade tpsFasade,
                        BehandlingsresultatRepository behandlingsresultatRepository) {

        log.info("IverksetteVedtakOppdaterMEDL initialisert");
        this.medlFasade = medlFasade;
        this.tpsFasade = tpsFasade;
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
        String fnr = tpsFasade.hentIdentForAktørId(bruker.getAktørId());

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findOne(behandling.getId());

        if (behandlingsresultat == null ) {
            throw new IkkeFunnetException("Opprettelse av periode i MEDL feilet fordi behandlingsresultat med behandling ID " + behandling.getId() + " finnes ikke.");
        }

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.isEmpty()) {
            throw new FunksjonellException("Lovvalgsperiode mangler for behandling " + behandling.getId());
        }

        Lovvalgsperiode lovvalgsperiode = lovvalgsperioder.iterator().next();

        if (erPeriodeEndelig(behandlingsresultat, lovvalgsperiode)) {
            medlFasade.opprettPeriodeEndelig(fnr, lovvalgsperiode);
        } else if (erPeriodeUnderAvklaring(behandlingsresultat)) {
            medlFasade.opprettPeriodeUnderAvklaring(fnr, lovvalgsperiode);
        }
       prosessinstans.setSteg(IV_SEND_BREV);
    }

    public boolean erPeriodeEndelig(Behandlingsresultat behandlingsresultat, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultat.getType() == BehandlingsresultatType.FASTSATT_LOVVALGSLAND && lovvalgsperiode.getInnvilgelsesresultat() == InnvilgelsesResultat.INNVILGET;
    }

    public boolean erPeriodeUnderAvklaring(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getType() == BehandlingsresultatType.ANMODNING_OM_UNNTAK;
    }
}
