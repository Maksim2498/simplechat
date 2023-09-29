package ru.fominmv.simplechat.core.util


import scala.collection.mutable.{HashSet, HashMap}


object RuntimeUtil:
    def addShutdownHook(hook: () => Unit): Boolean =
        if hookThreads contains hook then
            return false

        val runtime = Runtime.getRuntime
        val thread  = Thread(() => hook())

        runtime addShutdownHook thread
        hookThreads addOne (hook, thread)

        true

    def removeShutdownHook(hook: () => Unit): Boolean =
        val threadOption = hookThreads get hook

        if threadOption == None then
            return false

        val runtime = Runtime.getRuntime
        val thread  = threadOption.get

        runtime removeShutdownHook thread

        return true


    private val hookThreads = HashMap[() => Unit, Thread]()