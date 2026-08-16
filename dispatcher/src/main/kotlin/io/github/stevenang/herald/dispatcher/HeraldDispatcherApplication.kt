package io.github.stevenang.herald.dispatcher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HeraldDispatcherApplication

fun main(args: Array<String>) {
    runApplication<HeraldDispatcherApplication>(*args)
}