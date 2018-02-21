#!/bin/bash
# -x

# Nicole Chan (nc83@students.waikato.ac.nz)

# Command line args with examples:
#	$0 = name of script											./open_weka.sh		use ${0##*/} to get name without ./
#	$1 = INPUT_DIR 	- directory to .xml files					/home/nc83/veridian/CambridgeSentinel/1906/11/
#	$2 = ALG_SCRIPT - script of algorithm to use				run_RandForest.sh
#	$3 = CSV_OUTPUT - .csv filename of java output/alg input	sea_19260225-N.csv
#	$4 = verbosity												-v or -V
# 	$5 = VERB_LEVEL	- verbosity level							2

#	Check if at least 2 arguments were provided.
if [ $# -lt 2 ]; then
	echo "Please provide arguments. Usage: ${0##*/} Input_Directory algorithm" >&2
	exit 1 # break
fi

# Path to input data
INPUT_DIR=$1 	# e.g. /home/nc83/veridian/CambridgeSentinel/1906/11/
ALG_SCRIPT=$2 	# e.g. run_RandForest.sh

# Check if the first argument is a directory for input files
if ! [ -d $1 ] ; then
	echo "First argument: '$1' is not a valid directory for input files."
	exit 1 # break
fi

# Check if the third input is a filename, if not, set the name for the java output to full.csv
if [ -z "$3" ] && [ -f "$3" ] ; then
	CSV_OUTPUT="full.csv"
else
	CSV_OUTPUT=$3
fi

# Check if the fourth and first arguments are for verbosity, if they are, set the variable
if [ "$5" -eq "$5" ] && [ "$4" == "-v" ]  || [ "$5" -eq "$5" ] && [ "$4" == "-V" ] ; then
	VERB_LEVEL=$5
fi



# Run Visualiser.java
java -classpath $CLASSPATH Visualiser "$INPUT_DIR" "$CSV_OUTPUT" $*

# Check if Visualiser.java was successful
java_status=$?
if [ $java_status -ne 0 ] ; then
	echo "Exit Status of Visualiser.java: $java_status" >&2
	exit $java_status
fi



# Run the algorithm, and print the time taken and exit status
time $ALG_SCRIPT $CSV_OUTPUT $VERB_LEVEL
echo "Exit Status of "$ALG_SCRIPT": "$?



echo "done!"
