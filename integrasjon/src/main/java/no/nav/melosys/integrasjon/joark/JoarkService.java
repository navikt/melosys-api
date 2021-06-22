package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.dok.tjenester.journalfoerinngaaende.Avsender;
import no.nav.dok.tjenester.journalfoerinngaaende.Bruker;
import no.nav.dok.tjenester.journalfoerinngaaende.Dokument;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.AvsenderMottaker;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.saf.SafConsumer;
import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DokumentInnhold;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;

import static no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand.UTGAAR;

@Service
@Primary
public class JoarkService implements JoarkFasade {
    private final JournalConsumer journalConsumer;
    private final JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer;
    private final JournalpostapiConsumer journalpostapiConsumer;
    private final SafConsumer safConsumer;
    private final Unleash unleash;

    static final String SAF_FEATURE_TOGGLE_NAVN = "melosys.saf";

    public JoarkService(JournalConsumer journal,
                        JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer,
                        JournalpostapiConsumer journalpostapiConsumer, SafConsumer safConsumer, Unleash unleash) {
        this.journalConsumer = journal;
        this.journalfoerInngaaendeConsumer = journalfoerInngaaendeConsumer;
        this.journalpostapiConsumer = journalpostapiConsumer;
        this.safConsumer = safConsumer;
        this.unleash = unleash;
    }

    @Override
    public void ferdigstillJournalføring(String journalpostId) {
        FerdigstillJournalpostRequest request = new FerdigstillJournalpostRequest();
        journalpostapiConsumer.ferdigstillJournalpost(request, journalpostId);
    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) {
        if (unleash.isEnabled(SAF_FEATURE_TOGGLE_NAVN)) {
            return safConsumer.hentDokument(journalPostID, dokumentID);
        }

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
    public Journalpost hentJournalpost(String journalpostID) {
        if (unleash.isEnabled(SAF_FEATURE_TOGGLE_NAVN)) {
            return safConsumer.hentJournalpost(journalpostID).tilDomene();
        } else {
            return hentInngåendeJournalpost(journalpostID);
        }
    }

    private Journalpost hentInngåendeJournalpost(String journalpostID) {
        GetJournalpostResponse response;
        try {
            response = journalfoerInngaaendeConsumer.hentJournalpost(journalpostID);
        } catch (IntegrasjonException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof HttpStatusCodeException httpException
                && httpException.getStatusCode().is4xxClientError()
                && httpException.getResponseBodyAsString().contains("Inngaaende")) {
                throw new IkkeInngaaendeJournalpostException(e);
            }
            throw e;
        }
        return lagJournalpostFraResponse(journalpostID, response);
    }

