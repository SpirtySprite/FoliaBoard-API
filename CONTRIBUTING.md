# Contributing to FoliaBoard

Thanks for helping out. FoliaBoard is a library for scoreboards, nametags, tab lists and boss bars on Paper and Folia, so the two rules
that matter most are: keep every public call safe from any thread, and keep the public API
small and stable.

## Setup

- Java 21
- Maven (a wrapper is not required; any recent Maven works)

```
mvn verify
```

This compiles, runs the tests and builds the sources and javadoc jars.

## Compatibility checks

Where the project supports several Paper API versions, CI compiles and tests against each of
them. You can do the same locally:

```
mvn verify -Dpaper.version=1.20.6-R0.1-SNAPSHOT
```

These runs check that the code compiles and unit-tests against that API. They do not replace a
smoke test on a real server. If you touch packet or reflection code, please say which server
versions you tried it on in the pull request.

## Code guidelines

- Anything under an `internal` package is not public API and may change freely. Do not reference
  it from the documentation.
- New public types and methods need javadoc and a test.
- Do not block a region thread. Anything slow (HTTP, disk, database) runs asynchronously and
  hands its result back through the scheduler.
- Keep the style of the surrounding code. `.editorconfig` covers whitespace.

## Pull requests

- One logical change per pull request.
- Add a line to `CHANGELOG.md` under "Unreleased" for anything a user would notice.
- Breaking changes to public API need a deprecation cycle first, unless the type was never
  released.

## Releasing (maintainers)

1. Move the "Unreleased" notes in `CHANGELOG.md` under a new `## [x.y.z] - date` heading.
2. Set the version in `pom.xml` to `x.y.z`.
3. Tag the commit `vx.y.z` and push the tag. The release workflow checks that the tag matches the
   pom, runs the build and publishes a GitHub release with the jars.
