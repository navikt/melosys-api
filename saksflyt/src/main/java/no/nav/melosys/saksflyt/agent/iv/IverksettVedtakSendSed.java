package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.SedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IverksettVedtakSendSed extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakSendSed.class);

    private final BehandlingRepository behandlingRepository;
    private final SedService sedService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public IverksettVedtakSendSed(BehandlingRepository behandlingRepository, SedService sedService, BehandlingsresultatService behandlingsresultatService) {
        this.behandlingRepository = behandlingRepository;
        this.sedService = sedService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_SEND_SED;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException {

        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException(String.format("Finner ikke behandlingen %s.", prosessinstans.getBehandling().getId()));
        }

        try {
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            if (sedSkalSendes(behandlingsresultat.getType(), validerLovvalgsperiode(behandlingsresultat.getLovvalgsperioder()))) {
                sedService.opprettOgSendSed(behandling, behandlingsresultat);
            }
            prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
        } catch (MelosysException ex) {
            log.error("Kan ikke opprette og sende sed for behandling {}", behandling.getId(), ex);
            prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        }
    }

    private Lovvalgsperiode validerLovvalgsperiode(Set<Lovvalgsperiode> lovvalgsperioder) throws FunksjonellException {
        if (lovvalgsperioder.size() == 0) {
            throw new FunksjonellException("Lovvalgsperiode mangler");
        }

        if (lovvalgsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en"
                + " lovvalgsperiode er ikke støttet i første leveranse");
        }

        return lovvalgsperioder.iterator().next();
    }

    /**
     * Finn ut om SED skal sendes.
     * <p>
     * Innvilgelsesbrev skal sendes dersom behandlingen har resultert i:
     * <ul>
     * <li>Lovvalgsland er avklart</li>
     * <li>Innvilget lovvalgsland er Norge</li>
     * <li>Lovvalgbestemmelsen er 12.1 eller 12.2</li>
     */
    private boolean sedSkalSendes(BehandlingsresultatType behandlingsresultatType, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultatType == BehandlingsresultatType.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.getLovvalgsland() == Landkoder.NO
            && erGyldigBestemmelse(lovvalgsperiode.getBestemmelse());
    }

    private static boolean erGyldigBestemmelse(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1
            || bestemmelse == LovvalgBestemmelse_883_2004.FO_883_2004_ART12_2;
    }
}
