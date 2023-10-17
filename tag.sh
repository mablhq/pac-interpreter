#!/bin/bash

BRANCH=$(git branch --no-color 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/\1/')
if [ "$BRANCH" != "main" ]; then
	echo "Must be on main branch to tag"
	exit 1
fi

# Extract the version from build.gradle, and use it as the tag:
VERSION_RE="^.*baseVersion = '([0-9]+\.[0-9]+\.[0-9]+)'.*"
VERSION_STRING="$(grep 'baseVersion =' build.gradle)"
if [[ ${VERSION_STRING} =~ ${VERSION_RE} ]]; then
  VERSION=${BASH_REMATCH[1]}
fi

if [ -z "${VERSION}" ]; then
  echo "Invalid version found in [${VERSION_STRING}]"
  exit 1
fi

TAG="v${VERSION}"
HASH=$(git rev-parse HEAD)
git tag -a -m "${HASH}" "${TAG}"
git push origin "${TAG}"
