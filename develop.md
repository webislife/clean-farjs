## Developing

### How to Build

To build and run all the tests use the following command:
```bash
sbt test
```

### How to Run

To run the app use the following commands:
```bash
sbt "project farjs-app" npmUpdate fastOptJS

node ./app/target/scala-2.13/scalajs-bundler/main/dev.loader.js
```

### How to Run with Reload Workflow

```bash
#console 1:
sbt
>project farjs-app
>~fastOptJS

#console 2:
node --watch ./app/target/scala-2.13/scalajs-bundler/main/dev.loader.js
```

## Resources

You can find more info about the common modules [here](https://scommons.org/)
