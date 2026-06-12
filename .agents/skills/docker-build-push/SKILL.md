---
name: docker-build-push
description: Build and push multi-platform Docker images to Google Artifact Registry. Use when user asks to build, push, or deploy a Docker image with a tag. Triggers: "build and push", "push docker image", "build with tag", "deploy image".
---

# Docker Build and Push

Build and push multi-platform Docker images to the melosys-api Google Artifact Registry.

> This repo-local skill intentionally overrides the global `docker-build-push`,
> which auto-detects the image name from the cwd (`basename` of the git root)
> and would mis-name images in parallel checkouts like `melosys-api-claude`.

## Image name — ALWAYS `melosys-api`

The image repository is **always** `melosys-api`, never the cwd directory name.
Parallel workspaces may be checked out as `melosys-api-claude`, `melosys-api-2`,
etc., but the published image name never changes.

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

The `Dockerfile` is single-stage (distroless) and copies a pre-built JAR
(`COPY /app/target/melosys-sb-execution.jar`) — it does NOT build the JAR
itself. So you must produce the JAR before the buildx build whenever code
changed or `app/target/` has no current JAR.

1. Build the JAR first: `make build-fast` (`mvn clean install -DskipTests`)
2. Run buildx build with `--push` — keep the image path exactly as shown above
3. Report: tag, platforms, manifest digest
4. Before reporting "done", verify the `-t` argument ends with `/melosys-api:TAG`
   (not `/melosys-api-claude:TAG` or any other variant)

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
