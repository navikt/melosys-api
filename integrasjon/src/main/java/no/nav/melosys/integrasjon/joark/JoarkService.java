package no.nav.melosys.integrasjon.joark;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.behandleinngaaendejournal.BehandleInngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.ArkivSak;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Avsender;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentkategori;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.*;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovResponse;
import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand.UTGAAR;

@Service
public class JoarkService implements JoarkFasade {

    private BehandleInngaaendeJournalConsumer behandleInngåendeJournalConsumer;
    private InngaaendeJournalConsumer inngåendeJournalConsumer;
    private JournalConsumer journalConsumer;

    @Autowired
    public JoarkService(BehandleInngaaendeJournalConsumer behandleInngåendeJournal, InngaaendeJournalConsumer inngåendeJournal, JournalConsumer journal) {
        this.behandleInngåendeJournalConsumer = behandleInngåendeJournal;
        this.inngåendeJournalConsumer = inngåendeJournal;
        this.journalConsumer = journal;
    }

    @Override
    public void ferdigstillJournalføring(String journalpostId) throws FunksjonellException {
        FerdigstillJournalfoeringRequest request = new FerdigstillJournalfoeringRequest();
        request.setJournalpostId(journalpostId);
        request.setEnhetId(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
        try {
            behandleInngåendeJournalConsumer.ferdigstillJournalfoering(request);
        } catch (FerdigstillJournalfoeringFerdigstillingIkkeMulig | FerdigstillJournalfoeringJournalpostIkkeInngaaende | FerdigstillJournalfoeringUgyldigInput e) {
            throw new FunksjonellException(e);
        } catch (FerdigstillJournalfoeringObjektIkkeFunnet e) {
            throw new IkkeFunnetException(e.getMessage());
        } catch (FerdigstillJournalfoeringSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        }
    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) throws SikkerhetsbegrensningException, IkkeFunnetException {
        HentDokumentRequest request = new HentDokumentRequest();
        request.setDokumentId(dokumentID);
        request.setJournalpostId(journalPostID);

        Variantformater variantformat = new Variantformater();
        variantformat.setValue(Variantformat.ARKIV.toString());
        request.setVariantformat(variantformat);

        HentDokumentResponse hentDokumentResponse;
        try {
            hentDokumentResponse = journalConsumer.hentDokument(request);
        } catch (HentDokumentDokumentIkkeFunnet | HentDokumentJournalpostIkkeFunnet e) {
            throw new IkkeFunnetException(e.getMessage());
        } catch (HentDokumentSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        }
        return hentDokumentResponse.getDokument();
    }

    @Override
    public Journalpost hentJournalpost(String journalpostID) throws FunksjonellException, IntegrasjonException {
        HentJournalpostRequest request = new HentJournalpostRequest();
        request.setJournalpostId(journalpostID);

        HentJournalpostResponse hentJournalpostResponse;
        try {
            hentJournalpostResponse = inngåendeJournalConsumer.hentJournalpost(request);
        } catch (HentJournalpostJournalpostIkkeFunnet e) {
            throw new IkkeFunnetException(e);
        } catch (HentJournalpostJournalpostIkkeInngaaende | HentJournalpostUgyldigInput e) {
            throw new FunksjonellException(e);
        } catch (HentJournalpostSikkerhetsbegrensning hentJournalpostSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentJournalpostSikkerhetsbegrensning);
        }

        InngaaendeJournalpost inngaaendeJournalpost = hentJournalpostResponse.getInngaaendeJournalpost();
        Journalpost journalpost = new Journalpost(journalpostID);
        List<Aktoer> brukerListe = inngaaendeJournalpost.getBrukerListe();
        if (!brukerListe.isEmpty()) {
            if (brukerListe.size() > 1) {
                // Vi antar at det er bare en bruker.
                // Flere brukere på samme journalpost har ikke vært et behov.
                throw new FunksjonellException("Det finnes flere brukere i journalpost " + journalpostID); 
            } else {
                Aktoer aktoer = brukerListe.get(0);
                if (aktoer instanceof Person) {
                    Person p = (Person) aktoer;
                    journalpost.setBrukerId(p.getIdent());
                } else if (aktoer instanceof Organisasjon) {
                    throw new IntegrasjonException("Organisasjon i brukerlisten er ikke støttet");
                } else {
                    throw new IntegrasjonException(aktoer.getClass().getSimpleName() + " i brukerlisten er ikke støttet");
                }
            }
        }
        journalpost.setAvsenderId(inngaaendeJournalpost.getAvsenderId());
        if (inngaaendeJournalpost.getForsendelseMottatt() != null) {
            journalpost.setForsendelseMottatt(KonverteringsUtils.xmlGregorianCalendarToInstant(inngaaendeJournalpost.getForsendelseMottatt()));
        }
        Dokumentinformasjon hoveddokument = inngaaendeJournalpost.getHoveddokument();
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(hoveddokument.getDokumentId());
        // FIXME MELOSYS-1416
        // Tjenesten mangler opplysninger (tittel og vedleggstitler) tilgjengelige i nye REST tjenester (https://jira.adeo.no/browse/PK-51497).
        arkivDokument.setTittel("");
        journalpost.setHoveddokument(arkivDokument);
        if (inngaaendeJournalpost.getArkivSak() != null) {
            journalpost.setArkivSakId(inngaaendeJournalpost.getArkivSak().getArkivSakId());
        }

        return journalpost;
    }

