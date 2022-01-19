package stub;

import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.integrasjon.eessi.EessiConsumer;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EessiConsumerStub implements EessiConsumer {
    @Override
    public OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, Collection<Vedlegg> vedlegg, BucType bucType, boolean forsøkSend, boolean oppdaterEksisterendeOmFinnes) {
        return null;
    }

    @Override
    public void sendSedPåEksisterendeBuc(SedDataDto sedDataDto, String rinaSaksnummer, SedType sedType) {

    }

    @Override
    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) {
        return null;
    }

    @Override
    public List<Institusjon> hentMottakerinstitusjoner(String bucType, Collection<String> landkode) {
        return null;
    }

    @Override
    public MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) {
        return null;
    }

    @Override
    public void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) {

    }

    @Override
    public List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) {
        SaksrelasjonDto saksrelasjonDto = new SaksrelasjonDto(161L, rinaSaksnummer, "LA_BUC_04");
        return Collections.singletonList(saksrelasjonDto);
    }

    @Override
    public byte[] genererSedPdf(SedDataDto sedDataDto, SedType sedType) {
        return new byte[0];
    }

    @Override
    public SedGrunnlagDto hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) {
        return null;
    }

    @Override
    public void lukkBuc(String rinaSaksnummer) {

    }
}
