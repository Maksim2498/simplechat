package ru.fominmv.simplechat.client


import org.apache.logging.log4j.LogManager


object Main:
    def main(args: Array[String]): Unit =
        logger info "Starting chat client..."
        logger info "Stopping"


    private val logger = LogManager getLogger getClass