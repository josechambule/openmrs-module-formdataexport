OpenMRS Form Data Export Module
===============================

[![OpenMRS](http://openmrs.org/wp-content/uploads/2013/07/OpenMRS-logo.svg)](http://www.openmrs.org/)

For information information about this module see the [Form Data Export module wiki page](https://wiki.openmrs.org/display/docs/Form+Data+Export).

Installation
------------

To install the Form Data Export module, download the right version from the [module download page](https://modules.openmrs.org/modules/view.jsp?module=formdataexport) and follow the installation instructions on the [Administering Modules wiki page](https://wiki.openmrs.org/display/docs/Administering+Modules).

Building
--------

To build the module, execute the following commands:

    git clone https://github.com/jembi/openmrs-module-formdataexport.git
    cd openmrs-module-formdataexport
    mvn clean install -DskipTests

Known Issues
------------

For now, the tests don't run, hence the `-DskipTests` in the build instructions above.
