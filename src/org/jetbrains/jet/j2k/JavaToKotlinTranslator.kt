package org.jetbrains.jet.j2k

import com.intellij.psi.PsiFile
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.psi.PsiFileFactory
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.Disposable
import com.intellij.core.JavaCoreApplicationEnvironment
import org.jetbrains.jet.utils.PathUtil
import java.net.URLClassLoader
import com.intellij.psi.PsiElement
import org.jetbrains.jet.j2k.visitors.ClassVisitor
import com.intellij.psi.PsiJavaFile
import java.io.File

public class JavaToKotlinTranslator() {


    class object {
        private val CONVERTER = Converter()
        private fun createFile(text: String): PsiFile? {
            val javaCoreEnvironment = setUpJavaCoreEnvironment()
            return PsiFileFactory.getInstance(javaCoreEnvironment.getProject())?.createFileFromText("test.java", JavaLanguage.INSTANCE, text)
        }

        fun createFile(javaCoreEnvironment: JavaCoreProjectEnvironment, text: String): PsiFile? {
            return PsiFileFactory.getInstance(javaCoreEnvironment.getProject())?.createFileFromText("test.java", JavaLanguage.INSTANCE, text)
        }

        fun setUpJavaCoreEnvironment(): JavaCoreProjectEnvironment {
            val parentDisposable = object : Disposable {
                public override fun dispose() {

                }
            }
            val applicationEnvironment = JavaCoreApplicationEnvironment(parentDisposable)
            val javaCoreEnvironment = JavaCoreProjectEnvironment(parentDisposable, applicationEnvironment)
            javaCoreEnvironment.addJarToClassPath(PathUtil.findRtJar())
            val annotations = findAnnotations()
            if (annotations != null && annotations.exists()) {
                javaCoreEnvironment.addJarToClassPath(annotations)
            }

            return javaCoreEnvironment
        }

        fun prettify(code: String?): String {
            if (code == null) {
                return ""
            }

            return code.trim().replaceAll("\r\n", "\n").replaceAll(" \n", "\n").replaceAll("\n ", "\n").replaceAll("\n+", "\n").replaceAll(" +", " ").trim()
        }

        private fun findAnnotations(): File? {
            var classLoader: ClassLoader? = javaClass<JavaToKotlinTranslator?>().getClassLoader()
            while (classLoader != null) {
                val loader = classLoader
                if (loader is URLClassLoader) {
                    for (url in loader.getURLs()!!) {
                        val file = url.getFile()!!
                        if (("file".equals(url.getProtocol())) && (file.endsWith("/annotations.jar"))) {
                            return File(file)
                        }
                    }
                }

                classLoader = classLoader?.getParent()
            }
            return null
        }

        fun setClassIdentifiers(converter: Converter, psiFile: PsiElement): Unit {
            val c = ClassVisitor()
            psiFile.accept(c)
            converter.clearClassIdentifiers()
            converter.setClassIdentifiers(c.getClassIdentifiers())
        }

        fun generateKotlinCode(javaCode: String): String {
            val file = createFile(javaCode)
            if (file is PsiJavaFile)
            {
                setClassIdentifiers(CONVERTER, file)
                return prettify(CONVERTER.fileToFile(file).toKotlin())
            }

            return ""
        }

        fun generateKotlinCodeWithCompatibilityImport(javaCode: String): String {
            val file = createFile(javaCode)
            if (file is PsiJavaFile) {
                setClassIdentifiers(CONVERTER, file)
                return prettify(CONVERTER.fileToFileWithCompatibilityImport(file).toKotlin())
            }

            return ""
        }

        public fun main(args: Array<String>): Unit {
            val out = System.out
            if (args.size == 1) {
                try {
                    val kotlinCode = generateKotlinCode(args[0])
                    if (kotlinCode.isEmpty()) {
                        out.println("EXCEPTION: generated code is empty.")
                    }
                    else {
                        out.println(kotlinCode)
                    }
                }
                catch (e: Exception) {
                    out.println("EXCEPTION: " + e.getMessage())
                }


            }
            else {
                out.println("EXCEPTION: wrong number of arguments (should be 1).")
            }
        }
        public fun translateToKotlin(code: String): String? {
            return generateKotlinCode(code)
        }
    }
}
