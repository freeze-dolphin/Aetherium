package io.sn.aetherium.objects.exceptions

class ShardHaventInitException(s: String) : Exception(s) {
    constructor() : this("")
}