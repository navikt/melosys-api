package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BEHANDLINGSRESULTATTYPE;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.ProsessSteg.IV_VALIDERING;
import static no.nav.melosys.feil.Feilkategori.FUNKSJONELL_FEIL;
import static no.nav.melosys.feil.Feilkategori.TEKNISK_FEIL;

/**
 * Validerer opplysning bli brukt for iverksett vedtak.
 *
 * Transisjoner:
 *
 * ProsessType.IVERKSETT_VEDTAK
 *  IV_VALIDERING -> IV_OPPDATER_RESULTAT eller FEILET_MASKINELT hvis feil
 */
@Component
public class IverksettVedtakValidering extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(IverksettVedtakValidering.class);

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public IverksettVedtakValidering(BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        log.info("IverksetteVedtakValidering initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_VALIDERING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        if (prosessType != ProsessType.IVERKSETT_VEDTAK) {
            String feilmelding = "ProsessType " + prosessType + " er ikke støttet.";
            håndterUnntak(TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Behandling behandling = prosessinstans.getBehandling();
        if (behandling == null) {
            String feilmelding = "Prosessinstans " + prosessinstans.getId() + " er ikke knyttet til en behandling.";
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId()).orElse(null);
        if (behandlingsresultat == null) {
            String feilmelding = "Ingen behandlingsresultat knyttet til behandling " + behandling.getId();
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.isEmpty()) {
            String feilmelding = "Lovvalgsperiode mangler for behandlingsresultat " + behandlingsresultat.getId();
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        String saksbehandlerID = prosessinstans.getData(SAKSBEHANDLER);
        if (saksbehandlerID == null) {
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "SaksbehandlerID er ikke oppgitt.", null);
            return;
        }

        String behandlingsResultatType = prosessinstans.getData(BEHANDLINGSRESULTATTYPE);
        if (behandlingsResultatType == null) {
            håndterUnntak(FUNKSJONELL_FEIL, prosessinstans, "behandlingsResultatType er ikke oppgitt.", null);
            return;
        }

        prosessinstans.setSteg(IV_OPPDATER_RESULTAT);
    }
}
