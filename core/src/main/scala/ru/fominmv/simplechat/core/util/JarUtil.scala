package ru.fominmv.simplechat.core.util


import java.io.File


object JarUtil:
    def jarName: String =
        val classPath = System getProperty "java.class.path"
        val file      = File(classPath)

        file.getName