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

package io.github.coteji.sources

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.utils.SourceRoot
import io.github.coteji.core.Coteji
import io.github.coteji.core.TestsSource
import io.github.coteji.extensions.getAnnotationValue
import io.github.coteji.extensions.hasAnnotation
import io.github.coteji.filter.TestsFilter
import io.github.coteji.model.CotejiTest
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class JavaCodeSource : TestsSource {

    // root directory path where your tests reside (mandatory)
    lateinit var testsDir: String
    // function that defines what method is considered a test (default: methods with @Test annotation)
    var isTest: MethodDeclaration.() -> Boolean = { this.hasAnnotation("Test") }
    // annotation name where you keep the test ID from the target TMS (default: "TestCase")
    var testIdAnnotationName: String = "TestCase"
    // function that maps method to the test name in the target TMS (default: method name)
    var getTestName: MethodDeclaration.() -> String = { this.nameAsString }
    // function that transforms every statement from Java method for the target TMS (default: line as is)
    var lineTransform: String.() -> String = { this }
    // function that maps method to the map of test attributes. See the required attributes in the target package docs
    var getAttributes: MethodDeclaration.() -> Map<String, Any> = { HashMap() }

    override fun getAll(): List<CotejiTest> {
        validateMandatoryParameters()
        val result = arrayListOf<CotejiTest>()
        val packagePath = File(testsDir).toPath()
        findJavaFiles(packagePath)
            .forEach { javaFile ->
                javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                    if (method.isTest()) {
                        result.add(method.toCotejiTest())
                    }
                }
            }
        return result
    }

    override fun getTests(query: String): List<CotejiTest> {
        validateMandatoryParameters()
        val filter = TestsFilter(query)
        val result = arrayListOf<CotejiTest>()
        val packagePath = File(testsDir).toPath()
        if (filter.hasOnlyPackagesAndClasses()) {
            findJavaFiles(packagePath)
                .forEach { javaFile ->
                    if (filter.classIncluded(javaFile)) {
                        javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                            if (method.isTest()) {
                                result.add(method.toCotejiTest())
                            }
                        }
                    }
                }
        } else if (filter.hasOnlyMethods()) {
            findJavaFiles(packagePath)
                .forEach { javaFile ->
                    if (filter.classIncluded(javaFile)) {
                        javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                            if (method.isTest() && filter.methodIncluded(method)) {
                                result.add(method.toCotejiTest())
                            }
                        }
                    }
                }
        } else {
            findJavaFiles(packagePath)
                .forEach { javaFile ->
                    javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                        if (method.isTest() && filter.methodIncluded(method)) {
                            result.add(method.toCotejiTest())
                        }
                    }
                }
        }
        return result
    }

    override fun updateIdentifiers(tests: List<CotejiTest>) {
        validateMandatoryParameters()
        val packagePath = File(testsDir).toPath()
        findJavaFiles(packagePath)
            .forEach { javaFile ->
                var fileChanged = false
                javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                    if (method.isTest()) {
                        val currentTest = method.toCotejiTest()
                        val test = tests.find { it.name == currentTest.name && it.content == currentTest.content }
                        if (test != null) {
                            method.annotations.find { it.nameAsString == testIdAnnotationName }?.remove()
                            method.addSingleMemberAnnotation(testIdAnnotationName, "\"${test.id}\"")
                            fileChanged = true
                        }
                    }
                }
                if (fileChanged) {
                    Files.write(javaFile.storage.get().path, javaFile.toString().toByteArray())
                }
            }
    }

    private fun findJavaFiles(packagePath: Path) = SourceRoot(packagePath)
        .tryToParse("")
        .filter { it.isSuccessful }
        .map { it.result.get() }

    private fun validateMandatoryParameters() {
        if (!this::testsDir.isInitialized) {
            throw IllegalArgumentException("testsDir property cannot be empty")
        }
    }

    private fun MethodDeclaration.toCotejiTest(): CotejiTest {
        val statements = this.body
            .orElseThrow { RuntimeException("Method ${this.nameAsString} has no body") }
            .statements
        val content = statements.joinToString("\n") { it.toString().lineTransform() }
        return CotejiTest(
            name = this.getTestName(),
            id = this.getAnnotationValue(testIdAnnotationName),
            content = content,
            attributes = this.getAttributes()
        )
    }
}

infix fun Coteji.javaCodeSource(init: JavaCodeSource.() -> Unit): JavaCodeSource {
    val source = JavaCodeSource()
    source.init()
    return source
}
