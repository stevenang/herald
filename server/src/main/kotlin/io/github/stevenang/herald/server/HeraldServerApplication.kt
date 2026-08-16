package io.github.stevenang.herald.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HeraldServerApplication

fun main(args: Array<String>) {
    runApplication<HeraldServerApplication>(*args)
}