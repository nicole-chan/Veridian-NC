#!/bin/bash

# Nicole Chan (nc83@students.waikato.ac.nz)

# Compiles all of the .java files in the src folder
javac "$DLC_NEWSFRAME_HOME/src/"*.java

# Check if the exit status was successful
javac_status=$?

if [ $javac_status -ne 0 ] ; then
	echo 'Exit Status of compilation of *.java: $javac_status' >&2
	echo 'Tip: have you run \". ./setup.bash\" yet?' >&2
	exit $javac_status
fi
