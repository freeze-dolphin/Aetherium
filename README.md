# Aetherium

Illuminated etheric substance, Arcaea composing software in C/S model.

## Running the server

Just run the jar, which will start the server

Directory structure:

- `aetherium.toml`: the configurations which control the main functionalities of the server
- `plugins/`: put your shard into this folder
- `config/`: the configurations of external shards will be generated into this folder
    - `config/internal/`: where the configurations of internal shard exist (if there is any)

## Writing your own shards

### Developing

A plugin for `Aetherium` is called a `Aetherium Shard`, and `shard` in short, which extends the function of `Aetherium` and provides more
features

To create a shard, you need to specify a 'main' class which should extends `io.sn.aetherium.objects.AetheriumShard`, and annotate it
with `io.sn.aetherium.objects.ShardInfo` like this:

```kotlin
// the annotation ShardInfo contains basic information of the shard
// annotation class ShardInfo(val id: String, val hasConfig: Boolean = false, val manualLoad: Boolean = false)
// `id` is used to identify your shard, please ensure this is not conflicting with any other shard
// `hasConfig` tells whether your shard need a TOML configuration
// `manualLoad` indicates if your plugin will handle the register and init stage itself. `onRegister()` will not be called if this field is set to true
@ShardInfo("example")
class ExampleShard : AetheriumShard() {

    // ...

}
```

Also, don't forget to override the necessary fields and functions:

- Field `digestionInfo` tells the system how your shard takes arguments:

```kotlin
override val digestionInfo = ShardDigestionArgsInfo {

    // declare a new argument named `timingList` with type List<Long>
    // there are currently ten types available, defined in enum class `io.sn.aetherium.objects.ShardDigestionArgsInfo.Item.Type`
    addInfo("timingList", ShardDigestionArgsInfo.Item.Type.LONG_LIST)

}
```

- Optional function `onGenesis()` is called right after init of your shard, for example you can load your configuration file at this stage

- Optional function `onTermination()` is called before the server is down, you can do some cleanup here

- Function `generator()` is how you handle the arguments:

```kotlin
override fun generator(): Difficulty.() -> Unit = {

    // use `digestSOME_TYPE` functions to get the parameters
    // there are currently ten digest functions available, defined in abstract class `io.sn.aetherium.objects.AetheriumShard`
    // digest functions are corresponding to the ShardDigestionArgsInfo you have defined above, please make sure their types are the same
    val timingList = digestLongList("timingList")

    timingList.forEach {
        quickArctap(it, Random.nextDouble(-0.5, 1.5) pos 0.5)
    }
}
```

For how to use [aff-compose](https://github.com/freeze-dolphin/aff-compose), please check out the readme of the repository whose demo shows
detailed usages

Above all, you should add an entry to your MANIFEST.MF to assign your 'main' class:

```kotlin
tasks.jar {
    manifest {
        attributes(
            "Aetherium-Entry" to "org.example.aetheriumshard.ExampleShard",
        )
    }
}
```

For full code, please check out [exampleShard](example/exampleShard)

For a more complex example, please give a glimpse
at [ArcTapJumpShard.kt](src/main/kotlin/io/sn/aetherium/implementations/shards/ArcTapJumpShard.kt)

### Testing

Please check out [ArcTapJumpShardTest.kt](src/test/kotlin/io/sn/aetherium/implementations/shards/ArcTapJumpShardTest.kt), which is a testing
unit written for the internal plugin [ArcTapJumpShard.kt](src/main/kotlin/io/sn/aetherium/implementations/shards/ArcTapJumpShard.kt)

## Etoile Resurrection

USAGE: `java -cp <PATH TO Aetherium.jar> io.sn.aetherium.implementations.crystals.EtoileRessurectionKt <PREFIX> <MODE> [arcpkgs]`

Available Modes:

- SIMPLIFIED: extract backgrounds, ignore if existed
- PRECISE: use tree structure to store backgrounds, you may need to manually set backgrounds later
- OVERWRITE: extract backgrounds, overwrite if existed
- AUTO_RENAME: add prefix to filenames to avoid conflicts
  The convert result will be in `$PWD/result/`