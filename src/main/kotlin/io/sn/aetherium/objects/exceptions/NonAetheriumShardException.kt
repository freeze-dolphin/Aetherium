package io.sn.aetherium.objects.exceptions

class NonAetheriumShardException(s: String) : Exception(s) {
    constructor() : this("")
}