package org.example.tests.datetime;

public class DateTimeTest {
    @Test
    @UserStories({"COT-10"})
    @TestCase("COT-110")
    public void currentDate() {
        NavigationSteps.openRemindersApp();
        DateTimeSteps.checkCurrentDate();
    }

    @Test
    @UserStories({"COT-10"})
    @TestCase("COT-111")
    public void currentTime() {
        NavigationSteps.openRemindersApp();
        DateTimeSteps.checkCurrentTimeWithPrecisionInMinutes(2);
    }
}