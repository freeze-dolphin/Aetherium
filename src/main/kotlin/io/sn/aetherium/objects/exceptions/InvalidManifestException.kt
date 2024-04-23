package io.sn.aetherium.objects.exceptions

class InvalidManifestException(s: String) : Exception(s) {
    constructor() : this("")
}