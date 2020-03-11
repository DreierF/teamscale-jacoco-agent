# Teamscale JaCoCo Agent [![Build Status](https://travis-ci.com/cqse/teamscale-jacoco-agent.svg?branch=master)](https://travis-ci.com/cqse/teamscale-jacoco-agent)

## Download

* [Binary Distribution](https://github.com/cqse/teamscale-jacoco-agent/releases)
* [Docker Container](https://hub.docker.com/r/cqse/teamscale-jacoco-agent/tags/)

## Documentation

* [Teamscale JaCoCo Agent](agent/README.md)
* [Java Web Start Wrapper](javaws-wrapper/README.md)

## Development

### Build locally

* Import in Eclipse/IntelliJ as Gradle project
* Command line: `./gradlew assemble`
* Local docker build: `docker build -f agent/src/docker/Dockerfile .`

### Debugging the Gradle plugin

* increase the plugin version in `build.gradle` and in `BuildVersion.kt`
* `./gradlew iGP` will deploy your checked out version to your local m2 cache
* then you can import this version into any other gradle project by
  * replacing the `share.cqse.eu` repository with the `mavenLocal()` repository in the `buildscript` section of the project's `build.gradle`
  * adding `repositories { mavenLocal(); mavenCentral() }` to the body of the `build.gradle`
  * declaring a plugin dependency on the incremented version of the teamscale plugin
* to debug a build that uses the plugin, run `./gradlew` with `--no-daemon -Dorg.gradle.debug=true`.
  The build will pause and wait for you to attach a debugger, via IntelliJ's `Run > Attach to Process`.
* to debug the impacted test engine during a build, run `./gradlew` with `--no-daemon --debug-jvm` and wait for the test phase to start.
  The build will pause and wait for you to attach a debugger, via IntelliJ's `Run > Attach to Process`.
* These two debug flags can also be combined. The build will then pause twice

### Contributing

* Create a JIRA issue for changes
* Use pull requests. Complete the "Definition of Done" for every pull request.
* There's a Teamscale project, please fix all findings before submitting your pull request for review. The Teamscale coding guidelines and Definition of Done apply as far as possible with the available tooling.

### Publishing

When master has accumulated changes you want to release, please perform the following on master in a single commit:

- update [the changelog](CHANGELOG.md) and move all changes from the _Next release_ section to a new version, e.g. `v8.1.0`
- update the [build.gradle](build.gradle)'s `appVersion` accordingly
- if you want to release a new version of the Gradle plugin:
    - update `BuildVersion.kt`
    - run `./gradlew deployGradlePlugin`
- commit and push your changes
- create a GitHub Release tag with the same version number and the text from the changleog.

Releases are numbered according to semantic versioning (see full [changelog](CHANGELOG.md)).

All tags are built automatically using [Travis CI](https://travis-ci.com/cqse/teamscale-jacoco-agent) and [Docker Cloud Build](https://cloud.docker.com/swarm/cqse/repository/docker/cqse/teamscale-jacoco-client) with the release binaries being uploaded to the GitHub Releases.

Only use GitHub releases in production. This ensures that we always know which code is running in production.

### Compiling for a different JaCoCo version

* change `ext.jacocoVersion` in the build script
* `gradlew dist`
* **Do not commit unless you are upgrading to a newer version!**