    private Journalpost lagJournalpostFraResponse(String journalpostID, GetJournalpostResponse response) {
        Journalpost journalpost = new Journalpost(journalpostID);
        journalpost.setErFerdigstilt(response.getJournalTilstand() == GetJournalpostResponse.JournalTilstand.ENDELIG);
        response.getBrukerListe().stream().findFirst().ifPresent(b -> {
            journalpost.setBrukerId(b.getIdentifikator());
            journalpost.setBrukerIdType(tilBrukerIdType(b.getBrukerType()));
        });
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
            journalpost.setAvsenderNavn(response.getAvsender().getNavn());

            final Avsender.AvsenderType avsenderType = response.getAvsender().getAvsenderType();
            if (avsenderType != null) {
                if (avsenderType == Avsender.AvsenderType.ORGANISASJON) {
                    journalpost.setAvsenderType(Avsendertyper.ORGANISASJON);
                }
                if (avsenderType == Avsender.AvsenderType.PERSON) {
                    journalpost.setAvsenderType(Avsendertyper.PERSON);
                }
            }
        }
        return journalpost;
    }

    private BrukerIdType tilBrukerIdType(Bruker.BrukerType brukerType) {
        return switch (brukerType) {
            case PERSON -> BrukerIdType.FOLKEREGISTERIDENT;
            case ORGANISASJON -> BrukerIdType.ORGNR;
        };
    }

    public void validerDokumenterTilhørerSakOgHarTilgang(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest,
                                                         Collection<DokumentReferanse> dokumentReferanser) {
        if (CollectionUtils.isEmpty(dokumentReferanser)) {
            return;
        }

        Map<String, Journalpost> journalposterTilknyttetSak = hentJournalposterTilknyttetSak(hentJournalposterTilknyttetSakRequest)
            .stream()
            .collect(Collectors.toMap(Journalpost::getJournalpostId, j -> j));

        for (var dokumentReferanse : dokumentReferanser) {
            if (!journalposterTilknyttetSak.containsKey(dokumentReferanse.getJournalpostID())) {
                throw new FunksjonellException("Journalpost med ID " + dokumentReferanse.getJournalpostID() +
                    " tilhører ikke sak " + hentJournalposterTilknyttetSakRequest.saksnummer());
            }
            var arkivDokument = journalposterTilknyttetSak.get(dokumentReferanse.getJournalpostID()).finnArkivDokument(dokumentReferanse.getDokumentID());
            if (arkivDokument.isEmpty()) {
                throw new IkkeFunnetException("Finner ikke dokument med id" + dokumentReferanse.getDokumentID() +
                    "for journalpost " + dokumentReferanse.getJournalpostID());
            } else if (!arkivDokument.get().harTilgangTilArkivVariant()) {
                throw new SikkerhetsbegrensningException("Ikke tilgang til arkivdokument " + dokumentReferanse.getDokumentID() +
                    " i journalpost " + dokumentReferanse.getJournalpostID());
            }
        }

    }

    @Override
    public LocalDate hentMottaksDatoForJournalpost(String journalpostID) {
        return LocalDate.ofInstant(hentJournalpost(journalpostID).getForsendelseMottatt(), ZoneId.systemDefault());
    }

    private ArkivDokument lagArkivDokument(Dokument dokument) {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokument.getDokumentId());
        arkivDokument.setTittel(dokument.getTittel());
        arkivDokument.setNavSkjemaID(dokument.getNavSkjemaId());
        dokument.getLogiskVedleggListe().forEach(
            l -> arkivDokument.getLogiskeVedlegg().add(new LogiskVedlegg(l.getLogiskVedleggId(), l.getLogiskVedleggTittel()))
        );
        /*dokument.getVariant().forEach(
            v -> arkivDokument.getDokumentVarianter().add(new DokumentVariant(v.getVariantFormat(),v.))
        );*/
        return arkivDokument;
    }

    @Override
    public List<Journalpost> hentJournalposterTilknyttetSak(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest) {

        if (unleash.isEnabled(SAF_FEATURE_TOGGLE_NAVN)) {
            return safConsumer.hentDokumentoversikt(hentJournalposterTilknyttetSakRequest.saksnummer())
                .stream()
                .map(no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost::tilDomene)
                .collect(Collectors.toList());
        }

        Assert.notNull(hentJournalposterTilknyttetSakRequest.arkivsakID(), "HentKjerneJournalpostListe krever en arkivSakID.");
        var hentKjerneJournalpostListeRequest = new HentKjerneJournalpostListeRequest();
        hentKjerneJournalpostListeRequest.getArkivSakListe().add(lagArkivSak(hentJournalposterTilknyttetSakRequest.arkivsakID(), Fagsystem.GSAK_I_JOARK.getKode()));

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
            .peek(j -> j.setSaksnummer(hentJournalposterTilknyttetSakRequest.saksnummer()))
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
        arkivDokument.setDokumentVarianter(lagDokumentVarianter(detaljertDokumentinformasjon.getDokumentInnholdListe()));

        detaljertDokumentinformasjon.getSkannetInnholdListe()
            .forEach(vedlegg -> arkivDokument.getLogiskeVedlegg().add(new LogiskVedlegg(null, vedlegg.getVedleggInnhold())));
        return arkivDokument;
    }

    private List<DokumentVariant> lagDokumentVarianter(List<DokumentInnhold> dokumentInnhold) {
        return dokumentInnhold.stream()
            .map(d -> new DokumentVariant(DokumentVariant.VariantFormat.valueOf(d.getVariantformat().getValue()), true))
            .collect(Collectors.toList());
    }

    @Override
    public String opprettJournalpost(OpprettJournalpost opprettJournalpost, boolean forsøkEndeligJfr) {
        OpprettJournalpostRequest request = OpprettJournalpostRequest.av(opprettJournalpost);
        if (forsøkEndeligJfr) {
            JournalpostRequestValidator.validerJournalpostForEndeligJfr(request);
        }

        return journalpostapiConsumer.opprettJournalpost(request, forsøkEndeligJfr).getJournalpostId();
    }

    @Override
    public void oppdaterJournalpost(String journalpostID, JournalpostOppdatering journalpostOppdatering, boolean forsøkFerdigstill) {

        fjernEksisterendeLogiskeVedleggPåHovddokument(journalpostID);

        OppdaterJournalpostRequest.Builder request = new OppdaterJournalpostRequest.Builder()
            .medDatoMottatt(journalpostOppdatering.getMottattDato())
            .medTittel(journalpostOppdatering.getTittel())
            .medBruker(journalpostOppdatering.getBrukerID())
            .medSaksnummer(journalpostOppdatering.getSaksnummer())
            .medTema(journalpostOppdatering.getTema());

        final String hovedDokumentID = journalpostOppdatering.getHovedDokumentID();
        if (hovedDokumentID != null) {
            if (journalpostOppdatering.harLogiskeVedlegg()) {
                for (String vedleggTittel : journalpostOppdatering.getLogiskeVedleggTitler()) {
                    journalpostapiConsumer.leggTilLogiskVedlegg(hovedDokumentID, vedleggTittel);
                }
            }
            request.leggTilDokumentoppdatering(hovedDokumentID, journalpostOppdatering.getTittel());
        }

        if (journalpostOppdatering.harFysiskeVedlegg()) {
            for (Map.Entry<String, String> vedleggIdMedTittel : journalpostOppdatering.getFysiskeVedlegg().entrySet()) {
                request.leggTilDokumentoppdatering(vedleggIdMedTittel.getKey(), vedleggIdMedTittel.getValue());
            }
        }

        if (journalpostOppdatering.getAvsenderType() != null) {
            AvsenderMottaker avsender = AvsenderMottaker.builder()
                .id(journalpostOppdatering.getAvsenderID())
                .land(journalpostOppdatering.getAvsenderLand())
                .navn(journalpostOppdatering.getAvsenderNavn())
                .idType(AvsenderMottaker.tilAvsenderMottakerIdType(journalpostOppdatering.getAvsenderType()))
                .build();

            request.medAvsender(avsender);
        }
        journalpostapiConsumer.oppdaterJournalpost(request.build(), journalpostID);

        if (forsøkFerdigstill) {
            journalpostapiConsumer.ferdigstillJournalpost(new FerdigstillJournalpostRequest(), journalpostID);
        }
    }

    private void fjernEksisterendeLogiskeVedleggPåHovddokument(String journalpostID) {
        var journalpost = hentJournalpost(journalpostID);
        if (journalpost.getHoveddokument() != null) {
            var hoveddokument = journalpost.getHoveddokument();
            for (var logiskVedlegg : hoveddokument.getLogiskeVedlegg()) {
                journalpostapiConsumer.fjernLogiskeVedlegg(hoveddokument.getDokumentId(), logiskVedlegg.getLogiskVedleggID());
            }
        }
    }
}
