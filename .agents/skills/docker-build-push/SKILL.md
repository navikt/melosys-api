---
name: docker-build-push
description: Build and push multi-platform Docker images to Google Artifact Registry. Use when user asks to build, push, or deploy a Docker image with a tag. Triggers: "build and push", "push docker image", "build with tag", "deploy image".
---

# Docker Build and Push

Build and push multi-platform Docker images to the melosys-api Google Artifact Registry.

## Registry

```
europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-api
```

## Build and Push Command

```bash
docker buildx build --platform linux/amd64,linux/arm64 \
  -t europe-north1-docker.pkg.dev/nais-management-233d/teammelosys/melosys-api:TAG \
  --push .
```

Replace `TAG` with user-specified tag.

## Workflow

1. If code changed, rebuild JAR first: `make build-fast`
2. Run buildx build with `--push`
3. Report: tag, platforms, manifest digest

## Setup (if buildx not configured)

```bash
docker buildx create --name multiplatform --use --driver docker-container || docker buildx use multiplatform
```

## Error Handling

### Authentication expired
Message: "Reauthentication failed"
Solution: Ask user to run `gcloud auth login`, then retry

### No matching manifest for linux/amd64
Cause: Image built only for arm64
Solution: Use `--platform linux/amd64,linux/arm64` flag

## Output Format

After successful push:
```
Done!

Image details:
- Tag: europe-north1-docker.pkg.dev/.../melosys-api:TAG
- Platforms: linux/amd64, linux/arm64
- Manifest digest: sha256:...
