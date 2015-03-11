# Getting Data

This contains code and data for chapter 1, Getting Data, of *The Clojure Data
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

Although the files are named to make clear which recipe they're meant to
accompany, here's a directory of which source file contains the code for which
recipe.

Recipe                                   File
---------------------------------------  ----------------------------------------
Reading CSV Data                         src/getting_data/read_csv.clj
Reading JSON Data                        src/getting_data/read_json.clj
Reading Data from Excel                  src/getting_data/read_xls.clj
Reading Data from JDBC Databases         src/getting_data/read_jdbc.clj
Reading XML Data                         src/getting_data/read_xml.clj
Scraping Data from Tables                src/getting_data/read_html_table.clj
Scraping Textual Data                    src/getting_data/read_html_text.clj
Reading RDF Data                         src/getting_data/read_rdf.clj
Reading Data with SPARQL                 src/getting_data/read_sparql.clj
Aggregating Semantic Web Data            src/getting_data/aggregate_semweb.clj
Aggregating Data from Different Sources  src/getting_data/aggregate_sources.clj
Downloading Data in Parallel             src/getting_data/download_concurrent.clj



