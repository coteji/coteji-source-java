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
import io.github.coteji.core.TestsSource
import io.github.coteji.extensions.getAnnotationValue
import io.github.coteji.extensions.hasAnnotation
import io.github.coteji.filter.TestsFilter
import io.github.coteji.model.CotejiTest
import java.io.File
import java.nio.file.Files

class JavaCodeSource(
        private val testsDir: String,
        private val isTest: MethodDeclaration.() -> Boolean = { this.hasAnnotation("Test") },
        private val testIdAnnotationName: String = "TestCase",
        private val getTestName: MethodDeclaration.() -> String = { this.nameAsString },
        private val lineTransform: String.() -> String = { this },
        private val getAttributes: MethodDeclaration.() -> Map<String, Any> = { HashMap() },
) : TestsSource {

    override fun getAll(): List<CotejiTest> {
        val result = arrayListOf<CotejiTest>()
        val packagePath = File(testsDir).toPath()
        SourceRoot(packagePath)
                .tryToParse("")
                .filter { it.isSuccessful }
                .map { it.result.get() }
                .forEach { javaFile ->
                    javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                        if (method.isTest()) {
                            result.add(parseMethod(method))
                        }
                    }
                }
        return result
    }

    override fun getTests(searchCriteria: String): List<CotejiTest> {
        val filter = TestsFilter(searchCriteria)
        val result = arrayListOf<CotejiTest>()
        val packagePath = File(testsDir).toPath()
        if (filter.hasOnlyPackagesAndClasses()) {
            SourceRoot(packagePath)
                    .tryToParse("")
                    .filter { it.isSuccessful }
                    .map { it.result.get() }
                    .forEach { javaFile ->
                        if (filter.classIncluded(javaFile)) {
                            javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                                result.add(parseMethod(method))
                            }
                        }
                    }
        } else if (filter.hasOnlyMethods()) {
            SourceRoot(packagePath)
                    .tryToParse("")
                    .filter { it.isSuccessful }
                    .map { it.result.get() }
                    .forEach { javaFile ->
                        if (filter.classIncluded(javaFile)) {
                            javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                                if (filter.methodIncluded(method)) {
                                    result.add(parseMethod(method))
                                }
                            }
                        }
                    }
        } else {
            SourceRoot(packagePath)
                    .tryToParse("")
                    .filter { it.isSuccessful }
                    .map { it.result.get() }
                    .forEach { javaFile ->
                        javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                            if (filter.methodIncluded(method)) {
                                result.add(parseMethod(method))
                            }
                        }
                    }
        }
        return result
    }

    override fun updateIdentifiers(tests: List<CotejiTest>) {
        val packagePath = File(testsDir).toPath()
        SourceRoot(packagePath)
                .tryToParse("")
                .filter { it.isSuccessful }
                .map { it.result.get() }
                .forEach { javaFile ->
                    var fileChanged = false
                    javaFile.findAll(MethodDeclaration::class.java).forEach { method ->
                        if (method.isTest()) {
                            val currentTest = parseMethod(method)
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

    private fun parseMethod(method: MethodDeclaration): CotejiTest {
        val statements = method.body
                .orElseThrow { RuntimeException("Method ${method.nameAsString} has no body") }
                .statements
        val content = statements.joinToString("\n") { it.toString().lineTransform() }
        return CotejiTest(
                name = method.getTestName(),
                id = method.getAnnotationValue(testIdAnnotationName),
                content = content,
                attributes = method.getAttributes())
    }

}
