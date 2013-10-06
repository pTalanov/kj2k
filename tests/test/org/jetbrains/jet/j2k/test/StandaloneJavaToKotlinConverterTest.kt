package org.jetbrains.jet.j2k.test

import junit.framework.TestCase
import java.io.File
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.io.FileUtil
import junit.framework.Assert
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiFile
import com.intellij.core.JavaCoreProjectEnvironment
import junit.framework.Test
import junit.framework.TestSuite
import java.io.FilenameFilter
import com.intellij.openapi.application.PathManager
import java.io.FileFilter
import java.util.Collections
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.JavaToKotlinTranslator

public class StandaloneJavaToKotlinConverterTest(val dataPath: String, val name: String) : TestCase() {

    protected override fun runTest(): Unit {
        val converter = Converter()
        val javaPath = "tests/testData/" + getTestFilePath()
        val kotlinPath = javaPath.replace(".jav", ".kt")
        val kotlinFile = File(kotlinPath)
        if (!kotlinFile.exists()) {
            FileUtil.writeToFile(kotlinFile, "")
        }

        val expected = StringUtil.convertLineSeparators(FileUtil.loadFile(kotlinFile))
        val javaFile = File(javaPath)
        val javaCode = FileUtil.loadFile(javaFile)
        val parentFileName = javaFile.getParentFile()?.getName()

        val actual = StringUtil.convertLineSeparators(
                when (parentFileName)
                {
                    "expression" -> expressionToKotlin(converter, javaCode)
                    "statement" -> statementToKotlin(converter, javaCode)
                    "method" -> methodToKotlin(converter, javaCode)
                    "class" -> fileToKotlin(converter, javaCode)
                    "file" -> fileToKotlin(converter, javaCode)
                    "comp" -> fileToFileWithCompatibilityImport(javaCode)
                    else -> throw IllegalStateException("Specify what is it: file, class, method, statement or expression: "
                    + javaPath + " parent: " + parentFileName)
                })

        val tmp = File(kotlinPath + ".tmp")
        if (!expected.equals(actual)) {
            FileUtil.writeToFile(tmp, actual)
        }

        if ((expected.equals(actual)) && (tmp.exists())) {
            tmp.delete()
        }

        Assert.assertEquals(expected, actual)
    }

    fun getTestFilePath(): String {
        return dataPath + "/" + name + ".jav"
    }

    private fun fileToKotlin(converter: Converter, text: String): String {
        return generateKotlinCode(converter, JavaToKotlinTranslator.createFile(myJavaCoreEnvironment, text))
    }

    private fun methodToKotlin(converter: Converter, text: String?): String {
        var result = fileToKotlin(converter, "final class C {" + text + "}").replaceAll("class C\\(\\) \\{", "")
        result = result.substring(0, (result.lastIndexOf("}")))
        return prettify(result)
    }

    private fun statementToKotlin(converter: Converter, text: String?): String {
        var result = methodToKotlin(converter, "void main() {" + text + "}")
        val pos = result.lastIndexOf("}")
        result = result.substring(0, pos).replaceFirst("fun main\\(\\) : Unit \\{", "")
        return prettify(result)
    }

    private fun expressionToKotlin(converter: Converter, code: String?): String {
        var result = statementToKotlin(converter, "Object o =" + code + "}")
        result = result.replaceFirst("var o : Any\\? =", "")
        return prettify(result)
    }

    class object {
        private val myJavaCoreEnvironment: JavaCoreProjectEnvironment = JavaToKotlinTranslator.setUpJavaCoreEnvironment()

        private fun fileToFileWithCompatibilityImport(text: String): String {
            return JavaToKotlinTranslator.generateKotlinCodeWithCompatibilityImport(text)
        }

        private fun generateKotlinCode(converter: Converter, file: PsiFile?): String {
            if (file is PsiJavaFile) {
                JavaToKotlinTranslator.setClassIdentifiers(converter, file)
                return prettify(converter.elementToKotlin(file))
            }

            return ""
        }

        private fun prettify(code: String?): String {
            if (code == null) {
                return ""
            }

            return code.trim().replaceAll("\r\n", "\n").replaceAll(" \n", "\n").replaceAll("\n ", "\n").replaceAll("\n+", "\n").replaceAll(" +", " ").trim()
        }
    }
}

private val emptyFilter = object : FilenameFilter {
    public override fun accept(dir: File, name: String): Boolean {
        return true
    }
}

public fun getTestDataPathBase(): String {
    return "testData"
}

public fun getHomeDirectory(): String? {
    return File(PathManager.getResourceRoot(javaClass<StandaloneJavaToKotlinConverterTest>(), "/org/jetbrains/jet/TestCaseBuilder.class")!!).getParentFile()?.getParentFile()?.getParent()
}

public trait NamedTestFactory {
    fun createTest(dataPath: String, name: String): Test
}

public fun suiteForDirectory(baseDataDir: String?, dataPath: String, factory: NamedTestFactory): TestSuite {
    return suiteForDirectory(baseDataDir, dataPath, true, emptyFilter, factory)
}

public fun suiteForDirectory(baseDataDir: String?, dataPath: String, recursive: Boolean, filter: FilenameFilter, factory: NamedTestFactory): TestSuite {
    val suite = TestSuite(dataPath)
    val extensionJava = ".jav"
    val extensionFilter = object : FilenameFilter {
        public override fun accept(dir: File, name: String): Boolean {
            return name.endsWith(extensionJava)
        }
    }

    val resultFilter =
            if (filter != emptyFilter) {
                object : FilenameFilter {
                    public override fun accept(dir: File, name: String): Boolean {
                        return (extensionFilter.accept(dir, name)) && filter.accept(dir, name)
                    }
                }
            }
            else {
                extensionFilter
            }

    val dir = File(baseDataDir + dataPath)
    val dirFilter = object : FileFilter {
        public override fun accept(pathname: File): Boolean {
            return pathname.isDirectory()
        }
    }

    if (recursive) {
        val files = dir.listFiles(dirFilter)
        val subdirs = files!!.toLinkedList()
        Collections.sort(subdirs)
        for (subdir in subdirs) {
            suite.addTest(suiteForDirectory(baseDataDir, dataPath + "/" + subdir.getName(), recursive, filter, factory))
        }
    }

    val files = (dir.listFiles(resultFilter))!!.toLinkedList()
    Collections.sort(files)
    for (file in files) {
        val testName = file.getName().substring(0, (file.getName().length()) - (extensionJava.length()))
        suite.addTest(factory.createTest(dataPath, testName))
    }
    return suite
}

