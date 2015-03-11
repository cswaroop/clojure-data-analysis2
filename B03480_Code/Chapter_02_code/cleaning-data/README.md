# Cleaning Data

This contains code and data for chapter 2, Cleaning Data, of *The Clojure Data
Analysis Cookbook* by Eric Rochester.

## Dependencies

To use this, you need to have [Leiningen
2](https://github.com/technomancy/leiningen) installed.

Leiningen will take care of downloading the remaining dependencies:

> lein deps

## Usage

Once that's installed, you can run the code by running the specs:

> lein spec

However, the primary usage of the files is reading them in conjunction with the
recipes and modifying them.

## The Files

The main code for the recipes is in the `src` directory, and the specs for them
are in the `spec` directory.

In general, the generic, abstract code is in the main source file, and the code
pointing to specific data sources---as well as the tests---are in the specs
file. Occasionally, if a recipe is extremely simple, only the spec file is
given. Or if it is primarily about interactive workflows or something, only the
source file.

