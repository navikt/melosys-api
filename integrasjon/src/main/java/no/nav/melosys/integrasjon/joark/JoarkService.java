package no.nav.melosys.integrasjon.joark;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.dok.tjenester.journalfoerinngaaende.*;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.*;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journalfoeringsbehov;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.JournalpostMangler;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovResponse;
import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import static no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand.UTGAAR;

@Service
public class JoarkService implements JoarkFasade {

    private static Logger log = LoggerFactory.getLogger(JoarkService.class);

    private InngaaendeJournalConsumer inngåendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private final JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer;

    @Autowired
    public JoarkService(InngaaendeJournalConsumer inngåendeJournal, JournalConsumer journal, JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer) {
        this.inngåendeJournalConsumer = inngåendeJournal;
        this.journalConsumer = journal;
        this.journalfoerInngaaendeConsumer = journalfoerInngaaendeConsumer;
    }

    @Override
    public void ferdigstillJournalføring(String journalpostId) throws FunksjonellException, IntegrasjonException {
        PutJournalpostRequest journalpostRequest = new PutJournalpostRequest();
        journalpostRequest.setForsoekEndeligJF(true);
        journalpostRequest.setJournalfEnhet(Fagsystem.MELOSYS.getKode());
        journalfoerInngaaendeConsumer.oppdaterJournalpost(journalpostRequest, journalpostId);
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

        GetJournalpostResponse response = journalfoerInngaaendeConsumer.hentJournalpost(journalpostID);

        Journalpost journalpost = new Journalpost(journalpostID);
        journalpost.setBrukerId(response.getBrukerListe().stream().map(Bruker::getIdentifikator).findFirst().orElse(null));
        journalpost.setAvsenderId(response.getAvsender().getIdentifikator());
        journalpost.setForsendelseMottatt(response.getForsendelseMottatt().toInstant());

        if (response.getDokumentListe().size() > 1) {
            log.warn("Journalpost {} inneholder flere dokumenter!", journalpostID);
        }
        Dokument dokument = response.getDokumentListe().get(0);
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokument.getDokumentId());
        arkivDokument.setTittel(dokument.getTittel());

        journalpost.setHoveddokument(arkivDokument);

        if (response.getArkivSak() != null) {
            journalpost.setArkivSakId(response.getArkivSak().getArkivSakId());
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
    public void oppdaterJournalpost(String journalpostId, String dokumentID, Long gsakSaksnummer, String brukerID, String avsenderID, String avsenderNavn, String tittel, List<String> vedleggTittelListe, boolean medDokumentkategori) throws SikkerhetsbegrensningException, IntegrasjonException {

        oppdaterDokument(journalpostId, dokumentID, tittel, medDokumentkategori);
        if (!CollectionUtils.isEmpty(vedleggTittelListe)) {
            for (String vedleggTittel : vedleggTittelListe) {
                PostLogiskVedleggRequest logiskVedleggRequest = new PostLogiskVedleggRequest();
                logiskVedleggRequest.setTittel(vedleggTittel);
                journalfoerInngaaendeConsumer.leggTilLogiskVedlegg(logiskVedleggRequest, journalpostId, dokumentID);
            }
        }

        PutJournalpostRequest journalpost = new PutJournalpostRequest();

        ArkivSakWithArkivsakSystemEnum arkivsak = new ArkivSakWithArkivsakSystemEnum();
        arkivsak.setArkivSakId(Long.toString(gsakSaksnummer));
        arkivsak.setArkivSakSystem(ArkivSakWithArkivsakSystemEnum.ArkivSakSystem.GSAK);
        journalpost.setArkivSak(arkivsak);

        Bruker bruker = new Bruker();
        bruker.setIdentifikator(brukerID);
        bruker.setBrukerType(Bruker.BrukerType.PERSON);
        journalpost.setBruker(bruker);

        no.nav.dok.tjenester.journalfoerinngaaende.Avsender avsender = new no.nav.dok.tjenester.journalfoerinngaaende.Avsender();
        avsender.setAvsenderType(no.nav.dok.tjenester.journalfoerinngaaende.Avsender.AvsenderType.PERSON);
        avsender.setIdentifikator(avsenderID);
        avsender.setNavn(avsenderNavn);
        journalpost.setAvsender(avsender);

        journalpost.setJournalfEnhet(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
        journalpost.setForsoekEndeligJF(false);

        journalfoerInngaaendeConsumer.oppdaterJournalpost(journalpost, journalpostId);
    }

    private void oppdaterDokument(String journalpostId, String dokumentID, String tittel, boolean medDokumentkategori) throws SikkerhetsbegrensningException, IntegrasjonException {
        PutDokumentRequest dokumentRequest = new PutDokumentRequest();
        if (medDokumentkategori) {
            dokumentRequest.setDokumentKategori(DokumentKategoriKode.IS.getKode());
        }

        dokumentRequest.setTittel(tittel);
        journalfoerInngaaendeConsumer.oppdaterDokument(dokumentRequest, journalpostId, dokumentID);
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
