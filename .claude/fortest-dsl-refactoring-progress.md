# forTest DSL Refactoring Progress

## Status: In Progress

## Completed

### Shared Factories (service module)
- [x] `SaksbehandlingDataFactory.kt` - Converted FagsakTestFactory.builder(), Behandlingsresultat().apply, Saksopplysning().apply
- [x] `MottatteOpplysningerStub.kt` - Converted MottatteOpplysninger().apply, Soeknad().apply
- [x] `SaksopplysningStubs.kt` - Converted Saksopplysning().apply, removed mocks
- [x] `DataByggerStubs.kt` - Converted Soeknad().apply, ForetakUtland().apply, PersonDokument().apply

### service/behandling tests
- [x] `AngiBehandlingsresultatServiceTest.kt` - Converted Behandlingsresultat().apply, Medlemskapsperiode().apply
- [x] `ReplikerBehandlingsresultatServiceTest.kt` - Converted all Entity().apply patterns, removed lateinit vars
- [x] `AvsluttArt13BehandlingServiceTest.kt` - Converted Behandlingsresultat().apply, Lovvalgsperiode().apply, removed lateinit vars
- [x] `UtledMottaksdatoTest.kt` - Converted MottatteOpplysninger().apply

### DSL Extensions Added
- `SoeknadTestFactory`: Added `oppholdUtland()`, `maritimtArbeid {}`, `foretakUtland(ForetakUtland)`, `foretakUtlandMedDetaljer {}`, `utenlandskIdent()`, `erSelvstendig`
- `SaksopplysningTestFactory`: Added `kilde {}` for SaksopplysningKilde
- `PersonDokumentTestFactory`: Added `erEgenAnsatt`, `familiemedlem {}`
- `BehandlingsresultatTestFactory`: Added `kontrollresultat {}`
- `BehandlingTestFactory`: Added `behandlingsaarsak {}`
- `MottatteOpplysningerTestFactory`: Added `mottaksdato`

## Remaining (service module)

### service/dokument tests (~25 files)
- [x] BrevDataServiceTest.kt
- [x] TilBrevAdresseServiceTest.kt (Batch 3)
- [x] DokumentServiceTest.kt
- [x] BrevmottakerServiceTest.kt
- [x] HentMuligeBrevmottakereServiceTest.kt - 13 tests
- [x] DokgenTestData.kt - utility file, 50+ dependent tests
- [x] InnvilgelsesbrevMapperTest.kt - 2 tests
- [ ] Various mapper tests (remaining)

### service/ftrl tests (~5 files)
- [x] AvklarteFaktaForBestemmelseTest.kt - 7 tests
- [x] VilkårForBestemmelseIkkeYrkesaktivTest.kt - 14 tests
- [x] VilkårForBestemmelsePensjonistTest.kt - 9 tests
- [x] VilkårForBestemmelseYrkesaktivTest.kt - 21 tests
- [x] UtledMedlemskapsperiodeTest.kt

### service/other tests (~20 files)
- [x] OppfriskSaksopplysningerServiceTest.kt (Batch 4)
- [x] UnntaksregistreringServiceTest.kt (Batch 3)
- [x] UnntaksperiodeServiceTest.kt (Batch 3)
- [x] AnmodningsperiodeServiceTest.kt (Batch 4)
- [x] LovligeKombinasjonerSaksbehandlingServiceTest.kt - 67 tests
- [x] OpprettLovvalgsperiodeServiceTest.kt - 17 tests
- [x] TrygdeavtaleServiceTest.kt - 11 tests
- [ ] And more...

## Anti-patterns to Convert
- `Entity().apply {}` → `Entity.forTest {}` or `entityForTest {}`
- `FagsakTestFactory.builder()...build()` → `Fagsak.forTest {}`
- `lateinit var entity` → local val in each test
- Post-construction mutation → configure in DSL block

## Test Results
- 1018 service tests passing as of last commit

## Batch 3 Completed
- [x] TilBrevAdresseServiceTest.kt - 13 tests
- [x] UnntaksperiodeServiceTest.kt - 10 tests
- [x] UnntaksregistreringServiceTest.kt - 4 tests

## Batch 4 Completed
- [x] OppfriskSaksopplysningerServiceTest.kt - 10 tests
- [x] AnmodningsperiodeServiceTest.kt - 13 tests

## Batch 5 Completed (ftrl/vilkaar tests)
- [x] AvklarteFaktaForBestemmelseTest.kt - 7 tests
- [x] VilkårForBestemmelseIkkeYrkesaktivTest.kt - 14 tests
- [x] VilkårForBestemmelsePensjonistTest.kt - 9 tests
- [x] VilkårForBestemmelseYrkesaktivTest.kt - 21 tests

### DSL Extensions Added (Batch 5)
- `SøknadNorgeEllerUtenforEØSTestFactory`: New factory for SøknadNorgeEllerUtenforEØS with `landkoder()` DSL
- `MottatteOpplysningerTestFactory`: Added `søknadNorgeEllerUtenforEØS {}` extension function

## Batch 6 Completed
- [x] HentMuligeBrevmottakereServiceTest.kt - 13 tests
- [x] DokgenTestData.kt - utility file

## Batch 7 Completed
- [x] InnvilgelsesbrevMapperTest.kt - 2 tests (enhanced MaritimtArbeidBuilder DSL)
- [x] LovligeKombinasjonerSaksbehandlingServiceTest.kt - 67 tests

## Batch 8 Completed
- [x] OpprettLovvalgsperiodeServiceTest.kt - 17 tests (enhanced AnmodningEllerAttestTestFactory with periode())
- [x] TrygdeavtaleServiceTest.kt - 11 tests

## Batch 9 Completed
- [x] A1MapperTest.kt - 25 tests (removed lateinit var, added helper functions)
- [x] AttestMapperTest.kt - 1 test

## Batch 10 Completed
- [x] AvslagYrkesaktivMapperTest.kt - 3 tests
- [x] AvslagArbeidsgiverMapperTest.kt - 2 tests

## Batch 11 Completed
- [x] InnvilgelseFtrlYrkesaktivMapperTest.kt - 14 tests
- [x] InnvilgelseFtrlPensjonistMapperTest.kt - 4 tests

## Next Files to Refactor
- More mapper tests in service/dokument
- Remaining service tests

## Workflow Hints (Avoiding Context Overflow)

When using parallel subagents for batch refactoring:

1. **Small batches**: Run 2-3 subagents max, not 4-5
2. **Commit immediately**: After each batch completes, commit before starting next
3. **Manual compact**: Run `/compact` after each batch, don't rely on autocompact
4. **Short prompts**: Keep subagent task descriptions concise
5. **Check progress file**: Start each session by reading this file to know where to resume

### Recommended Session Flow
```
1. Read progress file
2. Run 2 subagents in parallel
3. Wait for completion
4. Commit changes
5. /compact
6. Repeat or end session
```

This prevents the orchestrator from accumulating too much context from parallel agent results.
