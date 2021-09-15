/*
 *    Copyright (c) 2020 - 2021 Coteji AUTHORS.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.coteji.tests

import io.github.coteji.extensions.separateByUpperCaseLetters
import io.github.coteji.model.CotejiTest
import io.github.coteji.sources.JavaCodeSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaCodeSourceTest {
    private lateinit var javaCodeSource: JavaCodeSource
    private val createReminderTest: CotejiTest = CotejiTest(
            name = "[TEST] Create Reminder",
            id = "COT-101",
            content = "Open Reminders App\nAdd Reminder [ reminder ]\nCheck Last Reminder [ reminder ]",
            attributes = mapOf()
    )
    private val deleteReminderTest: CotejiTest = CotejiTest(
            name = "[TEST] Delete Reminder",
            id = "COT-102",
            content = "Open Reminders App\nAdd Reminder [ reminder ]\nDelete Last Reminder\nRefresh Page\nCheck Reminder Is Absent [ reminder ]",
            attributes = mapOf()
    )
    private val currentDateTest: CotejiTest = CotejiTest(
            name = "[TEST] Current Date",
            id = "COT-110",
            content = "Open Reminders App\nCheck Current Date",
            attributes = mapOf()
    )
    private val currentTimeTest: CotejiTest = CotejiTest(
            name = "[TEST] Current Time",
            id = "COT-111",
            content = "Open Reminders App\nCheck Current Time With Precision In Minutes [ 2 ]",
            attributes = mapOf()
    )

    @BeforeAll
    fun setUp() {
        javaCodeSource = JavaCodeSource(
                testsDir = "src/test/resources/org/example/tests",
                getTestName = { "[TEST] " + this.nameAsString.separateByUpperCaseLetters() },
                lineTransform = {
                    this.substringAfter(".")
                            .separateByUpperCaseLetters()
                            .replace("();", "")
                            .replace("(", " [ ")
                            .replace(");", " ]")
                }
        )
    }

    @Test
    fun testGetAll() {
        val actualTests = javaCodeSource.getAll()
        assertThat(actualTests)
                .containsExactlyInAnyOrder(
                        createReminderTest,
                        deleteReminderTest,
                        currentDateTest,
                        currentTimeTest)
    }
}
