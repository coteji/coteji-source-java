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

package io.github.coteji.filter

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration

class TestsFilter(query: String) {
    private val entries: List<FilterEntry>

    init {
        if (query.isBlank()) {
            throw RuntimeException("Search criteria cannot be empty")
        }
        entries = query.trim().split(" ").map {
            FilterEntry(
                    included = it.first() == '+',
                    type = it.substring(1).substringBefore(":"),
                    value = it.substringAfter(":")
            )
        }
    }

    fun hasOnlyPackagesAndClasses(): Boolean = entries.all { it.type == "package" || it.type == "class" }
    fun hasOnlyMethods(): Boolean = entries.all { it.type == "method" }

    fun classIncluded(javaFile: CompilationUnit): Boolean {
        val pack = javaFile.packageDeclaration.get().nameAsString
        val className = javaFile.primaryTypeName.get()
        if (packageAllowed(pack) && classAllowed(className)) {
            return packageNotExcluded(pack) && classNotExcluded(className)
        }
        return false
    }

    private fun classNotExcluded(className: String): Boolean =
            entries.none { !it.included && it.type == "class" && it.value == className }

    private fun packageNotExcluded(pack: String) =
            entries.none { !it.included && it.type == "package" && inPackage(it.value, pack) }

    private fun classAllowed(className: String): Boolean =
            entries.none { it.included && (it.type == "class" || it.type == "method") }
                    || entries.any {
                it.included && (it.type == "class" && it.value == className
                        || it.type == "method" && it.value.startsWith("$className."))
            }

    private fun packageAllowed(pack: String) = (entries.none { it.included && it.type == "package" }
            || entries.any { it.included && it.type == "package" && inPackage(it.value, pack) })

    fun methodIncluded(method: MethodDeclaration): Boolean {
        val methodName = method.nameAsString
        val className = method.findCompilationUnit().get().primaryTypeName.get()
        val pack = method.findCompilationUnit().get().packageDeclaration.get().nameAsString

        if (packageAllowed(pack) && classAllowed(className) && methodAllowed(className, methodName)
                && annotationsAllowed(method)) {
            return packageNotExcluded(pack) && classNotExcluded(className) && methodNotExcluded(className, methodName)
                    && annotationsNotExcluded(method)
        }
        return false
    }

    private fun annotationsNotExcluded(method: MethodDeclaration): Boolean =
            entries.none {
                !it.included && it.type.startsWith("annotation") && methodCorresponds(method, it.type, it.value)
            }

    private fun methodNotExcluded(className: String, methodName: String) =
            entries.none { !it.included && it.type == "method" && it.value == "$className.$methodName" }

    private fun annotationsAllowed(method: MethodDeclaration) =
            (entries.none { it.included && it.type.startsWith("annotation") }
                    || entries.any {
                it.included && it.type.startsWith("annotation") && methodCorresponds(method, it.type, it.value)
            })

    private fun methodAllowed(className: String, methodName: String) =
            (entries.none { it.included && it.type == "method" && it.value.startsWith("$className.") }
                    || entries.any { it.included && it.type == "method" && it.value == "$className.$methodName" })

    private fun methodCorresponds(method: MethodDeclaration, conditionType: String, value: String): Boolean =
            getConditionByType(conditionType).invoke(method, value)

    private fun inPackage(parentPackage: String, currentPackage: String): Boolean =
            parentPackage == currentPackage || currentPackage.startsWith("$parentPackage.")

}

data class FilterEntry(
        val included: Boolean,
        val type: String,
        val value: String)

fun getConditionByType(type: String): (MethodDeclaration, String) -> Boolean {
    return when (type) {
        "annotationName" -> { method, value ->
            method.annotations.any { it.nameAsString == value }
        }
        "annotationValue" -> { method, value ->
            val annotationName = value.substringBefore(",")
            val annotationValue = value.substringAfter(",")
            method.annotations
                    .filter { it.isSingleMemberAnnotationExpr }
                    .any {
                        it.nameAsString == annotationName
                                && it.asSingleMemberAnnotationExpr().memberValue.toString() == annotationValue
                    }
        }
        "annotationValueContains" -> { method, value ->
            val annotationName = value.substringBefore(",")
            val annotationValue = value.substringAfter(",")
            method.annotations
                    .filter { it.isSingleMemberAnnotationExpr }
                    .any {
                        it.nameAsString == annotationName
                                && it.asSingleMemberAnnotationExpr().memberValue.toString().contains(annotationValue)
                    }
        }
        "annotationAttributeValue" -> { method, value ->
            val parts = value.split(",")
            if (parts.size != 3) {
                throw RuntimeException("Condition value should contain 3 values separated by comma")
            }
            val annotationName = parts.first()
            val attrName = parts.getOrNull(1)
            val attrValue = parts.last()
            method.annotations
                    .filter { it.isNormalAnnotationExpr && it.nameAsString == annotationName }
                    .any {
                        it.asNormalAnnotationExpr().pairs.any { attr ->
                            attr.nameAsString == attrName && attr.value.toString() == attrValue
                        }
                    }
        }
        "annotationAttributeValueContains" -> { method, value ->
            val parts = value.split(",")
            if (parts.size != 3) {
                throw RuntimeException("Condition value should contain 3 values separated by comma")
            }
            val annotationName = parts.first()
            val attrName = parts.getOrNull(1)
            val attrValue = parts.last()
            method.annotations
                    .filter { it.isNormalAnnotationExpr && it.nameAsString == annotationName }
                    .any {
                        it.asNormalAnnotationExpr().pairs.any { attr ->
                            attr.nameAsString == attrName && attr.value.toString().contains(attrValue)
                        }
                    }
        }
        else -> throw RuntimeException("Annotation condition not recognized")
    }
}