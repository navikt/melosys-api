package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.util.LandkoderUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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


    @Autowired
    public OppdaterMedl(MedlFasade medlFasade, TpsFasade tpsFasade, LovvalgsperiodeRepository lovvalgsperiodeRepository) {
        log.info("IverksetteVedtakOppdaterMEDL initialisert");
        this.medlFasade = medlFasade;
        this.tpsFasade = tpsFasade;
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
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentAktørMedRolleType(RolleType.BRUKER);
        String aktørID = bruker.getAktørId();
        String fnr = tpsFasade.hentIdentForAktørId(aktørID);

        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandling.getId());

        Medlemsperiode medlemsperiode = new Medlemsperiode();

        medlemsperiode.grunnlagstype = lovvalgsperiode.hentFellesKodeForGrunnlagMedltype();
        medlemsperiode.land = LandkoderUtils.tilIso3(lovvalgsperiode.getLovvalgsland().getKode());
        if (lovvalgsperiode.hentFellesKodeForTrygdDekningtype() != null) {
            medlemsperiode.trygdedekning = lovvalgsperiode.hentFellesKodeForTrygdDekningtype();
        }
        medlFasade.opprettPeriode(fnr, medlemsperiode);

        prosessinstans.setSteg(IV_SEND_BREV);
    }
}
