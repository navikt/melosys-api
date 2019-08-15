package no.nav.melosys.service.dokument;

import java.util.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;

@Service
public class BrevmottakerService {
    private static final Set<Produserbaredokumenter> DOKUMENTER_TIL_BRUKER = Collections.unmodifiableSet(EnumSet.of(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK, MELDING_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK, INNVILGELSE_YRKESAKTIV));

    private final KontaktopplysningService kontaktopplysningService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    @Autowired
    public BrevmottakerService(KontaktopplysningService kontaktopplysningService,
                               AvklarteVirksomheterService avklarteVirksomheterService,
                               UtenlandskMyndighetService utenlandskMyndighetService) {
        this.kontaktopplysningService = kontaktopplysningService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    public Aktoersroller avklarMottakerRolleFraDokument(Produserbaredokumenter produserbartDokument) throws TekniskException {
        Aktoersroller mottakerRolle;
        if (DOKUMENTER_TIL_BRUKER.contains(produserbartDokument)) {
            mottakerRolle = BRUKER;
        } else if (produserbartDokument == INNVILGELSE_ARBEIDSGIVER || produserbartDokument == AVSLAG_ARBEIDSGIVER) {
            mottakerRolle = ARBEIDSGIVER;
        } else if (produserbartDokument == ANMODNING_UNNTAK || produserbartDokument == ATTEST_A1) {
            mottakerRolle = MYNDIGHET;
        } else {
            throw new TekniskException("Valg av mottakerRolle støttes ikke for " + produserbartDokument);
        }
        return mottakerRolle;
    }

    public List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling) throws FunksjonellException, TekniskException {
        return avklarMottakere(produserbartDokument, mottaker, behandling, false);
    }

    public List<Aktoer> avklarMottakere(Produserbaredokumenter produserbartDokument, Mottaker mottaker, Behandling behandling, boolean forhåndsvisning)
        throws FunksjonellException, TekniskException {
        List<Aktoer> mottakere;
        Aktoersroller mottakerRolle = mottaker.getRolle();
        if (mottakerRolle == BRUKER) {
            mottakere = avklarMottakereForBruker(produserbartDokument, behandling, forhåndsvisning);
        } else if (mottakerRolle == ARBEIDSGIVER) {
            mottakere = avklarMottakereForArbeidsgiver(behandling);
        } else if (mottakerRolle == MYNDIGHET) {
            mottakere = avklarMottakereForMyndigheter(mottaker, behandling);
        } else {
            throw new FunksjonellException(mottakerRolle + " støttes ikke.");
        }
        return mottakere;
    }

    private List<Aktoer> avklarMottakereForBruker(Produserbaredokumenter produserbartDokument, Behandling behandling, boolean forhåndsvisning)
        throws FunksjonellException, TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.hentBruker();
        if (bruker == null) {
            throw new FunksjonellException("Bruker er ikke registrert.");
        }

        // Dokumenter til bruker sendes i utgangspunkt bare til fullmektig dersom fullmektig finnes.
        // Vedtaksbrevene er imidlertid sendt til både bruker og fullmektig (gjelder ikke forhåndsvisning).
        boolean tilBegge = false;
        if (produserbartDokument == INNVILGELSE_YRKESAKTIV || produserbartDokument == AVSLAG_YRKESAKTIV) {
            tilBegge = !forhåndsvisning;
        }

        List<Aktoer> mottakere = new ArrayList<>();
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.BRUKER);
        if (representant.isPresent()) {
            mottakere.add(representant.get());
            if (tilBegge) {
                mottakere.add(bruker);
            }
        } else {
            mottakere.add(bruker);
        }
        return mottakere;
    }

    // Dokumenter til arbeidsgiver sendes bare til representant når representant finnes.
    private List<Aktoer> avklarMottakereForArbeidsgiver(Behandling behandling) throws FunksjonellException, TekniskException {
        List<Aktoer> mottakere = new ArrayList<>();
        Fagsak fagsak = behandling.getFagsak();
        Optional<Aktoer> representant = fagsak.hentRepresentant(Representerer.ARBEIDSGIVER);
        if (representant.isPresent()) {
            mottakere.add(representant.get());
        } else {
            mottakere.add(avklarArbeidsgiver(behandling));
        }
        return mottakere;
    }

    private Aktoer avklarArbeidsgiver(Behandling behandling) throws FunksjonellException, TekniskException {
        Aktoer arbeidsgiver = behandling.getFagsak().hentArbeidsgiver();
        if (arbeidsgiver != null) {
            return arbeidsgiver;
        } else {
            Set<String> arbeidsgivendeOrgnumre = avklarteVirksomheterService.hentArbeidsgivendeOrgnumre(behandling);
            if (arbeidsgivendeOrgnumre.isEmpty()) {
                throw new FunksjonellException("Arbeidsgiver er ikke registrert.");
            } else if (arbeidsgivendeOrgnumre.size() > 1) {
                throw new FunksjonellException("Flere arbeidsgivere er avklart.");
            } else {
                String orgnr = arbeidsgivendeOrgnumre.iterator().next();
                Aktoer avklartArbeidsgiver = new Aktoer();
                avklartArbeidsgiver.setRolle(ARBEIDSGIVER);
                avklartArbeidsgiver.setOrgnr(orgnr);
                return avklartArbeidsgiver;
            }
        }
    }

    private List<Aktoer> avklarMottakereForMyndigheter(Mottaker mottaker, Behandling behandling) throws TekniskException {
        if (mottaker.getAktør().getOrgnr() != null) {
            // Norsk myndighet har orgnummer.
            return Collections.singletonList(mottaker.getAktør());
        } else {
            // Utenlandsk myndighet
            return utenlandskMyndighetService.lagUtenlandskMyndighetFraBehandling(behandling);
        }
    }

    public Kontaktopplysning hentKontaktopplysning(String saksnumner, Aktoer mottaker) {
        if (mottaker == null) {
            return null;
        }

        Aktoersroller mottakerRolle = mottaker.getRolle();
        if (mottakerRolle == ARBEIDSGIVER || mottakerRolle == REPRESENTANT) {
            return kontaktopplysningService.hentKontaktopplysning(saksnumner, mottaker.getOrgnr()).orElse(null);
        } else {
            return null;
        }
    }
}