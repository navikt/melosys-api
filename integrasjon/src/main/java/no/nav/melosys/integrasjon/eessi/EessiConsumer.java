package no.nav.melosys.integrasjon.eessi;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import org.springframework.retry.annotation.Retryable;

@Retryable
public interface EessiConsumer {

    OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto,
                                  Collection<Vedlegg> vedlegg,
                                  BucType bucType,
                                  boolean forsøkSend,
                                  boolean oppdaterEksisterendeOmFinnes);

    void sendSedPåEksisterendeBuc(SedDataDto sedDataDto,
                                  String rinaSaksnummer,
                                  SedType sedType);

    List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer,
                                              List<String> statuser);

    List<Institusjon> hentMottakerinstitusjoner(String bucType,
                                                Collection<String> landkode);

    MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID);

    void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto);

    List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer);

    byte[] genererSedPdf(SedDataDto sedDataDto, SedType sedType);

    SedGrunnlagDto hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID);

    void lukkBuc(String rinaSaksnummer);

    List<String> hentMuligeAksjoner(String rinaSaksnummer);
}
