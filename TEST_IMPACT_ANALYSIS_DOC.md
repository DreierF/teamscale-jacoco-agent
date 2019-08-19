# Test Impact Analysis in Teamscale

## Concept

Regression testing is typically done in a retest-all strategy, even if most of the tests are
irrelevant when only considering a given small change. By collecting data of which test executes
which part of the system we can find out which tests are actually impacted by a given change.
More concrete this means we have to gather coverage information per test case.

Teamscale with its Test Impact Analysis augments this data with source code change
information and calculates the impacted tests for one or more changes. It's then the
responsibility of the test framework to execute only those.

## High level view on the API

### 1. Upload Test Details and query impacted tests

Impacted tests can be queried via the `Test Impact` REST-API, which returns a list of tests that should be executed to 
test recent changes. The API can be called as `GET` or `PUT` method. The `GET` method uses only tests that are already 
known to Teamscale from previous test runs. The `PUT` method allows to send a list of currently available tests to 
Teamscale and therefore to pick up new tests on the go.

For both methods a branch and timestamp must be given, which specifies the code version that the tests are about to be 
executed for.

### 2. Upload TESTWISE_COVERAGE report

During the test execution the coverage produced by each test case has to be saved separately.
After the execution the collected testwise coverage needs to be converted to the following format.
If possible the covered lines can already be compressed to method level, but this step is optional and only needed in 
order to reduce the file size of the final report.

```json
{
  "tests": [
    {
      "uniformPath": "com/example/project/JUnit4Test/systemTest",
      "sourcePath": "com/example/project/JUnit4Test",
      "duration": 0.025,
      "result": "PASSED",
      "paths": [
        {
          "path": "com/example/project",
          "files": [
            {
              "fileName": "Calculator.java",
              "coveredLines": "20-24,26,27,29"
            },
            {
              "fileName": "JUnit4Test.java",
              "coveredLines": "26-28"
            }
          ]
        }
      ]
    },
    {
      "uniformPath": "com/example/project/JUnit4Test/notExecutedTest",
      "sourcePath": "com/example/project/JUnit4Test"
    },
    {
      "uniformPath": "com/example/project/JUnit5Test/failedTest()",
      "sourcePath": "com/example/project/JUnit5Test",
      "duration": 0.045,
      "result": "ERROR",
      "message": "Some error"
    },
    {
      "uniformPath": "com/example/project/JUnit5Test/skippedTest()",
      "result": "SKIPPED"
    }
  ]
}
```

The report can be uploaded to Teamscale via the `Report Upload` REST-API by specifying `TESTWISE_COVERAGE` as report format.

## Rest API

To authorize the request Teamscale requires you to use Basic Authentication.
This means that the request should contain an `Authentication` header with the content `Basic ` followed by a Base64 
encoded version of `userName:userAccessToken`.

The `partition` parameters in the requests refers to a set of tests and there coverage respectively. In most cases this 
refers to the type of test e.g. "Unit Test" or "UI Test". It's important to note that reports uploaded to the same 
partition will overwrite existing data in this partition.

___
### Test Impact

  Returns the tests, which should be executed for the given set of changes. The set of changes is described by two 
  commits: The baseline commit and the end commit. All code changes after the baseline (exclusive) and before 
  end (inclusive) are considered. Also tests which have been changed in between are considered for execution.

  The `Accept` and `Content-Type` headers should be set to `application/json` for this request.

* **URL**

  p/{projectName}/test-impact

* **Method:**

  `GET`

*  **URL Parameters**

   `projectName` Project identifier within Teamscale
   
*  **Query Parameters**

   `baseline=[timestamp]` *(optional)* The baseline timestamp. A long value of the timestamp in milliseconds. 
       Only changes to the code after the baseline will be considered for execution. If not specified all not yet covered 
       changes will be considered.

   `end=[branch]:[timestamp]` Identifies the commit that has been used to build the system that we are about to test.

   `partitions=[string]` *(optional)* The partition to get impacted tests for. Multiple partitions could be specified as a comma 
        separated list (Not recommended). If no partition is given all existing partitions are used.
        
   `prioritizationStrategy=[string]` *(optional)* The strategy used to order the impacted tests. Can be one of:
        `NONE`, `FULLY_RANDOM`, `RANDOM_BUT_IMPACTED_FIRST`, `ADDITIONAL_COVERAGE_PER_TIME` (default)
        
   `includeNonImpacted=true|false` *(optional)* Whether non impacted tests should be included in the result. (Default is `false`)


  `PUT`

*  **URL Parameters**

   `projectName` Project identifier within Teamscale
   
*  **Body**

```json
[
  {
    "uniformPath": "com/example/JUnit5Test/myFirstTest"
  },
  {
    "uniformPath": "com/example/JUnit4Test/testAdd"
  },
  {
    "uniformPath": "com/example/JUnit4Test/systemTest"
  }
]
```

- `uniformPath` Is a file system like path, which is used to uniquely identify a test within Teamscale and should be 
  chosen accordingly. It is furthermore used to make the set of tests hierarchically navigable within Teamscale.
- `sourcePath` *(optional)* Path to the source of the method if the test is specified in a programming language and is 
  known to Teamscale. Will be equal to `uniformPath` in most cases, but e.g. in JUnit `@Test` annotated methods in a base 
  class will have the sourcePath pointing to the base class, which contains the actual implementation, whereas 
  `uniformPath` will contain the class name of the most specific subclass from where it was actually executed.
- `content` *(optional)* Identifies the content of the test specification. This can be used to indicate that the 
  specification of a test has changed and therefore should be re-executed. The value can be an arbitrary string value 
  e.g. a hash of the test specification or a revision number of the test.

