package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiClient;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import no.nav.melosys.integrasjon.joark.saf.SafClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Primary
public class JoarkService implements JoarkFasade {
    private final JournalpostapiClient journalpostapiClient;
    private final SafClient safClient;

    public JoarkService(JournalpostapiClient journalpostapiClient, SafClient safClient) {
        this.journalpostapiClient = journalpostapiClient;
        this.safClient = safClient;
    }

    @Override
    public void ferdigstillJournalføring(String journalpostId) {
        FerdigstillJournalpostRequest request = new FerdigstillJournalpostRequest();
        journalpostapiClient.ferdigstillJournalpost(request, journalpostId);
    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) {
        return safClient.hentDokument(journalPostID, dokumentID);
    }

    @Override
    public Journalpost hentJournalpost(String journalpostID) {
        return safClient.hentJournalpost(journalpostID).tilDomene();
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
                throw new FunksjonellException("Journalpost med ID %s tilhører ikke sak %s".formatted(
                    dokumentReferanse.getJournalpostID(),
                    hentJournalposterTilknyttetSakRequest.saksnummer()));
            }

            journalposterTilknyttetSak.get(dokumentReferanse.getJournalpostID()).finnArkivDokument(dokumentReferanse.getDokumentID())
                .ifPresentOrElse(
                    arkivDokument -> {
                        if (!arkivDokument.harTilgangTilArkivVariant())
                            throw new SikkerhetsbegrensningException("Ikke tilgang til arkivdokument %s i journalpost %s".formatted(
                                dokumentReferanse.getDokumentID(),
                                dokumentReferanse.getJournalpostID()
                            ));
                    },
                    () -> {
                        throw new IkkeFunnetException("Finner ikke dokument med id %s for journalpost %s".formatted(
                            dokumentReferanse.getDokumentID(),
                            dokumentReferanse.getJournalpostID()));
                    });
        }
    }

    @Override
    public LocalDate hentMottaksDatoForJournalpost(String journalpostID) {
        return Optional.ofNullable(hentJournalpost(journalpostID).getForsendelseMottatt())
            .map(forsendelseMottattDato -> LocalDate.ofInstant(forsendelseMottattDato, ZoneId.systemDefault()))
            .orElse(null);
    }

    @Override
    public List<Journalpost> hentJournalposterTilknyttetSak(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest) {
        return safClient.hentDokumentoversikt(hentJournalposterTilknyttetSakRequest.saksnummer())
            .stream()
            .map(no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost::tilDomene)
            .toList();

    }

    @Override
    public String opprettJournalpost(OpprettJournalpost opprettJournalpost, boolean forsøkEndeligJfr) {
        OpprettJournalpostRequest request = OpprettJournalpostRequest.av(opprettJournalpost);
        if (forsøkEndeligJfr) {
            JournalpostRequestValidator.validerJournalpostForEndeligJfr(request);
        }

        return journalpostapiClient.opprettJournalpost(request, forsøkEndeligJfr).getJournalpostId();
    }

    @Override
    public void oppdaterOgFerdigstillJournalpost(String journalpostID, JournalpostOppdatering journalpostOppdatering) {
        fjernEksisterendeLogiskeVedleggPåHoveddokument(journalpostID);

        OppdaterJournalpostRequest.Builder request = new OppdaterJournalpostRequest.Builder()
            .medDatoMottatt(journalpostOppdatering.getMottattDato())
            .medTittel(journalpostOppdatering.getTittel())
            .medSaksnummer(journalpostOppdatering.getSaksnummer())
            .medTema(journalpostOppdatering.getTema());

        if (StringUtils.isNotEmpty(journalpostOppdatering.getBrukerID())) {
            request.medBruker(journalpostOppdatering.getBrukerID());
        } else if (StringUtils.isNotEmpty(journalpostOppdatering.getVirksomhetOrgnr())) {
            request.medBruker(journalpostOppdatering.getVirksomhetOrgnr(), Bruker.BrukerIdType.ORGNR);
        }

        final String hovedDokumentID = journalpostOppdatering.getHovedDokumentID();
        if (hovedDokumentID != null) {
            if (journalpostOppdatering.harLogiskeVedlegg()) {
                for (String vedleggTittel : journalpostOppdatering.getLogiskeVedleggTitler()) {
                    journalpostapiClient.leggTilLogiskVedlegg(hovedDokumentID, vedleggTittel);
                }
            }
            request.leggTilDokumentoppdatering(hovedDokumentID, journalpostOppdatering.getTittel());
        }

        if (journalpostOppdatering.harFysiskeVedlegg()) {
            for (Map.Entry<String, String> vedleggIdMedTittel : journalpostOppdatering.getFysiskeVedlegg().entrySet()) {
                request.leggTilDokumentoppdatering(vedleggIdMedTittel.getKey(), vedleggIdMedTittel.getValue());
            }
        }

        if (harAvsenderMottakerFelt(journalpostOppdatering)) {
            AvsenderMottaker avsender = AvsenderMottaker.builder()
                .id(journalpostOppdatering.getAvsenderID())
                .land(journalpostOppdatering.getAvsenderLand())
                .navn(journalpostOppdatering.getAvsenderNavn())
                .idType(AvsenderMottaker.tilAvsenderMottakerIdType(journalpostOppdatering.getAvsenderType()))
                .build();
            request.medAvsender(avsender);
        }

        journalpostapiClient.oppdaterJournalpost(request.build(), journalpostID);
        journalpostapiClient.ferdigstillJournalpost(new FerdigstillJournalpostRequest(), journalpostID);
    }

    private boolean harAvsenderMottakerFelt(JournalpostOppdatering journalpostOppdatering) {
        return journalpostOppdatering.getAvsenderID() != null
            || journalpostOppdatering.getAvsenderNavn() != null
            || journalpostOppdatering.getAvsenderLand() != null
            || journalpostOppdatering.getAvsenderType() != null;
    }

    private void fjernEksisterendeLogiskeVedleggPåHoveddokument(String journalpostID) {
        var journalpost = hentJournalpost(journalpostID);
        if (journalpost.getHoveddokument() != null) {
            var hoveddokument = journalpost.getHoveddokument();
            for (var logiskVedlegg : hoveddokument.getLogiskeVedlegg()) {
                journalpostapiClient.fjernLogiskeVedlegg(hoveddokument.getDokumentId(), logiskVedlegg.logiskVedleggID());
            }
        }
    }
}
