package io.sn.aetherium.objects.exceptions

class MissingArgumentException(argumentName: String) :
    Exception("Missing argument `$argumentName`") {
    constructor() : this("")
}