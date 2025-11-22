# Makefile for melosys-api local development
# Usage: make <target>

.PHONY: help build build-fast docker-build docker-build-local clean run test package docker-push docker-tag

# Default target
.DEFAULT_GOAL := help

# Variables
APP_NAME := melosys-api
DOCKER_IMAGE := $(APP_NAME)
DOCKER_TAG := latest
DOCKER_REGISTRY := europe-north1-docker.pkg.dev/nais-management-233d/teammelosys
VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
MAVEN_OPTS := -Dmaven.test.skip=true

##@ Help

help: ## Display this help message
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Build

clean: ## Clean Maven build artifacts
	@echo "🧹 Cleaning Maven build artifacts..."
	mvn clean

build: ## Full Maven build with tests
	@echo "🔨 Building application with tests..."
	mvn clean install

build-fast: ## Fast Maven build without tests
	@echo "⚡ Fast build without tests..."
	mvn clean install -DskipTests -T 1C

package: ## Package the application JAR
	@echo "📦 Packaging application..."
	mvn clean package -DskipTests

##@ Docker

docker-build: package ## Build Docker image for local use (requires JAR)
	@echo "🐳 Building Docker image: $(DOCKER_IMAGE):$(DOCKER_TAG)"
	docker build -t $(DOCKER_IMAGE):$(DOCKER_TAG) .
	@echo "✅ Docker image built successfully: $(DOCKER_IMAGE):$(DOCKER_TAG)"

docker-build-local: build-fast docker-build ## Build application and Docker image (one command)

docker-tag: ## Tag local image with version
	@echo "🏷️  Tagging Docker image with version $(VERSION)..."
	docker tag $(DOCKER_IMAGE):$(DOCKER_TAG) $(DOCKER_IMAGE):$(VERSION)
	@echo "✅ Tagged: $(DOCKER_IMAGE):$(VERSION)"

docker-push: docker-tag ## Push Docker image to registry (requires authentication)
	@echo "⬆️  Pushing Docker image to registry..."
	docker tag $(DOCKER_IMAGE):$(DOCKER_TAG) $(DOCKER_REGISTRY)/$(DOCKER_IMAGE):$(VERSION)
	docker push $(DOCKER_REGISTRY)/$(DOCKER_IMAGE):$(VERSION)

docker-run: ## Run Docker container locally
	@echo "🚀 Running Docker container..."
	docker run -p 8080:8080 \
		-e SPRING_PROFILES_ACTIVE=local-mock \
		$(DOCKER_IMAGE):$(DOCKER_TAG)

docker-clean: ## Remove local Docker images
	@echo "🗑️  Removing Docker images..."
	docker rmi $(DOCKER_IMAGE):$(DOCKER_TAG) || true
	docker rmi $(DOCKER_IMAGE):$(VERSION) || true

##@ Testing

test: ## Run all tests
	@echo "🧪 Running tests..."
	mvn test

test-integration: ## Run integration tests
	@echo "🔬 Running integration tests..."
	mvn verify -Pintegration-tests

##@ Coverage

coverage: ## Run tests with coverage and show summary
	@echo "📊 Running tests with coverage..."
	@mvn clean test
	@echo ""
	@./scripts/coverage/summary.sh

coverage-report: ## Generate coverage reports (run after tests)
	@echo "📈 Generating coverage reports..."
	@mvn jacoco:report
	@echo ""
	@./scripts/coverage/summary.sh

coverage-summary: ## Show coverage summary (requires existing reports)
	@./scripts/coverage/summary.sh

##@ Local Development

run: ## Run application locally with local-mock profile
	@echo "🏃 Running application with local-mock profile..."
	mvn spring-boot:run -pl app -Dspring-boot.run.profiles=local-mock

run-dev: ## Run application with Spring DevTools for hot reload
	@echo "🔥 Running application with DevTools..."
	mvn spring-boot:run -pl app -Dspring-boot.run.profiles=local-mock -Dspring-boot.run.fork=false

##@ Docker Compose Integration

compose-up: docker-build ## Build image and start with docker-compose
	@echo "🐳 Starting melosys-api and dependencies with docker-compose..."
	@if [ -f "../melosys-docker-compose/docker-compose-api.yml" ]; then \
		cd ../melosys-docker-compose && docker-compose -f docker-compose.yml -f docker-compose-api.yml up -d; \
	else \
		echo "⚠️  melosys-docker-compose not found at ../melosys-docker-compose"; \
		echo "   Clone it with: git clone https://github.com/navikt/melosys-docker-compose.git ../melosys-docker-compose"; \
	fi

compose-down: ## Stop docker-compose services
	@echo "🛑 Stopping docker-compose services..."
	@if [ -f "../melosys-docker-compose/docker-compose-api.yml" ]; then \
		cd ../melosys-docker-compose && docker-compose -f docker-compose.yml -f docker-compose-api.yml down; \
	else \
		echo "⚠️  melosys-docker-compose not found"; \
	fi

