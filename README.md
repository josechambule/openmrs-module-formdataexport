OpenMRS Form Data Export Module
===============================

[![OpenMRS](http://openmrs.org/wp-content/uploads/2013/07/OpenMRS-logo.svg)](http://www.openmrs.org/)

For information information about this module see the [Form Data Export module wiki page](https://wiki.openmrs.org/display/docs/Form+Data+Export).

Installation
------------

To install the Form Data Export module, download the right version from the [module download page](https://modules.openmrs.org/modules/view.jsp?module=formdataexport) and follow the installation instructions on the [Administering Modules wiki page](https://wiki.openmrs.org/display/docs/Administering+Modules).

Configuration
-------------

The Form Data Export module is configured using [global properties](https://wiki.openmrs.org/display/docs/Global+Properties+Descriptions). To change the values of the global properties, go the the *Administration* page, then, under the *Maintenance* heading, click *Settings*. Finally, click the *Formdataexport* item on the menu on the left. The four available global properties are described below.

### Patient Identifier Types

A comma-separated list of patient identifier types by identifier name that you want to include in your data exports.

### Person Address Columns

A comma-separated list of person address columns by name and the titles that you want to use for the columns. Each element of the list should be of the form `title=addressColumn`, where title is the title and addressColumn is one of: `address1`, `address2`, `address3`, `address4`, `address5`, `address6`, `cityVillage`, `countyDistrict`, `stateProvince`, `country`, `postalCode`, `latitude` or `longitude`.

### Person Attribute Types

A comma-separated list of person attribute types by name and the titles that you want to use for the columns. Each element of the list should be of the form `title=personAttributeType`, where title is the title and personAttributeType is the name of the person attribute type. You may also include the following four special attribute types: `age`, `gender`, `givenName` and `familyName`.

### Skip Columns With Suffix

A comma-separated list of column suffixes to skip. If there are any exceptions, add a pipe symbol and then list the exceptions. For example `_DATE,_PARENT` and `_DATE,_PARENT|TRANSFER_IN_DATE` are both valid.

Building
--------

To build the module, execute the following commands:

    git clone https://github.com/jembi/openmrs-module-formdataexport.git
    cd openmrs-module-formdataexport
    mvn clean install -DskipTests

Known Issues
------------

For now, the tests don't run, hence the `-DskipTests` in the build instructions above.
