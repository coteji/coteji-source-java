package org.example.tests.datetime;

public class DateTimeTest {

    @Test(groups = { "datetime", "smoke" })
    @UserStories("COT-10")
    @TestCase("COT-110")
    public void currentDate() {
        NavigationSteps.openRemindersApp();
        DateTimeSteps.checkCurrentDate();
    }

    @Test(groups = { "datetime" })
    @UserStories("COT-10")
    public void currentTime() {
        NavigationSteps.openRemindersApp();
        DateTimeSteps.checkCurrentTimeWithPrecisionInMinutes(2);
    }
}