compose-logs: ## Show docker-compose logs for melosys-api
	@if [ -f "../melosys-docker-compose/docker-compose-api.yml" ]; then \
		cd ../melosys-docker-compose && docker-compose -f docker-compose.yml -f docker-compose-api.yml logs -f melosys-api; \
	else \
		echo "⚠️  melosys-docker-compose not found"; \
	fi

compose-restart: ## Restart melosys-api container
	@echo "🔄 Restarting melosys-api..."
	@if [ -f "../melosys-docker-compose/docker-compose-api.yml" ]; then \
		cd ../melosys-docker-compose && docker-compose -f docker-compose.yml -f docker-compose-api.yml restart melosys-api; \
	else \
		echo "⚠️  melosys-docker-compose not found"; \
	fi

compose-rebuild: docker-build compose-down compose-up ## Rebuild and restart with docker-compose

##@ Database

db-migrate: ## Run Flyway database migrations
	@echo "📊 Running database migrations..."
	mvn flyway:migrate -pl app

db-clean: ## Clean database (Flyway clean)
	@echo "⚠️  Cleaning database..."
	mvn flyway:clean -pl app

db-info: ## Show database migration info
	@echo "ℹ️  Database migration info..."
	mvn flyway:info -pl app

##@ Utilities

verify: ## Verify the build
	@echo "✅ Verifying build..."
	mvn verify

format: ## Format code with Maven
	@echo "🎨 Formatting code..."
	mvn fmt:format

check-format: ## Check code formatting
	@echo "🔍 Checking code format..."
	mvn fmt:check

dependencies: ## Show dependency tree
	@echo "📦 Showing dependency tree..."
	mvn dependency:tree

version: ## Show project version
	@echo "Version: $(VERSION)"

##@ Debugging

docker-inspect: ## Inspect locally built Docker image
	@echo "📋 Inspecting Docker image: $(DOCKER_IMAGE):$(DOCKER_TAG)"
	@docker inspect $(DOCKER_IMAGE):$(DOCKER_TAG) --format='{{.Config.Cmd}}' | echo "CMD: $$(cat)"
	@echo "\nEnvironment variables:"
	@docker inspect $(DOCKER_IMAGE):$(DOCKER_TAG) --format='{{range .Config.Env}}{{println .}}{{end}}'
	@echo "\nImage size:"
	@docker images $(DOCKER_IMAGE):$(DOCKER_TAG) --format "{{.Repository}}:{{.Tag}} - {{.Size}}"

docker-test-run: ## Test run container with port mapping and local-mock profile
	@echo "🧪 Test running container on http://localhost:8080"
	@echo "   Press Ctrl+C to stop"
	docker run --rm -it \
		-p 8080:8080 \
		-e SPRING_PROFILES_ACTIVE=local-mock \
		-e SERVER_ADDRESS=0.0.0.0 \
		--name melosys-test \
		$(DOCKER_IMAGE):$(DOCKER_TAG)

docker-debug: ## Debug container issues
	@echo "🔍 Debugging Docker setup..."
	@echo "\n1. Checking if JAR exists:"
	@ls -lh app/target/melosys-sb-execution.jar || echo "❌ JAR not found - run 'make package' first"
	@echo "\n2. Checking Docker images:"
	@docker images | grep melosys-api || echo "❌ No melosys-api images found"
	@echo "\n3. Checking if container is running:"
	@docker ps | grep melosys || echo "No melosys containers running"
	@echo "\n4. Recent container logs (if any):"
	@docker ps -a | grep melosys | head -1 | awk '{print $$1}' | xargs -I {} docker logs {} 2>&1 | tail -20 || echo "No recent logs"

compose-ps: ## Show docker-compose service status
	@if [ -f "../melosys-docker-compose/docker-compose-api.yml" ]; then \
		cd ../melosys-docker-compose && docker-compose -f docker-compose.yml -f docker-compose-api.yml ps; \
	else \
		echo "⚠️  melosys-docker-compose not found"; \
	fi

compose-check: ## Check if melosys-api container is running and accessible
	@echo "🔍 Checking melosys-api status..."
	@docker ps | grep melosys-api || echo "❌ melosys-api container is NOT running"
	@echo "\n📡 Testing connection to http://localhost:8080/internal/health..."
	@curl -s http://localhost:8080/internal/health 2>/dev/null && echo "\n✅ API is responding!" || echo "❌ API is not responding"

##@ Complete Workflows

all: clean build docker-build ## Clean, build, and create Docker image

local-setup: build-fast docker-build compose-up ## Complete local setup (build + docker + compose)

rebuild: clean build-fast docker-build ## Quick rebuild (clean + fast build + docker)
