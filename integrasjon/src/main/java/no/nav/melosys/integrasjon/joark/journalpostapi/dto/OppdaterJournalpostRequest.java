package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.integrasjon.Konstanter;
import org.apache.commons.lang3.StringUtils;

public class OppdaterJournalpostRequest {
    @JsonFormat(pattern = "yyyy-MM-dd")
    public final LocalDate datoMottatt;
    public final String tittel;
    public final String journalfoerendeEnhet;
    public final Bruker bruker;
    public final AvsenderMottaker avsenderMottaker;
    public final List<Dokumentoppdatering> dokumenter;
    public final Sak sak;

    public OppdaterJournalpostRequest(LocalDate datoMottatt,
                                      String tittel,
                                      Bruker bruker,
                                      AvsenderMottaker avsenderMottaker,
                                      List<Dokumentoppdatering> dokumenter,
                                      Sak sak) {
        this.datoMottatt = datoMottatt;
        this.tittel = tittel;
        this.bruker = bruker;
        this.avsenderMottaker = avsenderMottaker;
        this.sak = sak;
        this.dokumenter = dokumenter;
        this.journalfoerendeEnhet = String.valueOf(Konstanter.MELOSYS_ENHET_ID);
    }

     public static class Builder {
         private LocalDate datoMottat;
         private String tittel;
         private Bruker bruker;
         private AvsenderMottaker avsenderMottaker;
         private Sak sak;
         private List<Dokumentoppdatering> dokumenter = new ArrayList<>();

         public Builder() {}

        public Builder leggTilDokumentoppdatering(String dokumentID, String nyTittel) {
            dokumenter.add(new Dokumentoppdatering(dokumentID, nyTittel));
            return this;
        }

        public Builder medDatoMottatt(LocalDate datoMottatt) {
            this.datoMottat = datoMottatt;
            return this;
        }

        public Builder medTittel(String tittel) {
             this.tittel = tittel;
             return this;
        }

        public Builder medBruker(String fnr) {
            if (StringUtils.isNotEmpty(fnr)) {
                bruker = Bruker.builder()
                    .id(fnr).idType(Bruker.BrukerIdType.FNR)
                    .build();
            }
            return this;
        }

        public Builder medAvsender(AvsenderMottaker avsender) {
            this.avsenderMottaker = avsender;
            return this;
        }

        public Builder medArkivsaksnummer(String arkivsaknummer) {
             this.sak = new Sak(arkivsaknummer);
             return this;
        }

        public OppdaterJournalpostRequest build() {
             return new OppdaterJournalpostRequest(datoMottat, tittel, bruker, avsenderMottaker, dokumenter, sak);
        }
    }
}