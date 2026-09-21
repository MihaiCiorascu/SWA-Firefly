package com.wildfiresync.device

import com.wildfiresync.core.World

fun main() {
    println("Device starting (known reports: ${World.EMPTY.reports().size})")
}