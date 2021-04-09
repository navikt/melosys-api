package no.nav.melosys.service.dokument;

import java.util.List;

public record MuligeMottakereDto(
    MuligMottakerDto hovedMottaker,
    List<MuligMottakerDto> kopiMottakere,
    List<MuligMottakerDto> fasteMottakere) {

    public MuligMottakerDto getHovedMottaker() {
        return hovedMottaker;
    }

    public List<MuligMottakerDto> getKopiMottakere() {
        return kopiMottakere;
    }

    public List<MuligMottakerDto> getFasteMottakere() {
        return fasteMottakere;
    }
}
