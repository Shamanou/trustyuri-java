trustyuri-testsuite
===================

This is the test suite to validate implementations of _trusty URIs_.
See https://github.com/trustyuri/trustyuri.

_(under construction...)_

The top-level directory stand for the different modules. On the next level,
the following directory structure is applied:

- `pre` contains the plain files before they are transformed and the hash
  is calculated
- `valid` contains the same files as in `pre` but with a valid trusty URI
  hash as indicated in the file name
- `invalid` contains files that do not match the hash of the file name

