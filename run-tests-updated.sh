#!/bin/bash

set -u  # Error on unset vars

# Parse arguments
CMD_ARGS="$@"
LOG_FILE="/tmp/mvn.log"
VERBOSE=0
USE_VERIFY=0
USE_INTEGRATION_TEST=0  # New flag for integration tests
SUGGEST_AM=0  # Flag to track if we should suggest -am

for arg in "$@"; do
  if [[ "$arg" == "--verbose" ]]; then
    VERBOSE=1
  elif [[ "$arg" == "--verify" ]]; then
    USE_VERIFY=1
    # Remove --verify from CMD_ARGS since Maven doesn't recognize it
    CMD_ARGS="${CMD_ARGS/--verify/}"
  elif [[ "$arg" == "--integration" || "$arg" == "-it" ]]; then
    USE_INTEGRATION_TEST=1
    # Remove --integration/-it from CMD_ARGS since Maven doesn't recognize it
    CMD_ARGS="${CMD_ARGS/--integration/}"
    CMD_ARGS="${CMD_ARGS/-it/}"
  fi
done

# Determine Maven command
if [ $USE_INTEGRATION_TEST -eq 1 ]; then
  MVN_CMD="failsafe:integration-test"
  # Convert -Dtest to -Dit.test for failsafe
  CMD_ARGS="${CMD_ARGS/-Dtest=/-Dit.test=}"
elif [ $USE_VERIFY -eq 1 ]; then
  MVN_CMD="verify"
else
  MVN_CMD="test"
fi

# Check if -pl is used without -am (for informational message)
if echo "$CMD_ARGS" | grep -q -- "-pl" && ! echo "$CMD_ARGS" | grep -q -- "-am"; then
  SUGGEST_AM=1
fi

print_header() {
  echo ""
  echo "────────────────────────────────────────────"
  echo "▶️  $1"
  echo "────────────────────────────────────────────"
  echo ""
}

run_maven() {
  local maven_args="$1"
  local attempt_name="$2"

  print_header "$attempt_name: mvn $maven_args"

  # Inform about -am status when using -pl
  if echo "$maven_args" | grep -q -- "-pl"; then
    if echo "$maven_args" | grep -q -- "-am"; then
      echo "ℹ️  Including upstream dependencies (-am)"
    else
      echo "ℹ️  Testing module in isolation (no -am)"
      echo "   Add -am if you changed parent/shared modules"
    fi
  fi

  START=$(date +%s)
  mvn $maven_args > "$LOG_FILE" 2>&1
  RESULT=$?
  END=$(date +%s)
  echo "⏱️ Duration: $((END - START)) seconds"

  return $RESULT
}

# Main execution
START_TOTAL=$(date +%s)

# First attempt: run the test/verify
run_maven "$MVN_CMD $CMD_ARGS" "Running"
RESULT=$?

# If failed, check if it's a compilation/dependency issue
if [ $RESULT -ne 0 ]; then
  # Check for errors that indicate we need -am
  if grep -qE "cannot find symbol|Could not find artifact|package .* does not exist|cannot access|NoClassDefFoundError|ClassNotFoundException|NoSuchMethodError" "$LOG_FILE"; then

    # Check if this might be due to missing -am
    if [ $SUGGEST_AM -eq 1 ] && grep -qE "from parent|from upstream|domain\.|common\." "$LOG_FILE"; then
      echo ""
      echo "💡 TIP: This might be a cross-module dependency issue."
      echo "   Try: ./run-tests.sh $CMD_ARGS -am"
      echo "   This will rebuild upstream dependencies."
      echo ""
    fi

    print_header "⚠️ Compilation/dependency issue detected - running clean build"

    # Clean and rebuild with same scope as test
    if echo "$CMD_ARGS" | grep -q -- "-pl"; then
      # For module-specific: clean and install just that module + dependencies
      run_maven "clean install -DskipTests $CMD_ARGS" "Rebuilding module"
    else
      # For full project: clean everything
      run_maven "clean install -DskipTests" "Rebuilding project"
    fi

    if [ $? -ne 0 ]; then
      print_header "❌ Build failed"
      tail -40 "$LOG_FILE"
      echo ""
      echo "📄 Full log at: $LOG_FILE"
      exit 1
    fi

    # Retry the original test
    run_maven "$MVN_CMD $CMD_ARGS" "Retrying"
    RESULT=$?
  fi
fi

# Final output
print_header "Test Results"

# Extract test summary (handle both Surefire and Failsafe output)
SUMMARY=$(grep -E "Tests run:|IT tests run:" "$LOG_FILE" | tail -1 || echo "No test summary found")

if [ $RESULT -eq 0 ]; then
  echo "✅ SUCCESS"
  echo "$SUMMARY"

  # Show test count if available
  TEST_COUNT=$(echo "$SUMMARY" | sed -n 's/.*Tests run: \([0-9]*\),.*/\1/p')
  if [[ -n "$TEST_COUNT" && "$TEST_COUNT" =~ ^[0-9]+$ && "$TEST_COUNT" -gt 0 ]]; then
    echo "🧪 $TEST_COUNT tests executed"
  elif [[ "$TEST_COUNT" == "0" ]]; then
    echo "⚠️  WARNING: No tests were run! Check your test configuration."
  fi
else
  echo "❌ FAILED"
  echo "$SUMMARY"
  echo ""
  echo "🔍 Failure details:"

  if [ $VERBOSE -eq 1 ]; then
    cat "$LOG_FILE"
  else
    # Show first error and context
    awk '/^\[ERROR\]/ {found=1} found' "$LOG_FILE" | head -50

    # If no [ERROR] lines, show end of log
    if ! grep -q "^\[ERROR\]" "$LOG_FILE"; then
      echo "Last 50 lines of output:"
      tail -50 "$LOG_FILE"
    fi

    # Additional hints for common issues
    if grep -qE "NoClassDefFoundError.*domain\.|NoClassDefFoundError.*common\." "$LOG_FILE"; then
      echo ""
      echo "💡 Cross-module dependency issue detected!"
      echo "   Try: ./run-tests.sh $CMD_ARGS -am"
    elif grep -q "JsonMappingException\|JsonParseException" "$LOG_FILE"; then
      echo ""
      echo "💡 Jackson serialization issue (common after Java→Kotlin conversion)"
      echo "   Try: mvn clean install -DskipTests && ./run-tests.sh $CMD_ARGS"
    fi
  fi

  echo ""
  echo "📄 Full log at: $LOG_FILE"
fi

END_TOTAL=$(date +%s)
echo ""
echo "⏱️ Total time: $((END_TOTAL - START_TOTAL)) seconds"

exit $RESULT
