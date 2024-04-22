package io.sn.aetherium.objects.exceptions

class AetheriumHaventConnectException(s: String) : Exception(s) {
    constructor() : this("")
}