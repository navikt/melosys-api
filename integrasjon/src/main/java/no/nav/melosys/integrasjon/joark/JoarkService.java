package no.nav.melosys.integrasjon.joark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.dok.tjenester.journalfoerinngaaende.*;
import no.nav.dok.tjenester.journalfoerinngaaende.response.Mangler;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import static no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand.UTGAAR;

@Service
@Primary
public class JoarkService implements JoarkFasade {
    private final InngaaendeJournalConsumer inngåendeJournalConsumer;
    private final JournalConsumer journalConsumer;
    private final JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer;
    private final JournalpostapiConsumer journalpostapiConsumer;

    @Autowired
    public JoarkService(InngaaendeJournalConsumer inngåendeJournal,
                        JournalConsumer journal,
                        JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer,
                        JournalpostapiConsumer journalpostapiConsumer) {
        this.inngåendeJournalConsumer = inngåendeJournal;
        this.journalConsumer = journal;
        this.journalfoerInngaaendeConsumer = journalfoerInngaaendeConsumer;
        this.journalpostapiConsumer = journalpostapiConsumer;
    }

    @Override
    public void ferdigstillJournalføring(String journalpostId) throws FunksjonellException, IntegrasjonException {
        PutJournalpostRequest journalpostRequest = new PutJournalpostRequest();
        journalpostRequest.setForsoekEndeligJF(true);
        journalpostRequest.setJournalfEnhet(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
        PutJournalpostResponse putJournalpostResponse = journalfoerInngaaendeConsumer.oppdaterJournalpost(journalpostRequest, journalpostId);
        validerOppdaterJournalpostResponse(putJournalpostResponse);
    }

    private void validerOppdaterJournalpostResponse(PutJournalpostResponse putJournalpostResponse) throws FunksjonellException {
        if (!putJournalpostResponse.isHarEndeligJF()) {
            String exceptionString = "Journalpost " + putJournalpostResponse.getJournalpostId() + " har ikke blitt endelig journalført";
            Mangler mangler = putJournalpostResponse.getMangler();
            if (mangler != null) {
                exceptionString += getManglerString(mangler);
            }
            throw new FunksjonellException(exceptionString);
        }
    }

    private String getManglerString(Mangler mangler) {
        String manglerString = "\nAvsendernavn: " + mangler.getAvsenderNavn().value() + "\n";
        manglerString += "Bruker: " + mangler.getBruker().value() + "\n";
        manglerString += "Akrivsak: " + mangler.getArkivSak().value() + "\n";
        manglerString += "Tema: " + mangler.getTema().value() + "\n";
        manglerString += "Tittel: " + mangler.getTittel().value();
        return manglerString;
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
    public Journalpost hentJournalpost(String journalpostID) throws IntegrasjonException, SikkerhetsbegrensningException {
        GetJournalpostResponse response = journalfoerInngaaendeConsumer.hentJournalpost(journalpostID);

        Journalpost journalpost = new Journalpost(journalpostID);
        journalpost.setBrukerId(response.getBrukerListe().stream().map(Bruker::getIdentifikator).findFirst().orElse(null));
        journalpost.setForsendelseMottatt(response.getForsendelseMottatt().toInstant());
        journalpost.setMottaksKanal(response.getMottaksKanal());

        List<Dokument> dokumentListe = response.getDokumentListe();
        journalpost.setHoveddokument(lagArkivDokument(dokumentListe.get(0)));
        if (dokumentListe.size() > 1) {
            for (int i = 1; i < dokumentListe.size(); i++) {
                journalpost.getVedleggListe().add(lagArkivDokument(dokumentListe.get(i)));
            }
        }

        if (response.getAvsender() != null) {
            journalpost.setAvsenderId(response.getAvsender().getIdentifikator());
        }
        if (response.getArkivSak() != null) {
            journalpost.setArkivSakId(response.getArkivSak().getArkivSakId());
        }

        return journalpost;
    }

    private ArkivDokument lagArkivDokument(Dokument dokument) {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokument.getDokumentId());
        arkivDokument.setTittel(dokument.getTittel());
        arkivDokument.setNavSkjemaID(dokument.getNavSkjemaId());
        return arkivDokument;
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
    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request, boolean forsøkEndeligJfr) {
        return journalpostapiConsumer.opprettJournalpost(request, forsøkEndeligJfr);
    }

    @Override
    public void oppdaterJournalpost(String journalpostID, String hovedDokumentID, JournalpostOppdatering journalpostOppdatering)
        throws SikkerhetsbegrensningException, IntegrasjonException {

        oppdaterDokument(journalpostID, hovedDokumentID, journalpostOppdatering.getTittel(), journalpostOppdatering.isMedDokumentkategori());
        Map<String, String> fysiskeVedlegg = journalpostOppdatering.getFysiskeVedlegg();
        if (!CollectionUtils.isEmpty(fysiskeVedlegg)) {
            for (Map.Entry<String, String> vedleggIdMedTittel : fysiskeVedlegg.entrySet()) {
                oppdaterDokument(journalpostID, vedleggIdMedTittel.getKey(), vedleggIdMedTittel.getValue(), false);
            }
        }

        List<String> logiskeVedleggTitler = journalpostOppdatering.getLogiskeVedleggTitler();
        if (!CollectionUtils.isEmpty(logiskeVedleggTitler)) {
            for (String vedleggTittel : logiskeVedleggTitler) {
                PostLogiskVedleggRequest logiskVedleggRequest = new PostLogiskVedleggRequest();
                logiskVedleggRequest.setTittel(vedleggTittel);
                journalfoerInngaaendeConsumer.leggTilLogiskVedlegg(logiskVedleggRequest, journalpostID, hovedDokumentID);
            }
        }

        PutJournalpostRequest journalpost = new PutJournalpostRequest();
        journalpost.setTittel(journalpostOppdatering.getTittel());

        ArkivSakWithArkivsakSystemEnum arkivsak = new ArkivSakWithArkivsakSystemEnum();
        arkivsak.setArkivSakId(Long.toString(journalpostOppdatering.getGsakSaksnummer()));
        arkivsak.setArkivSakSystem(ArkivSakWithArkivsakSystemEnum.ArkivSakSystem.GSAK);
        journalpost.setArkivSak(arkivsak);

        Bruker bruker = new Bruker();
        bruker.setIdentifikator(journalpostOppdatering.getBrukerID());
        bruker.setBrukerType(Bruker.BrukerType.PERSON);
        journalpost.setBruker(bruker);

        no.nav.dok.tjenester.journalfoerinngaaende.Avsender avsender = new no.nav.dok.tjenester.journalfoerinngaaende.Avsender();
        avsender.setAvsenderType(no.nav.dok.tjenester.journalfoerinngaaende.Avsender.AvsenderType.PERSON);
        avsender.setIdentifikator(journalpostOppdatering.getAvsenderID());
        avsender.setNavn(journalpostOppdatering.getAvsenderNavn());
        journalpost.setAvsender(avsender);

        journalpost.setForsoekEndeligJF(false);

        journalfoerInngaaendeConsumer.oppdaterJournalpost(journalpost, journalpostID);
    }

    @Override
    public void oppdaterJournalpostMedSaksnummerOgBruker(String journalpostID, String brukerID, Long gsakSaksnummer, boolean forsokFerdigstill) throws SikkerhetsbegrensningException, IntegrasjonException {
        PutJournalpostRequest request = new PutJournalpostRequest();

        ArkivSakWithArkivsakSystemEnum arkivsak = new ArkivSakWithArkivsakSystemEnum();
        arkivsak.setArkivSakSystem(ArkivSakWithArkivsakSystemEnum.ArkivSakSystem.GSAK);
        arkivsak.setArkivSakId(Long.toString(gsakSaksnummer));
        request.setArkivSak(arkivsak);

        Bruker bruker = new Bruker();
        bruker.setBrukerType(Bruker.BrukerType.PERSON);
        bruker.setIdentifikator(brukerID);
        request.setBruker(bruker);

        request.setJournalfEnhet(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
        request.setForsoekEndeligJF(forsokFerdigstill);
        journalfoerInngaaendeConsumer.oppdaterJournalpost(request, journalpostID);
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
            | UtledJournalfoeringsbehovJournalpostIkkeInngaaende e) {
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
        if (!CollectionUtils.isEmpty(input.getVedleggListe())) {
            mangler.add(JournalfoeringMangel.VEDLEGG);
        }
        if (input.getTema() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.TEMA);
        }

        return mangler;
    }
}
