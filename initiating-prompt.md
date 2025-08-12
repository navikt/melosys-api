In this request, we will be creating our initial files to facilitate a job, working with reviewing and fixing Kotlin test files conversion from Java.
First, before doing anything, let's create some documentation that we will use to systematically go through each test file and set some fields. Look at this PR/branch. There are in total 136 files changed. Ten of them are .MD/.mdc files, and the rest of them are Kotlin test files. The Kotlin files are converted from Java files.

Create a documentation called "tracking-java-kotlin-review-and-fixes.md", where we will list _all_ the Kotlin test files and their corresponding Java test files, as well as the main class files they are testing. In addition, we want to include the status of each file, the progress of the review and fixes, and the verdict after the review is completed. To sum it up, here's the template for the tracking file, for each Kotlin test file:
```
1. **File**: `service/src/.../kotlin-test-file-name.kt`
    - **Java Test File**: `service/src/java-test-file-name.java`
    - **Main Class File**: `service/src/.../main-class-file-name.kt|java`
    - **ProgressFile**: Keep it empty. Will be created in the "The Golden Steps".
    - **Status**: [Completed, Processing, Not started]. Set to "Not started" by default.
    - **Verdict**: [Passed, Needs improvements] Will be set after the The Golden Steps. Keep this empty.
```

You can get a lot of help from a file called "kotlin-test-conversion-tracking.md" and "missing-tests-conversion-tracking.md" to get the names of the Java and Kotlin test files.

In this FIRST request, we need to create the requirements for this job and the tracking file. That way, an AI agent or a person can continue working with the same job, understand the full scope of this, even if we lose context of a session. By providing the "tracking-java-kotlin-review-and-fixes.md", and "requirements-java-kotlin-review-and-fixes.md", any person or AI will understand the task right away. By knowing the progress/tracking file, we will get a status of our progress right away. In the requirements, we are referring to the "The Golden Steps" which is really important. The reviewer needs to follow these points STRICTLY and systematically. This ensures that we do not miss any steps and that we follow the rules set in the "kotlin-test-file-processing-rules.md".

If there are ANY questions, please ask before proceeding. Do not assume anything you are not sure of. If you are unsure of something, ask me for clarification. The requirements below needs to be written in the requirements-java-kotlin-review-and-fixes.md file, and the tracking file needs to be created as "tracking-java-kotlin-review-and-fixes.md". Be sure to write them down and tell me if you changed anything in the requirements, and why you did it.

# Requirements:
In this request, you are an AI agent tasked with reviewing and processing a set of files according to specific guidelines. Your goal is to ensure that each file is examined thoroughly and that all relevant rules and instructions are followed. You will work systematically through the files, documenting your findings and any necessary actions in a clear and organized manner.

You will be reviewing Kotlin test files, which are converted from Java files. Your task is to ensure that the Kotlin files adhere to the specified coding standards and practices. In addition, we need to ensure that there are no missing tests or asserts. Each file will be processed according to a predefined set of rules, which you will follow step-by-step.

We are going to follow The Golden Steps to read and understand the requirements, get context, review and fix the Kotlin test files, and finalize the review.

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
3. Commit your changes with a commit message "Review and fixes for {PROCESSING-FILE}". Incldue all the files you have changed in the commit, including the {PROCESSING-FILE}-review-and-fixes.md and tracking-java-kotlin-review-and-fixes.md.