*  **Query Parameters**

   `baseline=[timestamp]` *(optional)* The baseline timestamp. A long value of the timestamp in milliseconds. 
       Only changes to the code after the baseline will be considered for execution. If not specified all not yet covered 
       changes will be considered.

   `end=[branch]:[timestamp]` Identifies the commit that has been used to build the system that we are about to test.

   `partitions=[string]` *(optional)* The partition to get impacted tests for. Multiple partitions could be specified as a comma 
        separated list (Not recommended). If no partition is given all existing partitions are used.
        
   `prioritizationStrategy=[string]` *(optional)* The strategy used to order the impacted tests. Can be one of:
        `NONE`, `FULLY_RANDOM`, `RANDOM_BUT_IMPACTED_FIRST`, `ADDITIONAL_COVERAGE_PER_TIME` (default)
        
   `includeNonImpacted=true|false` *(optional)* Whether non impacted tests should be included in the result. (Default is `false`)

* **Success Response:**

  * **Code:** 200 <br />
    **Content:**
    Returns a list of tests.
    ```json
    [
      {
        "uniformPath": "com/example/JUnit5Test/myFirstTest",
        "testSelectionReason": "COVERS_CHANGES",
        "partition": "Unit Tests",
        "durationInMs": 5
      },
      {
        "uniformPath": "com/example/JUnit4Test/testAdd",
        "testSelectionReason": "PREVIOUSLY_FAILED_OR_SKIPPED",
        "partition": "Unit Tests",
        "durationInMs": 3
      }
    ]
    ```

* **Error Response:**

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `failure`

___

### Report Upload

  Allows to upload external reports to Teamscale.

* **URL**

  /p/{projectName}/external-report/

* **Method:**

  `POST`

*  **URL Params**

   `projectName` Project identifier within Teamscale

*  **Query Params**

   `format=[TESTWISE_COVERAGE, JUNIT, JACOCO, GCOV, ...]` The format of the uploaded report

   `t=[branch]:[timestamp]` Identifies the commit which is associated with the upload (e.g. the commit for which coverage has been collected)

   `adjusttimestamp=[true or false]` *(optional)* Increases the timestamp in case a commit at the same time already exists.
   
   `movetolastcommit=[true or false]` *(optional)* Moves the timestamp right after the last commit.

   `partition=[string]` The partition to upload the report to. This typically identifies a set of tests e.g. "Unit Tests".

   `message=[string]` The commit message to show for this artificial report commit.

* **Data Params** (Multipart form data)

  `report` The UTF-8 encoded content of the report file.

* **Success Response:**

  * **Code:** 200 <br />
    **Content:** `success`

* **Error Response:**

  * **Code:** 404 NOT FOUND <br />
    **Content:** `failure`

  OR

  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `failure`

___

## Getting a testwise coverage report

For Gradle based builds using JUnit 5 as test framework, there is a plugin that handles most of the complexity to get 
testwise coverage (see teamscale-gradle-plugin/README.md for more details).

For other JVM-based systems the Teamscale JaCoCo Agent can be used to generate coverage per test case. The generated 
exec file can be augmented with test execution information to build a full testwise coverage report.

Three artifacts are needed:
 - a list of all tests as json list. The filename must start with `test-list` and have a `.json` file extension.
    ```json
    [
      {
        "uniformPath": "com/example/JUnit5Test/myFirstTest"
      },
      {
        "uniformPath": "com/example/JUnit4Test/testAdd"
      },
      {
        "uniformPath": "com/example/JUnit4Test/systemTest"
      }
    ]
    ```
    
    - `uniformPath` Is a file system like path, which is used to uniquely identify a test within Teamscale and should be 
      chosen accordingly. It is furthermore used to make the set of tests hierarchically navigable within Teamscale.
    - `sourcePath` *(optional)* Path to the source of the method if the test is specified in a programming language and is 
      known to Teamscale. Will be equal to `uniformPath` in most cases, but e.g. in JUnit `@Test` annotated methods in a base 
      class will have the sourcePath pointing to the base class, which contains the actual implementation, whereas 
      `uniformPath` will contain the class name of the most specific subclass from where it was actually executed.
    - `content` *(optional)* Identifies the content of the test specification. This can be used to indicate that the 
      specification of a test has changed and therefore should be re-executed. The value can be an arbitrary string value 
      e.g. a hash of the test specification or a revision number of the test.
  
 - a list of test execution results. The filename must start with `test-execution` and have a `.json` file extension.
    ```json
     [
       {
         "uniformPath": "com/example/JUnit5Test/myFirstTest",
         "durationMillis": 10,
         "result": "ERROR",
         "message": "<stacktrace>"
       },
       {
         "uniformPath": "com/example/JUnit4Test/testAdd",
         "durationMillis": 15,
         "result": "PASSED"
       },
       {
         "uniformPath": "com/example/JUnit4Test/systemTest",
         "result": "IGNORED",
         "message": "Flickers"
       }
     ]
     ```
     
     `result` can be one of:
     - `PASSED` Test execution was successful. 
     - `IGNORED` The test is currently marked as "do not execute" (e.g. JUnit @Ignore or @Disabled).
     - `SKIPPED` Caused by a failing assumption.
     - `FAILURE` Caused by a failing assertion.
     - `ERROR` Caused by an error during test execution (e.g. exception thrown).
  - one or more coverage files with coverage per test (See agent/README -> Testwise coverage mode). 
    The files must have an `.exec` file extension.
  
The above mentioned information can be converted into a single report with the Teamscale JaCoCo agent's convert tool 
(with option `--testwise-coverage` enabled).
See it's help command for detailed argument information.
The input files can be one or more directories that contain the files with the specified file name conventions.