    @Override
    public List<Journalpost> hentKjerneJournalpostListe(Long arkivSakID) throws IntegrasjonException, SikkerhetsbegrensningException {
        Assert.notNull(arkivSakID, "HentKjerneJournalpostListe krever en arkivSakID.");
        HentKjerneJournalpostListeRequest hentKjerneJournalpostListeRequest = new HentKjerneJournalpostListeRequest();
        hentKjerneJournalpostListeRequest.getArkivSakListe().add(lagArkivSak(arkivSakID, Fagsystem.GSAK_I_JOARK.getKode()));

        HentKjerneJournalpostListeResponse hentKjerneJournalpostListeResponse;
        try {
            hentKjerneJournalpostListeResponse = journalConsumer.hentKjerneJournalpostListe(hentKjerneJournalpostListeRequest);
        } catch (HentKjerneJournalpostListeSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (HentKjerneJournalpostListeUgyldigInput e) {
            throw new IntegrasjonException(e);
        }

        return hentKjerneJournalpostListeResponse.getJournalpostListe()
            .stream()
            .filter(journalpost -> !UTGAAR.equals(journalpost.getJournaltilstand()))
            .map(this::lagJournalpost)
            .collect(Collectors.toList());
    }

    private no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak lagArkivSak(Long arkivSakId, String fagsystem) {
        no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak arkivSak =
            new no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak();
        arkivSak.setArkivSakSystem(fagsystem);
        arkivSak.setArkivSakId(Long.toString(arkivSakId));
        arkivSak.setErFeilregistrert(false);
        return arkivSak;
    }

    private Journalpost lagJournalpost(no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost j) {
        Journalpost journalpost = new Journalpost(j.getJournalpostId());
        if (j.getGjelderArkivSak() != null) {
            journalpost.setArkivSakId(j.getGjelderArkivSak().getArkivSakId());
        }
        if (j.getForsendelseMottatt() != null) {
            journalpost.setForsendelseMottatt(KonverteringsUtils.xmlGregorianCalendarToInstant(j.getForsendelseMottatt()));
        }
        if (j.getKorrespondansePart() != null) {
            journalpost.setKorrespondansepartId(j.getKorrespondansePart().getKorrespondansepartId());
            journalpost.setKorrespondansepartNavn(j.getKorrespondansePart().getKorrespondansepartNavn());
        }
        journalpost.setHoveddokument(lagArkivDokument(j.getHoveddokument()));
        journalpost.setInnhold(j.getInnhold());
        if (j.getForsendelseJournalfoert() != null) {
            journalpost.setForsendelseJournalfoert(KonverteringsUtils.xmlGregorianCalendarToInstant(j.getForsendelseJournalfoert()));
        }
        journalpost.setJournalposttype(Journalposttype.fraKode(j.getJournalposttype().getValue()));

        j.getVedleggListe().forEach(vedlegg -> journalpost.getVedleggListe().add(lagArkivDokument(vedlegg)));
        return journalpost;
    }

    private ArkivDokument lagArkivDokument(DetaljertDokumentinformasjon detaljertDokumentinformasjon) {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(detaljertDokumentinformasjon.getDokumentId());
        arkivDokument.setTittel(detaljertDokumentinformasjon.getTittel());

        detaljertDokumentinformasjon.getSkannetInnholdListe()
            .forEach(vedlegg -> arkivDokument.getInterneVedlegg().add(new ArkivDokumentVedlegg(vedlegg.getVedleggInnhold())));
        return arkivDokument;
    }

    @Override
    public void oppdaterJounalpost(String journalpostId, String dokumentID, Long gsakSaksnummer, String brukerID, String avsenderID, String avsenderNavn, String tittel, boolean medDokumentkategori)
        throws FunksjonellException {
        OppdaterJournalpostRequest request = new OppdaterJournalpostRequest();
        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost journalpost =
                new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost();
        journalpost.setJournalpostId(journalpostId);

        ArkivSak arkivSak = new ArkivSak();
        arkivSak.setArkivSakId(Long.toString(gsakSaksnummer));
        arkivSak.setArkivSakSystem(Fagsystem.GSAK_I_JOARK.getKode());
        journalpost.setArkivSak(arkivSak);

        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person bruker = new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person();
        bruker.setIdent(brukerID);
        journalpost.setBruker(bruker);

        Avsender avsender = new Avsender();
        avsender.setAvsenderId(avsenderID);
        avsender.setAvsenderNavn(avsenderNavn);
        journalpost.setAvsender(avsender);

        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentinformasjon dokumentinfo =
            new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentinformasjon();
        if (medDokumentkategori) {
            Dokumentkategori dokumentkategori = new Dokumentkategori();
            dokumentkategori.setValue(DokumentKategoriKode.IS.getKode());
            dokumentinfo.setDokumentkategori(dokumentkategori);
        }
        dokumentinfo.setDokumentId(dokumentID);
        dokumentinfo.setTittel(tittel);
        journalpost.setHoveddokument(dokumentinfo);
        journalpost.setInnhold(tittel); // Innhold bruker titlen siden det ikke finnes andre grunnlag for det.

        request.setInngaaendeJournalpost(journalpost);
        try {
            behandleInngåendeJournalConsumer.oppdaterJournalpost(request);
        } catch (OppdaterJournalpostSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (OppdaterJournalpostObjektIkkeFunnet e) {
            throw new IkkeFunnetException(e.getMessage());
        } catch (OppdaterJournalpostOppdateringIkkeMulig | OppdaterJournalpostUgyldigInput | OppdaterJournalpostJournalpostIkkeInngaaende e) {
            throw new FunksjonellException(e);
        }
    }

    @Override
    public List<JournalfoeringMangel> utledJournalfoeringsbehov(String journalpostID) throws FunksjonellException {
        UtledJournalfoeringsbehovRequest request = new UtledJournalfoeringsbehovRequest();
        request.setJournalpostId(journalpostID);

        try {
            UtledJournalfoeringsbehovResponse utledJournalfoeringsbehovResponse = inngåendeJournalConsumer.utledJournalfoeringsbehov(request);
            JournalpostMangler journalfoeringsbehov = utledJournalfoeringsbehovResponse.getJournalfoeringsbehov();
            return konverterTilJournalfoeringmangler(journalfoeringsbehov);
        } catch (UtledJournalfoeringsbehovSikkerhetsbegrensning s) {
            throw new SikkerhetsbegrensningException(s);
        } catch (UtledJournalfoeringsbehovJournalpostIkkeFunnet e) {
            throw new IkkeFunnetException(e.getMessage());
        } catch (UtledJournalfoeringsbehovUgyldigInput | UtledJournalfoeringsbehovJournalpostKanIkkeBehandles 
            | UtledJournalfoeringsbehovJournalpostIkkeInngaaende  e) {
            throw new FunksjonellException(e);
        }

    }

    List<JournalfoeringMangel> konverterTilJournalfoeringmangler(JournalpostMangler input) {
        List<JournalfoeringMangel> mangler = new ArrayList<>();

        if (input.getArkivSak() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.ARKIVSAK);
        }
        if (input.getAvsenderId() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.AVSENDERID);
        }
        if (input.getAvsenderNavn() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.AVSENDERNAVN);
        }
        if (input.getBruker() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.BRUKER);
        }
        if (input.getForsendelseInnsendt() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.FORSENDELSEINNSENDT);
        }
        if (input.getHoveddokument().getDokumentkategori() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        }
        if (input.getHoveddokument().getTittel() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.HOVEDDOKUMENT_TITTEL);
        }
        if (input.getInnhold() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.INNHOLD);
        }
        if (input.getVedleggListe() != null && !input.getVedleggListe().isEmpty()) {
            mangler.add(JournalfoeringMangel.VEDLEGG);
        }
        if (input.getTema() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.TEMA);
        }

        return mangler;
    }
}
