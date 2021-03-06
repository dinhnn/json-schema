Contributing guidelines
=======================

Submitting bugreports
---------------------

Though any kind of feedback, feature request and bugreport is appreciated, I would more prefer if you would send a proper
pull request with a failing test in case you find a bug in the library.

Currently there is a simple mechanism in the integration tests to make it easy to add new tests. Using this is especially
preferred if your bugreport is related to a complex schema or multiple schemas. There is no need to dig into the java code,
you only have to create a few new files in the repo (but you will have to run the tests - see the build instructions below).

Steps:
 * create an issue, just to get an issue number
 * fork the repository
 * in your fork, create a directory under the `tests/src/test/resources/org/everit/json/schema/issues/` directory (for example `issue42` )
 * in this directory create a `schema.json` file with your JSON Schema document that is not handled correctly
 * in the same directory create a `subject-valid.json` file, which is a JSON document, and you expect that document to pass
the validation, but due to a bug it fails with a `ValidationException`
 * if you have a JSON document that you expect to be invalid, but it passes the validation, then you should name this file `subject-invalid.json`.
It will mean that for the test suite that an expected `ValidationException` is not thrown.
 * you can create both the `subject-valid.json` and `subject-invalid.json` test files if you find it needed


Remote schema loading:
If your testcase has anything to do with remote schemas, then
 * you can put those schemas under the `yourIssueDir/remotes/` directory
 * the `yourIssueDir/remotes/` directory will act as the document root of a HTTP server during test execution
 * this HTTP server will listen at address `http://localhost:1234` so please change your schemas (`id` and `$ref` properties)
to fetch the remote schemas relative from this address

You can find a good example for all of these in the `tests/src/test/resources/org/everit/json/schema/issues/issue17` testcase.

If you successfully created your testcase, then it will fail with an `AssertionError` with a message like
"validation failed with: ValidationException:..." or "did not throw ValidationException for invalid subject",
and then you are ready to send a pull request.


Building the project locally
----------------------------

Prerequisities: the following tools have to be installed:
* jdk1.8.0_45 (earlier versions of javac cannot compile the project due to a type inference issue)
* maven 3.x


Steps for building the project:
* clone the repository: `git clone https://github.com/everit-org/json-schema.git && cd json-schema/`
* build it with maven: `mvn clean install`

(or just `mvn clean test` if you are only interested in running the tests)
