package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.sikkerhet.context.SaksflytSubjektHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;


/**
 * En arbeidertråd som schedulerer arbeid som utføres av de maskinelle stegene.
 * Klassen er ikke en bønne håndtert av Spring, og får allse sine bønne-avhengigheter i konstruktøren.
 *
 * Konfigurasjon:
 * melosys.saksflyt.arbeider.oppholdMellomSteg – Hvor mange millisekunder trådene skal sove mellom hvert steg som aktiveres (default 47)
 */
@Component
@Scope(SCOPE_PROTOTYPE)
public class ArbeiderTraad implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ArbeiderTraad.class);
    private static final String PID_MELDING = "{}: {}";

    private final long oppholdMellomSteg;
    private final Binge binge;
    private final ProsessinstansRepository prosessinstansRepo;
    private final List<StegBehandler> stegBehandlere;
    private final Map<Feilkategori, UnntakBehandler> unntakBehandlere;
    private StegBehandler aktivStegBehandler; // Brukes kun for logging
    private Prosessinstans aktivProsessinstans;

    @Autowired
    ArbeiderTraad(Binge binge,
        ProsessinstansRepository prosessinstansRepo,
        List<StegBehandler> stegBehandlere,
        @Value("${melosys.saksflyt.arbeider.oppholdMellomSteg:47}") long oppholdMellomSteg) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
        this.stegBehandlere = stegBehandlere;
        this.oppholdMellomSteg = oppholdMellomSteg;
        this.unntakBehandlere = FeilStrategi.standardFeilHåndtering();
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            for (StegBehandler stegBehandler : stegBehandlere) {
                try {
                    finnProsessinstansOgUtførSteg(stegBehandler);
                    Thread.sleep(oppholdMellomSteg);
                } catch (InterruptedException e) {
                    log.error("Stegbehandler {} ble avbryt!", lagStegNavn(aktivStegBehandler));
                    // Prosessinstanser som avbrytes må registreres som feilet.
                    settTilFeilet();
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Ubehandlet exception! Aktiv stegBehandler: {}.", lagStegNavn(aktivStegBehandler), e);
                    settTilFeilet();
                }
            }
        }
    }

    private void settTilFeilet() {
        if (aktivProsessinstans != null) {
            log.error("Prosessinstans som må ryddes opp i: {}.", aktivProsessinstans.getId());
            try {
                aktivProsessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
                prosessinstansRepo.save(aktivProsessinstans);
                aktivProsessinstans = null;
            } catch (Exception e) {
                log.error("Prosessinstans {} kunne ikke settes til feilet: ", aktivProsessinstans.getId(), e);
            }
        }
    }

    private void finnProsessinstansOgUtførSteg(StegBehandler stegBehandler) {
        Prosessinstans pi = binge.hentOgSettProsessinstansTilAktiv(stegBehandler.inngangsvilkår());
        if (pi == null) {
            aktivStegBehandler = null;
            return;
        }

        SaksflytSubjektHolder.set(pi.getData(ProsessDataKey.SAKSBEHANDLER));

        aktivStegBehandler = stegBehandler;
        aktivProsessinstans = pi;
        try {
            ProsessSteg gammeltSteg = pi.getSteg();
            utførSteg(stegBehandler, pi);

            if (pi.getSteg() != gammeltSteg) {
                pi.setAntallRetry(0);
                pi.setSistForsøkt(LocalDateTime.now());
            }
            pi.setEndretDato(LocalDateTime.now());
            pi = prosessinstansRepo.save(pi); // Kan resultere i DataAccessException
            aktivProsessinstans = null;
        } finally {
            binge.fjernFraAktiveProsessinstanser(pi);
            SaksflytSubjektHolder.reset();
        }

        if (pi.getSteg() != ProsessSteg.FERDIG && pi.getSteg() != ProsessSteg.FEILET_MASKINELT) {
            binge.leggTil(pi);
        }
    }

    private void utførSteg(StegBehandler stegBehandler, Prosessinstans prosessinstans) {
        try {
            stegBehandler.utfør(prosessinstans);
        } catch (SikkerhetsbegrensningException e) {
            String feilmelding = "SikkerhetsbegrensningException ";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.INGEN_TILGANG, prosessinstans, feilmelding, e);
        } catch (IkkeFunnetException e) {
            String feilmelding = "IkkeFunnetException ";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.IKKE_FUNNET, prosessinstans, feilmelding, e);
        } catch (FunksjonellException e) {
            String feilmelding = "FunksjonellException ";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, e);
        } catch (TekniskException e) {
            String feilmelding = "TekniskException ";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, e);
        } catch (Exception e) {
            String feilmelding = "Exception ";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding, e);
            håndterUnntak(Feilkategori.UVENTET_EXCEPTION, prosessinstans, feilmelding, e);
        } catch (Throwable t) { //NOSONAR
            String feilmelding = "Throwable ";
            log.error(PID_MELDING, prosessinstans.getId(), feilmelding, t);
            håndterUnntak(Feilkategori.UVENTET_EXCEPTION, prosessinstans, feilmelding, t);
        }
    }

    private void håndterUnntak(Feilkategori kategori, Prosessinstans prosessinstans, String melding, Throwable e) {
        UnntakBehandler ub = unntakBehandlere.get(kategori);
        ub.behandleUnntak(prosessinstans, melding, e); // OK med NPE hvis ub er null
    }

    private static String lagStegNavn(StegBehandler stegBehandler) {
        return ClassUtils.getUserClass(stegBehandler.getClass()).getSimpleName();
    }
}
