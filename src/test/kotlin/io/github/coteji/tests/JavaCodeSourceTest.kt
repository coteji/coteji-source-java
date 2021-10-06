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
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Files

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
                currentTimeTest
            )
    }

    @ParameterizedTest
    @MethodSource("searchCriteriaData")
    fun testGetTestsBySearchCriteria(searchCriteria: String, expectedTests: List<CotejiTest>) {
        assertThat(javaCodeSource.getTests(searchCriteria))
            .containsExactlyInAnyOrderElementsOf(expectedTests)
    }

    private fun searchCriteriaData(): List<Arguments> {
        return listOf(
            Arguments.of("+method:RemindersTest.deleteReminder", listOf(deleteReminderTest)),
            Arguments.of(
                "-method:RemindersTest.deleteReminder",
                listOf(createReminderTest, currentDateTest, currentTimeTest)
            ),
            Arguments.of(
                "+method:RemindersTest.createReminder +method:RemindersTest.deleteReminder",
                listOf(createReminderTest, deleteReminderTest)
            ),
            Arguments.of("+class:DateTimeTest", listOf(currentDateTest, currentTimeTest)),
            Arguments.of(
                "+class:DateTimeTest +class:RemindersTest",
                listOf(createReminderTest, deleteReminderTest, currentDateTest, currentTimeTest)
            ),
            Arguments.of("-class:DateTimeTest", listOf(createReminderTest, deleteReminderTest)),
            Arguments.of(
                "+class:DateTimeTest +class:RemindersTest -method:RemindersTest.deleteReminder",
                listOf(createReminderTest, currentDateTest, currentTimeTest)
            ),
            Arguments.of(
                "+class:DateTimeTest +method:RemindersTest.deleteReminder",
                listOf(deleteReminderTest, currentDateTest, currentTimeTest)
            ),
            Arguments.of(
                "+package:org.example.tests",
                listOf(createReminderTest, deleteReminderTest, currentDateTest, currentTimeTest)
            ),
            Arguments.of(
                "+package:org.example.tests -package:org.example.tests.datetime",
                listOf(createReminderTest, deleteReminderTest)
            ),
            Arguments.of("-package:org.example.tests.datetime", listOf(createReminderTest, deleteReminderTest)),
            Arguments.of("+package:org.example.tests -class:RemindersTest", listOf(currentDateTest, currentTimeTest)),
            Arguments.of("+annotationName:TestCase", listOf(createReminderTest, deleteReminderTest, currentDateTest)),
            Arguments.of("-annotationName:TestCase", listOf(currentTimeTest)),
            Arguments.of("+annotationValue:UserStories,\"COT-10\"", listOf(currentDateTest, currentTimeTest)),
            Arguments.of(
                "+annotationValueContains:UserStories,COT-1",
                listOf(createReminderTest, currentDateTest, currentTimeTest)
            ),
            Arguments.of(
                "+annotationAttributeValue:Test,dataProvider,\"createReminderData\"",
                listOf(createReminderTest)
            ),
            Arguments.of(
                "+annotationAttributeValueContains:Test,groups,smoke",
                listOf(createReminderTest, currentDateTest)
            ),

            )
    }

    @ParameterizedTest
    @MethodSource("searchCriteriaNegativeData")
    fun testGetTestsBySearchCriteriaNegative(searchCriteria: String, expectedMessage: String) {
        val exception = assertThrows<RuntimeException> {
            javaCodeSource.getTests(searchCriteria)
        }
        assertThat(exception.message).isEqualTo(expectedMessage)
    }

    private fun searchCriteriaNegativeData(): List<Arguments> {
        return listOf(
            Arguments.of("+annotationFoo:Bar", "Annotation condition not recognized"),
            Arguments.of("+annotationAttributeValue:a,b", "Condition value should contain 3 values separated by comma"),
            Arguments.of(
                "+annotationAttributeValueContains:a,b,c,d",
                "Condition value should contain 3 values separated by comma"
            )
        )
    }

    @Test
    fun testUpdateTestId() {
        val file1 = File("src/test/resources/org/example/tests/Test1.java")
        val file2 = File("src/test/resources/org/example/tests/Test2.java")
        Files.write(
            file1.toPath(), """
            package org.example.tests;
            public class Test1 {
                @Test
                public void someTest() {
                    NavigationSteps.openApp();
                    SomeSteps.doSomething();
                }
            }
        """.trimIndent().toByteArray()
        )
        Files.write(
            file2.toPath(), """
            package org.example.tests;
            public class Test2 {
                @Test
                @TestCase("COT-100")
                public void anotherTest() {
                    NavigationSteps.openApp();
                    SomeSteps.doSomethingElse();
                }
            }
        """.trimIndent().toByteArray()
        )
        val test1 = CotejiTest(
            name = "[TEST] Some Test",
            id = "COT-123",
            content = "Open App\nDo Something",
            attributes = mapOf()
        )
        val test2 = CotejiTest(
            name = "[TEST] Another Test",
            id = "COT-124",
            content = "Open App\nDo Something Else",
            attributes = mapOf()
        )
        try {
            javaCodeSource.updateIdentifiers(listOf(test1, test2))
            assertThat(file1).hasContent(
                """
            package org.example.tests;
            
            public class Test1 {
            
                @Test
                @TestCase("COT-123")
                public void someTest() {
                    NavigationSteps.openApp();
                    SomeSteps.doSomething();
                }
            }
        """.trimIndent()
            )
            assertThat(file2).hasContent(
                """
            package org.example.tests;
            
            public class Test2 {
            
                @Test
                @TestCase("COT-124")
                public void anotherTest() {
                    NavigationSteps.openApp();
                    SomeSteps.doSomethingElse();
                }
            }
        """.trimIndent()
            )
        } finally {
            Files.delete(file1.toPath())
            Files.delete(file2.toPath())
        }
    }

    @Test
    fun testParametersDefaultValues() {
        assertThat(JavaCodeSource(testsDir = "src/test/resources/org/example/tests")).isNotNull
    }

    @Test
    fun testEmptyBody() {
        val source = JavaCodeSource(testsDir = "src/test/resources/org/example/negative/emptybody")
        assertThrowsExactly(RuntimeException::class.java) { source.getAll() }
    }
}
