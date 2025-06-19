#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$ROOT_DIR"

CURRENT_VERSION="$(tr -d '[:space:]' < VERSION)"

if [[ ${1-} ]]; then
  NEW_VERSION="$1"
else
  read -rp "Enter new version (current: $CURRENT_VERSION): " NEW_VERSION
fi

SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+([+-][A-Za-z0-9\.-]+)?$'
if [[ ! $NEW_VERSION =~ $SEMVER_REGEX ]]; then
  echo "Version must follow pattern <major>.<minor>.<patch>[-suffix]" >&2
  exit 1
fi

NUM_CUR=${CURRENT_VERSION%%[-+]*}
NUM_NEW=${NEW_VERSION%%[-+]*}
IFS='.' read -r MAJOR_CUR MINOR_CUR PATCH_CUR <<< "$NUM_CUR"
IFS='.' read -r MAJOR_NEW MINOR_NEW PATCH_NEW <<< "$NUM_NEW"

if (( MAJOR_NEW < MAJOR_CUR )) ||
   { (( MAJOR_NEW == MAJOR_CUR )) && (( MINOR_NEW < MINOR_CUR )); } ||
   { (( MAJOR_NEW == MAJOR_CUR )) && (( MINOR_NEW == MINOR_CUR )) && (( PATCH_NEW <= PATCH_CUR )); }; then
  echo "New version ($NEW_VERSION) must be greater than current version ($CURRENT_VERSION)" >&2
  exit 1
fi

echo "Updating versions to $NEW_VERSION…"

echo "$NEW_VERSION" > VERSION

echo "$NEW_VERSION" > docker-control/VERSION

sed -i -E "s/\"version\"[[:space:]]*:[[:space:]]*\"[^\"]+\"/\"version\": \"$NEW_VERSION\"/" frontend/package.json

sed -i -E "s/^version[[:space:]]*=.*/version = '$NEW_VERSION'/" gateway-api/build.gradle

echo "Files updated: VERSION, docker-control/VERSION, frontend/package.json, gateway-api/build.gradle"

read -rp "Commit changes now? [y/N]: " COMMIT
if [[ $COMMIT =~ ^[Yy]$ ]]; then
  git add VERSION docker-control/VERSION frontend/package.json gateway-api/build.gradle
  git commit -m "chore: bump version to $NEW_VERSION"
  echo "Commit created."
else
  echo "ℹ  Remember to commit the changes manually."
fi 