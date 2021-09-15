package org.example.tests;

public class RemindersTest {
    @Test(dataProvider = "createReminderData")
    @UserStories({"COT-1"})
    @TestCase("COT-101")
    public void createReminder(Reminder reminder) {
        NavigationSteps.openRemindersApp();
        ReminderSteps.addReminder(reminder);
        ReminderSteps.checkLastReminder(reminder);
    }

    @Test(dataProvider = "deleteReminderData")
    @UserStories({"COT-2"})
    @TestCase("COT-102")
    public void deleteReminder(Reminder reminder) {
        NavigationSteps.openRemindersApp();
        ReminderSteps.addReminder(reminder);
        ReminderSteps.deleteLastReminder();
        CommonUiSteps.refreshPage();
        ReminderSteps.checkReminderIsAbsent(reminder);
    }
}