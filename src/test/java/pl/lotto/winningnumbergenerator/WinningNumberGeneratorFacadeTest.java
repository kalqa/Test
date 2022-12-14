package pl.lotto.winningnumbergenerator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.lotto.numberreceiver.MockedTimeGeneratorFacade;
import pl.lotto.timegenerator.TimeGeneratorFacade;
import pl.lotto.winningnumbergenerator.dto.WinNumberStatus;
import pl.lotto.winningnumbergenerator.dto.WinningNumbersDto;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class WinningNumberGeneratorFacadeTest implements MockedTimeGeneratorFacade, MockedRandomGenerator {

    private final WinningPropertyConfigurable winningConfig = new WinningPropertyPropertyConfigTest(1,99,6);
    private final RandomNumbersGenerator randomNumbersGenerator = new RandomNumbersGenerator(winningConfig);
    private final WinningNumberRepository winningNumberRepository = new WinningNumberRepositoryInMemory();
    private final TimeGeneratorFacade mockedTimeGeneratorFacade = createMockedTimeGeneratorFacadeWithDefaultDates();
    private final WinningNumberGeneratorConfiguration winningNumberGeneratorConfig = new WinningNumberGeneratorConfiguration();
    private final WinningNumberGeneratorFacade winningNumberGeneratorFacade = winningNumberGeneratorConfig.createForTests(randomNumbersGenerator, mockedTimeGeneratorFacade, winningNumberRepository);

    @AfterEach
    void tearDown() {
        resetTimeFacadeToDefaultDates(mockedTimeGeneratorFacade);
    }

    @Test
    @DisplayName("should return winning numbers when draw date is provided")
    void getWinningNumbersForDate_givenDrawDate_shouldReturnWinningNumbers() {
        // given
        List<Integer> expectedWinningNumbers = List.of(1, 2, 3, 4, 5, 6);
        RandomNumbersGenerator mockedRandomNumbersGenerator = getMockedNumberGenerator(expectedWinningNumbers);
        WinningNumberGeneratorFacade winningNumberGeneratorFacade = winningNumberGeneratorConfig
                .createForTests(mockedRandomNumbersGenerator, mockedTimeGeneratorFacade, winningNumberRepository);
        winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);

        // when
        WinningNumbersDto actualWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(sampleDrawDate);

        // then
        assertThat(actualWinningNumbersDto).isNotNull();
        assertThat(actualWinningNumbersDto.drawDate()).isEqualTo(sampleDrawDate);
        assertThat(actualWinningNumbersDto.winningNumbers()).isEqualTo(expectedWinningNumbers);
        assertThat(actualWinningNumbersDto.status()).isEqualTo(WinNumberStatus.SAVED);

    }

    @Test
    @DisplayName("should return not found dto when draw date is provided which is not in the database")
    void getWinningNumbersForDate_givenInvalidDrawDate_shouldReturnNotFound(){
        // given
        winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);

        // when
        WinningNumbersDto actualWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(LocalDateTime.of(1900,1,1,1,1,1));

        // then
        assertThat(actualWinningNumbersDto).isNotNull();
        assertThat(actualWinningNumbersDto.status()).isEqualTo(WinNumberStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should return the same generated winning numbers when numbers was previously drawn before but they are still valid")
    void getLastWinningNumbers_givenCurrentDateBeforeDrawDateNumbersWereGeneratedBefore_returnsWinningNumbers() {
        // given
        boolean statusOfFirstRun = winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);
        WinningNumbersDto previousWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(sampleDrawDate);
        LocalDateTime dayLater = sampleCurrentDateTime.plusDays(1);
        when(mockedTimeGeneratorFacade.getCurrentDateAndTime()).thenReturn(dayLater);
        boolean statusOfSecondRun = winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);

        // when
        WinningNumbersDto actualWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(sampleDrawDate);

        // then
        assertThat(previousWinningNumbersDto).isEqualTo(actualWinningNumbersDto);
        assertThat(actualWinningNumbersDto.status()).isEqualTo(WinNumberStatus.SAVED);
        assertThat(statusOfFirstRun).isTrue();
        assertThat(statusOfSecondRun).isFalse();

    }

    @Test
    @DisplayName("should generate new winning numbers when given date is after a draw date")
    void getLastWinningNumbers_givenCurrentDateAfterDrawDateNumbersWereGeneratedBefore_returnsNewWinningNumbers() {
        // given
        winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);
        WinningNumbersDto previousWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(sampleDrawDate);
        // advancing in time by 10 days
        LocalDateTime tenDaysLater = sampleCurrentDateTime.plusDays(10);
        LocalDateTime laterDrawDate = sampleDrawDate.plusDays(14);
        when(mockedTimeGeneratorFacade.getCurrentDateAndTime()).thenReturn(tenDaysLater);
        when(mockedTimeGeneratorFacade.getDrawDateAndTime()).thenReturn(laterDrawDate);
        winningNumberGeneratorFacade.generateWinningNumbers(laterDrawDate);

        // when
        WinningNumbersDto actualWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(laterDrawDate);

        // then
        assertThat(actualWinningNumbersDto).isNotEqualTo(previousWinningNumbersDto);
        assertThat(actualWinningNumbersDto.status()).isEqualTo(WinNumberStatus.SAVED);

    }

    @Test
    @DisplayName("should return not found dto when winning number repository is empty")
    void getLastWinningNumbers_givenDrawDate_shouldReturnNotFoundDto(){
        // given
        // when
        WinningNumbersDto actualWinningNumbersDto = winningNumberGeneratorFacade.getWinningNumbersForDate(sampleDrawDate);

        // then
        assertThat(actualWinningNumbersDto).isNotNull();
        assertThat(actualWinningNumbersDto.status()).isEqualTo(WinNumberStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should remove winning numbers from repository for a specified draw date when draw date is provided")
    void deleteWinningNumbersForDate_givenDrawDate_shouldRemoveWinningNumbersFromDb() {
        // given
        winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);

        // when
        WinningNumbersDto actualDeletedListOfWinningNumbers = winningNumberGeneratorFacade.deleteWinningNumbersForDate(sampleDrawDate);
        List<WinningNumbersDto> actualRemainingListOfWinningNumbers = winningNumberGeneratorFacade.getAllWinningNumbers();

        // then
        assertThat(actualDeletedListOfWinningNumbers).isNotNull();
        assertThat(actualRemainingListOfWinningNumbers).isNotNull();
        assertThat(actualDeletedListOfWinningNumbers.status()).isEqualTo(WinNumberStatus.DELETED);
        assertThat(actualRemainingListOfWinningNumbers).isEmpty();
    }

    @Test
    @DisplayName("should return all winning number for each draw date")
    void getAllWinningNumbers_shouldReturnAllWinningNumbersForEachDrawDate(){
        // given
        winningNumberGeneratorFacade.generateWinningNumbers(sampleDrawDate);

        LocalDateTime oneWeekLaterSampleTime = sampleCurrentDateTime.plusDays(7);
        LocalDateTime oneWeekLaterDrawTime = sampleDrawDate.plusDays(7);
        when(mockedTimeGeneratorFacade.getCurrentDateAndTime()).thenReturn(oneWeekLaterSampleTime);
        when(mockedTimeGeneratorFacade.getDrawDateAndTime()).thenReturn(oneWeekLaterDrawTime);
        winningNumberGeneratorFacade.generateWinningNumbers(oneWeekLaterDrawTime);

        LocalDateTime twoWeekLaterSampleTime = sampleCurrentDateTime.plusDays(14);
        LocalDateTime twoWeekLaterDrawTime = sampleDrawDate.plusDays(14);
        when(mockedTimeGeneratorFacade.getCurrentDateAndTime()).thenReturn(twoWeekLaterSampleTime);
        when(mockedTimeGeneratorFacade.getDrawDateAndTime()).thenReturn(twoWeekLaterDrawTime);
        winningNumberGeneratorFacade.generateWinningNumbers(twoWeekLaterDrawTime);

        // when
        List<WinningNumbersDto> allWinningNumbersDto = winningNumberGeneratorFacade.getAllWinningNumbers();

        // then
        int expectedWinningNumberSets = 3;
        assertThat(allWinningNumbersDto).isNotNull();
        assertThat(allWinningNumbersDto).hasSize(expectedWinningNumberSets);
        assertThat(allWinningNumbersDto).allMatch(winningDto -> winningDto.status() == WinNumberStatus.SAVED);
    }

    @Test
    @DisplayName("should return not found dto when there are no winning results drawn yet")
    void getAllWinningNumbers_shouldReturnNotFoundDtoIfNoWinningNumbersAreStoredInRepository(){
        // given
        // when
        List<WinningNumbersDto> allWinningNumbersDto = winningNumberGeneratorFacade.getAllWinningNumbers();

        // then
        assertThat(allWinningNumbersDto).isNotNull();
        assertThat(allWinningNumbersDto).isEmpty();
    }

}
