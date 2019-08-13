package no.nav.melosys.saksflyt.steg.iv;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static no.nav.melosys.domain.ProsessDataKey.BEHANDLINGSRESULTATTYPE;
import static no.nav.melosys.domain.ProsessDataKey.SAKSBEHANDLER;
import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_RESULTAT;
import static no.nav.melosys.domain.ProsessSteg.IV_VALIDERING;

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
    private static final EnumSet AKSEPTERTE_PROSESSTYPER = EnumSet.of(
        ProsessType.IVERKSETT_VEDTAK,
        ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);

    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public IverksettVedtakValidering(BehandlingsresultatService behandlingsresultatService) {
        this.behandlingsresultatService = behandlingsresultatService;
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
        if (!AKSEPTERTE_PROSESSTYPER.contains(prosessType)) {
            throw new TekniskException("ProsessType " + prosessType + " er ikke støttet.");
        }

        Behandling behandling = prosessinstans.getBehandling();
        if (behandling == null) {
            throw new FunksjonellException("Prosessinstans " + prosessinstans.getId() + " er ikke knyttet til en behandling.");
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (behandlingsresultat == null) {
            throw new TekniskException("Ingen behandlingsresultat knyttet til behandling " + behandling.getId());
        }

        String saksbehandlerID = prosessinstans.getData(SAKSBEHANDLER);
        if (StringUtils.isEmpty(saksbehandlerID) && !behandlingsresultat.erAutomatisert()) {
            throw new FunksjonellException("SaksbehandlerID er ikke oppgitt.");
        }

        if (prosessinstans.getType() == ProsessType.IVERKSETT_VEDTAK) {
            validerBehandlingsResultatType(prosessinstans);
            validerLovalgsperioder(prosessinstans, behandlingsresultat);
        }

        prosessinstans.setSteg(IV_OPPDATER_RESULTAT);
    }

    private void validerBehandlingsResultatType(Prosessinstans prosessinstans) throws FunksjonellException {
        String behandlingsResultatType = prosessinstans.getData(BEHANDLINGSRESULTATTYPE);
        if (behandlingsResultatType == null) {
            throw new FunksjonellException("BehandlingsResultatType er ikke oppgitt.");
        }
    }

    private void validerLovalgsperioder(Prosessinstans prosessinstans, Behandlingsresultat behandlingsresultat) throws FunksjonellException {
        if (Behandlingsresultattyper.valueOf(prosessinstans.getData(BEHANDLINGSRESULTATTYPE)) == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND) {
            Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
            if (lovvalgsperioder.isEmpty()) {
                throw new FunksjonellException("Lovvalgsperiode mangler for behandlingsresultat " + behandlingsresultat.getId());
            }
        }
    }
}