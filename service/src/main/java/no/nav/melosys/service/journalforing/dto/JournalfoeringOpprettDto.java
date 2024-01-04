package no.nav.melosys.service.journalforing.dto;


import no.nav.melosys.saksflytapi.journalfoering.Fagsak;
import no.nav.melosys.saksflytapi.journalfoering.JournalfoeringOpprettRequest;
import no.nav.melosys.saksflytapi.journalfoering.Periode;
import no.nav.melosys.saksflytapi.journalfoering.Soeknadsland;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;

public class JournalfoeringOpprettDto extends JournalfoeringDto {
    private FagsakDto fagsak;
    @Deprecated(since = "Fjernes snarest med tilfølgende kode. Sendes aldri fra frontend")
    private String arbeidsgiverID;

    public FagsakDto getFagsak() {
        return fagsak;
    }

    public void setFagsak(FagsakDto fagsak) {
        this.fagsak = fagsak;
    }

    public String getArbeidsgiverID() {
        return arbeidsgiverID;
    }

    public void setArbeidsgiverID(String arbeidsgiverID) {
        this.arbeidsgiverID = arbeidsgiverID;
    }

    public JournalfoeringOpprettRequest tilJournalfoeringOpprettRequest() {
        return new JournalfoeringOpprettRequest(
            journalpostID,
            oppgaveID,
            brukerID,
            virksomhetOrgnr,
            avsenderID,
            avsenderNavn,
            avsenderType,
            hoveddokument.tilDokumentRequest(),
            vedlegg.stream().map(DokumentDto::tilDokumentRequest).toList(),
            skalTilordnes,
            mottattDato,
            forvaltningsmeldingMottaker,
            behandlingstemaKode,
            behandlingstypeKode,
            new Fagsak(fagsak.getSakstema(), fagsak.getSakstype(),
                getSoknadsperiode(), getLand()),
            arbeidsgiverID
        );
    }

    private Soeknadsland getLand() {
        SoeknadslandDto land = fagsak.getLand();
        return new Soeknadsland(land.getLandkoder(), land.erUkjenteEllerAlleEosLand());
    }

    private Periode getSoknadsperiode() {
        PeriodeDto soknadsperiode = fagsak.getSoknadsperiode();
        return new Periode(soknadsperiode.getFom(), soknadsperiode.getTom());
    }
}
