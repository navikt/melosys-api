# Requirements: Java to Kotlin Test Files Review and Fixes

In this request, you are an AI agent tasked with reviewing and processing a set of files according to specific guidelines. Your goal is to ensure that each file is examined thoroughly and that all relevant rules and instructions are followed. You will work systematically through the files, documenting your findings and any necessary actions in a clear and organized manner.

You will be reviewing Kotlin test files, which are converted from Java files. Your task is to ensure that the Kotlin files adhere to the specified coding standards and practices. In addition, we need to ensure that there are no missing tests or asserts. Each file will be processed according to a predefined set of rules, which you will follow step-by-step.

We are going to follow The Golden Steps to read and understand the requirements, get context, review and fix the Kotlin test files, and finalize the review.

## Application Instructions
1. Preserve original test logic and assertions
2. Add appropriate Kotlin annotations and modifiers
3. Ensure proper import statements

## The Golden Steps

### Phase 1 — starting the job:
1. Be sure to have read and understood the requirements that are set in this file (requirements-java-kotlin-review-and-fixes.md). Ask any questions you are unsure of before starting.
2. Read "tracking-java-kotlin-review-and-fixes.md" if you haven't do so yet. Understand how far we have come and which file we are working on ({PROCESSING-FILE}).
3. Create "{PROCESSING-FILE}-review-and-fixes.md" where you start with the template from the file progressing-file-review-and-fixes-template.md. You can copy this file and rename it.
4. Write the name of the file in tracking-java-kotlin-review-and-fixes.md in the "ProgressFile" field.
5. Write the status of the file in tracking-java-kotlin-review-and-fixes.md in the "Status" field. Set it to "Processing".

### Phase 2 — getting context:
1. Read the file kotlin-test-file-processing-rules.md. This is a document containing all the best practices and rules we need to check and follow.
2. Read and understand the Kotlin test file and Java test file. Also read the main class file which we are testing.

### Phase 3 — reviewing and fixing the Kotlin test file
For each of each of the steps in the file {PROCESSING-FILE}-review-and-fixes.md, you will follow these steps:
1. Check if the Kotlin test file follows the rules set in kotlin-test-file-processing-rules.md.
2. Fill out the status, verdict, and comments for each rule in {PROCESSING-FILE}-review-and-fixes.md. If a rule is not applicable, write "N/A".
3. If you find any issues or areas for improvement, make the necessary changes to the Kotlin test file. Document these changes in the comments section of {PROCESSING-FILE}-review-and-fixes.md.
4. Make SURE that the tests are running and passing after you have made the changes. If they are not passing, you need to fix them. If you are unsure how to fix them, ask me for help. You are not allowed to go to step 7 before the tests are passing.

### Phase 4 — finalizing the review
1. Once you have completed the review and made any necessary changes, see that you have followed the Validation Checklist. Again, make sure the tests are passing.
2. Update tracking-java-kotlin-review-and-fixes.md to set status and verdict.
3. Commit your changes with a commit message "Review and fixes for {PROCESSING-FILE}". Include all the files you have changed in the commit, including the {PROCESSING-FILE}-review-and-fixes.md and tracking-java-kotlin-review-and-fixes.md.

