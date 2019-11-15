package no.nav.melosys.integrasjonstest.felles.utils;

import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.service.aktoer.AktoerDto;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;

public final class AktoerTestUtils {
    private AktoerTestUtils() {}

    public static AktoerDto lagAktørBrukerDto(String aktoerID) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setRolleKode(BRUKER.getKode());
        aktoerDto.setAktoerID(aktoerID);
        return aktoerDto;
    }

    public static AktoerDto lagAktørRepresentantDto(String orgnr, Representerer representerer) {
        AktoerDto aktoerDto = new AktoerDto();
        aktoerDto.setRolleKode(REPRESENTANT.getKode());
        aktoerDto.setOrgnr(orgnr);
        aktoerDto.setRepresentererKode(representerer.getKode());
        return aktoerDto;
    }
}
