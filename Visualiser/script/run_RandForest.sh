#!/bin/bash

# Nicole Chan (nc83@students.waikato.ac.nz)

# NOTE: Make sure the -cp in the if/else statement is set to the correct location of the weka.jar file.
# If there is an error in SMOTE, try using a smaller weight, because the input data may not large enough. i.e. instead of 100 in the following line use, say, 35.0
# weka.filters.supervised.instance.SMOTE -C 3 -K 5 -P 100.0 -S 1
# If there is an error with the input file, check if there is a file with the same filename as you have provided in program arguments or as full.csv, delete the file and run again.

CSV_OUTPUT="$1" 	#filename of .csv OR .arff file for input to WEKA

# Check if a verbosity level was provided, set it to the variable, else default to 1
if ! [ -z "$2" ]  && [ "$2" -eq "$2" ] ; then
	VERB_LEVEL=$2
else
	VERB_LEVEL="1"
fi

# If the verbosity is less than 2, print the buffer to the terminal. Else, print the predictions to predictions.csv
if [ "$VERB_LEVEL" -lt "3" ] ; then
	java -cp ~/veridian/weka-3-8-1/weka.jar weka.classifiers.meta.FilteredClassifier -k -t $DLC_NEWSFRAME_HOME/$CSV_OUTPUT -F "weka.filters.MultiFilter -F \"weka.filters.supervised.instance.SMOTE -C 1 -K 5 -P 100.0 -S 1\" -F \"weka.filters.supervised.instance.SMOTE -C 21 -K 5 -P 65.0 -S 1\" -F \"weka.filters.supervised.instance.SMOTE -C 3 -K 5 -P 65.0 -S 1\" -F \"weka.filters.unsupervised.instance.Randomize -S 42\"" -W weka.classifiers.trees.RandomForest -- -P 100 -I 53 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1 -depth 10
else
	java -cp ~/veridian/weka-3-8-1/weka.jar weka.classifiers.meta.FilteredClassifier -classifications weka.classifiers.evaluation.output.prediction.CSV > $DLC_NEWSFRAME_HOME/predictions.csv -k -t $DLC_NEWSFRAME_HOME/$CSV_OUTPUT -F "weka.filters.MultiFilter -F \"weka.filters.supervised.instance.SMOTE -C 1 -K 5 -P 100.0 -S 1\" -F \"weka.filters.unsupervised.instance.Randomize -S 42\"" -W weka.classifiers.trees.RandomForest -- -P 100 -I 53 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1 -depth 10
fi
